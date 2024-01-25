package controllers.v2.files

import com.google.inject.Inject
import controllers.v2.SimpleHostingController
import components.services.serializer.JsonService
import play.api.mvc.ControllerComponents
import play.api.libs.Files
import play.api.mvc.Action
import play.api.mvc.MultipartFormData
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.CreateBucketRequest
import java.io.ByteArrayInputStream
import com.amazonaws.services.s3.model.ObjectMetadata
import components.basic.ErrorMonad
import components.basic.ResultMonad
import components.basic.{ mapToMonad, zipWith, enrichWith }
import controllers.v2.UserNotFoundForRequest
import com.amazonaws.services.s3.AmazonS3
import components.clients.juggernaut.JuggernautClient
import components.clients.juggernaut.DownloadFileTask
import components.clients.juggernaut.TaskNotFound
import components.clients.controller.ControllerClientFactory
import io.github.heavypunk.controller.client.Settings
import business.services.slickStorages.servers.GameServersStorage
import business.services.slickStorages.servers.findByHash
import business.services.slickStorages.servers.GameServerNotFound
import business.services.slickStorages.user.UserStorage
import components.clients.files.DeleteFileRequest
import io.github.heavypunk.controller.client.contracts.files.RemoveFileRequest
import java.time.Duration
import controllers.v2.RequestBodyNotFound
import controllers.v2.JsonNotFoundForRequestBody
import controllers.v2.JsonCannotBeParsed
import components.clients.files.DeleteFileResponse
import components.clients.files.CreateFileRequest
import components.clients.files.CreateFileResponse
import components.clients.files.CreateDirectoryRequest
import components.clients.files.CreateDirectoryResponse
import components.clients.files.ListDirectoryRequest
import components.clients.files.ListDirectoryResponse
import components.clients.files.FileNode
import components.clients.files.GetFileContentRequest
import components.clients.files.GetFileContentResponse
import java.util.Base64
import components.services.log.Log
import components.basic.serializeForLog

class AccessDenied
class FileDestinationPathNotFound
class ControllerPortNotDefinedForServer

class FilesControllerV2 @Inject() (
    val controllerComponents: ControllerComponents,
    val amazonS3: AmazonS3,
    val juggernautClient: JuggernautClient,
    val gameServerStorage: GameServersStorage,
    val userStorage: UserStorage,
    val controllerClientFactory: ControllerClientFactory,
    val controllerClientSettings: Settings,
    val jsonizer: JsonService,
    val log: Log,
) extends SimpleHostingController(jsonizer):
    def upload(gameServerHash: String): Action[MultipartFormData[Files.TemporaryFile]] = Action.async(parse.multipartFormData) { implicit request => {
        val user = findUserForCurrentRequest(request)
        val destinationPathResult = request.getQueryString("destination").mapToMonad(FileDestinationPathNotFound())
        val gameServer = gameServerStorage.findByHash(gameServerHash)
        val grantedGameServer = gameServer.zipWith(user)
            .flatMap((s, u) => if (s.owner.get.tryGetValue._2.id == u.id) ResultMonad(s) else ErrorMonad(AccessDenied()))
        val controllerPort = grantedGameServer.flatMap(server => server.ports.get).flatMap(ports => ports.find(_.portKind.equalsIgnoreCase("controller")).mapToMonad(ControllerPortNotDefinedForServer()))
        val result = user
            .zipWith(destinationPathResult, grantedGameServer, controllerPort)
            .flatMap((user, destinationPath, server, controllerPort) => {
                val commonBucketName = "common-bucket"
                if !amazonS3.doesBucketExistV2(commonBucketName) then
                    amazonS3.createBucket(commonBucketName)
                try {
                    val r = scala.collection.mutable.Set[DownloadFileTask]()
                    for (file <- request.body.files) {
                        val putObjectRequest = PutObjectRequest(
                            commonBucketName,
                            user.login + '/' + file.filename,
                            ByteArrayInputStream(file.transformRefToBytes().toArray),
                            ObjectMetadata()
                        )
                        val response = amazonS3.putObject(putObjectRequest)
                        r.add(
                            DownloadFileTask(
                                controllerClientSettings.scheme,
                                server.ip,
                                controllerPort.port,
                                commonBucketName,
                                user.login + '/' + file.filename,
                                destinationPath
                            )
                        )
                    }
                    ResultMonad(r)
                } catch {
                    case e: Exception => ErrorMonad(e)
                }
            })
            .flatMap(tasks => {
                val r = scala.collection.mutable.Set[String]()
                for (task <- tasks)
                    juggernautClient.createDownloadFileTask(task)
                    .flatMap(id => { r.add(id); ResultMonad(()) })
                ResultMonad(r)
            })

        val (err, taskIds) = result.tryGetValue
        if (err != null)
            err match

                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("User not found")))
                case _: FileDestinationPathNotFound => wrapToFuture(BadRequest(serializeError("File destination path not found (you should specify X-File-Destination-Path header)")))
                case _: GameServerNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case _: ControllerPortNotDefinedForServer => wrapToFuture(InternalServerError(serializeError("Controller port not defined for server")))
                case _: Exception => wrapToFuture(InternalServerError(serializeError("Internal server error")))
        else wrapToFuture(Ok(jsonizer.serialize(taskIds)))
    }}

    def deleteFile() = Action.async { implicit request => {
        val user = findUserForCurrentRequest(request)
        val req = getModelFromJsonRequest[DeleteFileRequest](request)
        val gameServer = req.flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val grantedGameServer = gameServer.zipWith(user)
            .flatMap((s, u) => if (s.owner.get.tryGetValue._2.id == u.id) ResultMonad(s) else ErrorMonad(AccessDenied()))
        val controllerPort = grantedGameServer.flatMap(server => server.ports.get).flatMap(ports => ports.find(_.portKind.equalsIgnoreCase("controller")).mapToMonad(ControllerPortNotDefinedForServer()))
        val controllerClient = controllerPort
            .zipWith(grantedGameServer)
            .flatMap((cp, server) => ResultMonad(controllerClientFactory.getControllerClient(Settings(controllerClientSettings.scheme, server.ip, cp.port))))


        val result = controllerClient
            .zipWith(req)
            .flatMap((cc, req) => {
                try {
                    ResultMonad(cc.files.deleteFile(RemoveFileRequest(req.pathToFile), Duration.ofMinutes(2)))
                } catch {
                    case e: Exception => ErrorMonad(e)
                }
            })
        
        val (err, res) = result.tryGetValue
        if err != null then
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: GameServerNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: ControllerPortNotDefinedForServer => wrapToFuture(InternalServerError(serializeError("Controller port not defined for server")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError(serializeError("Internal Server Error")))
        else wrapToFuture(Ok(jsonizer.serialize(DeleteFileResponse(
            res.taskId,
            res.success,
            res.error
        ))))
    }}

    def createFile() = Action.async { implicit request => {
        val user = findUserForCurrentRequest(request)
        val req = getModelFromJsonRequest[CreateFileRequest](request)
        val gameServer = req.flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val grantedGameServer = gameServer.zipWith(user)
            .flatMap((s, u) => if (s.owner.get.tryGetValue._2.id == u.id) ResultMonad(s) else ErrorMonad(AccessDenied()))
        val controllerPort = grantedGameServer.flatMap(server => server.ports.get).flatMap(ports => ports.find(_.portKind.equalsIgnoreCase("controller")).mapToMonad(ControllerPortNotDefinedForServer()))
        val controllerClient = controllerPort
            .zipWith(grantedGameServer)
            .flatMap((cp, server) => ResultMonad(controllerClientFactory.getControllerClient(Settings(controllerClientSettings.scheme, server.ip, cp.port))))


        val result = controllerClient
            .zipWith(req)
            .flatMap((cc, req) => {
                try {
                    ResultMonad(cc.files.createFile(io.github.heavypunk.controller.client.contracts.files.CreateFileRequest(req.pathToFile, Base64.getEncoder().encodeToString(req.content.getBytes("UTF-8"))), Duration.ofMinutes(2)))
                } catch {
                    case e: Exception => ErrorMonad(e)
                }
            })
        
        val (err, res) = result.tryGetValue
        if err != null then
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: GameServerNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: ControllerPortNotDefinedForServer => wrapToFuture(InternalServerError(serializeError("Controller port not defined for server")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError(serializeError("Internal Server Error")))
        else wrapToFuture(Ok(jsonizer.serialize(CreateFileResponse(
            res.taskId,
            res.success,
            res.error
        ))))
    }}

    def createDirectory() = Action.async { implicit request => {
        val user = findUserForCurrentRequest(request)
        val req = getModelFromJsonRequest[CreateDirectoryRequest](request)
        val gameServer = req.flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val grantedGameServer = gameServer.zipWith(user)
            .flatMap((s, u) => if (s.owner.get.tryGetValue._2.id == u.id) ResultMonad(s) else ErrorMonad(AccessDenied()))
        val controllerPort = grantedGameServer.flatMap(server => server.ports.get).flatMap(ports => ports.find(_.portKind.equalsIgnoreCase("controller")).mapToMonad(ControllerPortNotDefinedForServer()))
        val controllerClient = controllerPort
            .zipWith(grantedGameServer)
            .flatMap((cp, server) => ResultMonad(controllerClientFactory.getControllerClient(Settings(controllerClientSettings.scheme, server.ip, cp.port))))


        val result = controllerClient
            .zipWith(req)
            .flatMap((cc, req) => {
                try {
                    ResultMonad(cc.files.createDirectory(io.github.heavypunk.controller.client.contracts.files.CreateDirectoryRequest(req.pathToDirectory), Duration.ofMinutes(2)))
                } catch {
                    case e: Exception => ErrorMonad(e)
                }
            })
        
        val (err, res) = result.tryGetValue
        if err != null then
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: GameServerNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: ControllerPortNotDefinedForServer => wrapToFuture(InternalServerError(serializeError("Controller port not defined for server")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError(serializeError("Internal Server Error")))
        else wrapToFuture(Ok(jsonizer.serialize(CreateDirectoryResponse(
            res.taskId,
            res.success,
            res.error
        ))))
    }}

    def listDirectory() = Action.async { implicit request => {
        val user = findUserForCurrentRequest(request)
        val req = getModelFromJsonRequest[ListDirectoryRequest](request)
        val gameServer = req.flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val grantedGameServer = gameServer.zipWith(user)
            .flatMap((s, u) => if (s.owner.get.tryGetValue._2.id == u.id) ResultMonad(s) else ErrorMonad(AccessDenied()))
        val controllerPort = grantedGameServer.flatMap(server => server.ports.get).flatMap(ports => ports.find(_.portKind.equalsIgnoreCase("controller")).mapToMonad(ControllerPortNotDefinedForServer()))
        val controllerClient = controllerPort
            .zipWith(grantedGameServer)
            .flatMap((cp, server) => ResultMonad(controllerClientFactory.getControllerClient(Settings(controllerClientSettings.scheme, server.ip, cp.port))))


        val result = controllerClient
            .zipWith(req)
            .flatMap((cc, req) => {
                try {
                    ResultMonad(cc.files.listDirectory(io.github.heavypunk.controller.client.contracts.files.ListDirectoryRequest(req.pathToDirectory), Duration.ofMinutes(2)))
                } catch {
                    case e: Exception => ErrorMonad(e)
                }
            })
        
        val (err, res) = result.tryGetValue
        if err != null then
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: GameServerNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: ControllerPortNotDefinedForServer => wrapToFuture(InternalServerError(serializeError("Controller port not defined for server")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError(serializeError("Internal Server Error")))
        else wrapToFuture(Ok(jsonizer.serialize(ListDirectoryResponse(
            files = res.files.map(node => FileNode(
                path = node.path,
                nodeType = node.nodeType,
                size = node.size,
                fileName = node.name,
                extension = node.extension
            )),
            success = res.success,
            error = res.error,
        ))))
    }}

    def getFileContent() = Action.async { implicit request => {
        val user = findUserForCurrentRequest(request)
        val req = getModelFromJsonRequest[GetFileContentRequest](request)
        val gameServer = req.flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val grantedGameServer = gameServer.zipWith(user)
            .flatMap((s, u) => if (s.owner.get.tryGetValue._2.id == u.id) ResultMonad(s) else ErrorMonad(AccessDenied()))
        val controllerPort = grantedGameServer.flatMap(server => server.ports.get).flatMap(ports => ports.find(_.portKind.equalsIgnoreCase("controller")).mapToMonad(ControllerPortNotDefinedForServer()))
        val controllerClient = controllerPort
            .zipWith(grantedGameServer)
            .flatMap((cp, server) => ResultMonad(controllerClientFactory.getControllerClient(Settings(controllerClientSettings.scheme, server.ip, cp.port))))


        val result = controllerClient
            .zipWith(req)
            .flatMap((cc, req) => {
                try {
                    ResultMonad(cc.files.getFileContent(io.github.heavypunk.controller.client.contracts.files.GetFileContentRequest(req.path), Duration.ofMinutes(2)))
                } catch {
                    case e: Exception => ErrorMonad(e)
                }
            })
        
        val (err, res) = result.tryGetValue
        if err != null then
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: GameServerNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: ControllerPortNotDefinedForServer => wrapToFuture(InternalServerError(serializeError("Controller port not defined for server")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError(serializeError("Internal Server Error")))
        else wrapToFuture(Ok(jsonizer.serialize(GetFileContentResponse(
            content = String(Base64.getDecoder().decode(res.contentBase64), "UTF-8"),
            success = res.success,
            error = res.error
        ))))
    }}

    def checkForTask(taskId: String) = Action.async { implicit request => {
        val result = findUserForCurrentRequest(request) //NOTE: Нужно для закрытия ендпойнта авторизацией
            .flatMap(_ => juggernautClient.checkTaskStatus(taskId))
        
        val (err, status) = result.tryGetValue
        if (err != null)
            err match
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("User not found")))
                case _: TaskNotFound => wrapToFuture(BadRequest(serializeError("Task not found")))
                case _: Exception => wrapToFuture(InternalServerError(serializeError("Internal server error")))
        else wrapToFuture(Ok(status))
    }}


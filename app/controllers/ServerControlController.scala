package controllers

import javax.inject.Inject
import play.api.mvc.BaseController
import play.api.mvc.AnyContent
import play.api.mvc.Request
import components.services.serializer.JsonService
import components.clients.compositor.models.CreateServerRequest
import io.github.heavypunk.compositor.client.CompositorClient
import java.time.Duration
import play.api.mvc.ControllerComponents
import components.clients.compositor.models.{CreateServerResponse, StopServerRequest}
import io.github.heavypunk.compositor.client
import components.clients.compositor.models.{
    StopServerResponse,
    StartServerRequest,
    StartServerResponse,
    RemoveServerRequest,
    RemoveServerResponse,
    PortDescription
}
import play.api.Logger
import business.services.storages.tariffs.TariffGetter
import business.services.storages.servers.GameServerStorage
import business.entities.GameServer
import business.services.storages.tariffs.TariffStorage
import business.entities.User
import play.api.libs.typedmap.TypedKey
import components.basic.{ UserTypedKey, MessageResponse }
import components.clients.controller.ControllerUtils
import components.clients.controller.ControllerClientFactory
import io.github.heavypunk.controller.client.Settings
import business.entities.GameServerPort
import play.api.mvc
import scala.concurrent.Future
import components.clients.compositor.models.{
    GetUserServersRequest,
    ServerInfo,
    GetServersList,
    UpdateServerRequest
}
import scala.jdk.CollectionConverters._
import java.util.UUID
import business.services.storages.locations.LocationsStorage
import business.services.storages.users.UserStorage

class ServerControlController @Inject()(
    val controllerComponents: ControllerComponents,
    val compositorClient: CompositorClient,
    val controllerClientFactory: ControllerClientFactory,
    val controllerClientSettings: Settings,
    val jsonizer: JsonService,
    val tariffsGetter: TariffGetter,
    val tariffStorage: TariffStorage,
    val gameServerStorage: GameServerStorage,
    val locationsStorage: LocationsStorage,
    val userStorage: UserStorage,
) extends BaseController {

    def findUserForCurrentRequest(request: Request[AnyContent]): Option[User] = {
        val user = request.attrs.get(UserTypedKey.key)
        user
    }

    def serializeError(error: String, success: Boolean = false) = jsonizer.serialize(MessageResponse(error, success))

    def createServer(): mvc.Action[AnyContent] = Action.async { implicit request =>
        if (!request.hasBody)
            Future.successful(BadRequest(serializeError("Request body is missing")))
        else {
            val rawBody = request.body.asJson
            if (!rawBody.isDefined)
                Future.successful(BadRequest(serializeError("Invalid request body")))
            else {
                val user = findUserForCurrentRequest(request)
                if (user.isEmpty)
                    Future.successful(BadRequest(serializeError("Пользователь не найден")))
                else {
                    val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[CreateServerRequest])
                    val tariffId = reqObj.tariffId.toLongOption
                    if (tariffId.isEmpty)
                        Future.successful(BadRequest(serializeError("Номер тарифа имеет неверный формат")))
                    else {
                        val tariffOption = tariffsGetter.findTariffById(tariffId.get)
                        if (tariffOption.isEmpty)
                            Future.successful(BadRequest(serializeError(s"Тариф с номером ${tariffId.get} не найден")))
                        else {
                            val tariff = tariffOption.get
                            val serverSlug = UUID.randomUUID()
                            val respFuture = compositorClient.createServer(new io.github.heavypunk.compositor.client.models.CreateServerRequest(
                                tariff.hadrware.imageUri,
                                serverSlug.toString,
                                tariff.hadrware.availableRamBytes,
                                tariff.hadrware.availableDiskBytes,
                                tariff.hadrware.availableSwapBytes,
                                tariff.hadrware.vmExposePorts,
                            ), Duration.ofMinutes(2))

                            if (respFuture.vmId.equals(null) || respFuture.vmId.equals(""))
                                Future.successful(InternalServerError(serializeError("Не получилось создать сервер, пожалуйста, попробуйте позже")))
                            else {
                                val databaseTariff = tariffStorage.get(tariff.id)
                                val location = locationsStorage.findById(1)
                                val databaseUser = userStorage.get(user.get.id)
                                val server = GameServer()
                                server.name = reqObj.vmName
                                server.slug = serverSlug.toString
                                server.tariff = databaseTariff
                                server.owner = databaseUser
                                server.uuid = respFuture.vmId
                                server.kind = "minecraft"
                                server.location = location.get
                                if (gameServerStorage.add(server))
                                    Future.successful(Ok(jsonizer.serialize(new CreateServerResponse(respFuture.vmId, true, ""))))
                                else
                                    Future.successful(InternalServerError(serializeError(s"Не получилось сохранить сервер")))
                            }
                            
                        }
                    }
                }
            }
        }
    }

    def updateServer() = Action.async { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            Future.successful(BadRequest(serializeError("Request body is missing")))
        else {
            val rawBody = request.body.asJson
            if (!rawBody.isDefined)
                Future.successful(BadRequest(serializeError("Invalid request body")))
            else {
                val req = jsonizer.deserialize(rawBody.get.toString, classOf[UpdateServerRequest])      
                val user = findUserForCurrentRequest(request)
                if (user.isEmpty)
                    Future.successful(Forbidden(serializeError("You should specify user")))
                else {
                    val server = gameServerStorage.findByHash(req.gameServerHash)
                    if (server.isEmpty)
                        Future.successful(BadRequest(serializeError("Сервер не найден")))
                    else {
                        server.get.isPublic = req.isPublic
                        if (gameServerStorage.update(server.get)) Future.successful(Ok)
                        else Future.successful(InternalServerError(serializeError("Произошла ошибка при сохранении сервера")))
                    }
                }
            }
        }
    }}

    def stopServer(): mvc.Action[AnyContent] = Action.async { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            Future.successful(BadRequest(serializeError("Request body is missing")))
        else {
            val rawBody = request.body.asJson
            if (!rawBody.isDefined)
                Future.successful(BadRequest(serializeError("Invalid request body")))
            else {
                val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StopServerRequest])
                val resp = compositorClient.stopServer(new io.github.heavypunk.compositor.client.models.StopServerRequest(reqObj.gameServerHash), Duration.ofMinutes(2))
                val gameServer = gameServerStorage.findByHash(reqObj.gameServerHash)
                if (gameServer.isEmpty)
                    Future.successful(NotFound(serializeError(s"Сервер ${reqObj.gameServerHash} не найден")))
                else {
                    gameServer.get.isActiveVm = false
                    gameServerStorage.update(gameServer.get)
                    Future.successful(Ok(jsonizer.serialize(new StopServerResponse(resp.success, ""))))
                }
            }
        }
    }}

    def startServer(): mvc.Action[AnyContent] = Action.async { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            Future.successful(BadRequest(serializeError("Request body is missing")))
        else {
            val rawBody = request.body.asJson
            if (!rawBody.isDefined)
                Future.successful(BadRequest(serializeError("Invalid request body")))
            else {
                val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StartServerRequest])
                val resp = compositorClient.startServer(new io.github.heavypunk.compositor.client.models.StartServerRequest(reqObj.gameServerHash), Duration.ofMinutes(2))

                val gameServer = gameServerStorage.findByHash(resp.vmId)
                if (gameServer.isEmpty)
                    Future.successful(InternalServerError(serializeError(s"Сервер не найден: ${resp.vmId}")))
                else {
                    Thread.sleep(2000)
                    val controllerPortOption = ControllerUtils.findControllerPort(
                        resp.vmWhitePorts,
                        controllerClientFactory,
                        new Settings(controllerClientSettings.scheme, resp.vmWhiteIp, controllerClientSettings.port)
                    )
                    val controllerPort = if (controllerPortOption.isDefined) controllerPortOption.get else -1
                    val serializedControllerPort = GameServerPort()
                    serializedControllerPort.port = controllerPort
                    serializedControllerPort.portKind = "controller"
                    val ports = Array(serializedControllerPort) ++ (resp.vmWhitePorts map { p => {
                        val port = GameServerPort()
                        port.portKind = "none"
                        port.port = p.toInt
                        port
                    }})

                    gameServer.get.ip = resp.vmWhiteIp
                    gameServer.get.ports = ports
                    gameServer.get.isActiveVm = true
                    gameServerStorage.update(gameServer.get)

                    Future.successful(Ok(jsonizer.serialize(new StartServerResponse(
                        resp.vmId,
                        resp.vmWhiteIp,
                        ports map (p => new PortDescription(s"${p.portKind}-${p.port}", p.portKind, p.port.toString)),
                        true,
                        ""
                    ))))
                }
           }
        }
    }}

    def removeServer(): mvc.Action[AnyContent] = Action.async { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            Future.successful(BadRequest(serializeError("Request body is missing")))
        else {
            val rawBody = request.body.asJson

            if (!rawBody.isDefined)
                Future.successful(BadRequest(serializeError("Invalid request body")))
            else {
                val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[RemoveServerRequest])
                val resp = compositorClient.removeServer(new io.github.heavypunk.compositor.client.models.RemoveServerRequest(
                    reqObj.gameServerHash
                ), Duration.ofMinutes(2))
                val gameServer = gameServerStorage.findByHash(reqObj.gameServerHash)
                if (gameServer.isEmpty)
                    Future.successful(NotFound(serializeError(s"Сервер ${reqObj.gameServerHash} не найден")))
                else {
                    gameServerStorage.remove(gameServer.get)
                    Future.successful(Ok(jsonizer.serialize(new RemoveServerResponse(resp.success, resp.error))))
                }
            }
        }
    }}

    def getCompositorServers(): mvc.Action[AnyContent] = Action.async { implicit request: Request[AnyContent] => {
        val resp = compositorClient.getServerList();
        if (!resp.success)
            Future.successful(InternalServerError(serializeError(s"Hypervisor raised the error ${resp.error}")))
        else {
            val servers = GetServersList(resp.vmList map {vm => 
                val server = gameServerStorage.findByHash(vm.id)
                val (ip, ports) = if (server.isDefined) (server.get.ip, server.get.ports) else ("", Array.empty[GameServerPort])
                ServerInfo(vm.id, vm.names(0), "none", ip, ports, vm.state.equalsIgnoreCase("running"))}
            )
            val res = jsonizer.serialize(servers)
            Future.successful(Ok(res))
        }
    }}

    def getUserServers(): mvc.Action[AnyContent] = Action.async { implicit request: Request[AnyContent] => {
        if (!request.hasBody || request.body.asJson.isEmpty)
            Future.successful(BadRequest)
        else {
            val req = jsonizer.deserialize(request.body.asJson.get.toString, classOf[GetUserServersRequest])
            if (req.isPublic){
                val publicServers = gameServerStorage.findPublicServers(req.kind) // TODO: pagination
                if (publicServers.isEmpty) Future.successful(NotFound(jsonizer.serialize(GetServersList(Seq.empty))))
                else {
                    val servers = publicServers.get.asScala.toList
                    Future.successful(Ok(jsonizer.serialize(
                        GetServersList(servers map {s => ServerInfo(
                            s.uuid,
                            s.name,
                            s.kind,
                            s.ip,
                            s.ports,
                            ControllerUtils.checkForServerRunning(controllerClientFactory, new Settings(
                                controllerClientSettings.scheme,
                                controllerClientSettings.host,
                                s.ports.find(p => p.portKind.equals("controller")).get.port
                            ))
                        )}
                    ))))
                }
            } else {
                val user = findUserForCurrentRequest(request)
                if (user.isEmpty)
                    Future.successful(Forbidden(serializeError(s"Пользователь не указан")))
                else {
                    val userServers = gameServerStorage.findServersByOwner(user.get)
                    if (userServers.isEmpty)
                        Future.successful(Ok(jsonizer.serialize(GetServersList(Seq.empty))))
                    else{
                        val servers = userServers.get.asScala.toList
                        Future.successful(Ok(jsonizer.serialize(
                            GetServersList(servers map {s => ServerInfo(
                                s.uuid,
                                s.name,
                                s.kind,
                                s.ip,
                                s.ports,
                                if (s.ports.find(p => p.portKind.equals("controller")).isDefined)
                                    ControllerUtils.checkForServerRunning(controllerClientFactory, new Settings(
                                        controllerClientSettings.scheme,
                                        controllerClientSettings.host,
                                        s.ports.find(p => p.portKind.equals("controller")).get.port
                                    ))
                                else false
                            )}
                        ))))
                    }
                }
            }
        }
    }}

    def getUserServerByHash(serverHash: String) = Action.async {implicit request: Request[AnyContent] => {
        val server = gameServerStorage.findByHash(serverHash)
        if (server.isEmpty)
            Future.successful(NotFound(serializeError(s"Сервер ${serverHash} не найден")))
        else {
            if (server.get.isPublic)
                Future.successful(Ok(jsonizer.serialize(ServerInfo(
                    server.get.uuid,
                    server.get.name,
                    server.get.kind,
                    server.get.ip,
                    server.get.ports,
                    server.get.isActiveServer
                ))))
            else {
                val user = findUserForCurrentRequest(request)
                if (user.isEmpty)
                    Future.successful(Forbidden(serializeError(s"Пользователь не указан")))
                else {
                    if (server.get.owner.id != user.get.id)
                        Future.successful(Forbidden(serializeError(s"У вас нет доступа к этому серверу")))
                    else
                        Future.successful(Ok(jsonizer.serialize(ServerInfo(
                            server.get.uuid,
                            server.get.name,
                            server.get.kind,
                            server.get.ip,
                            server.get.ports,
                            server.get.isActiveServer
                        ))))
                }
            }
        }
    }}
}

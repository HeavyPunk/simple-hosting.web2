package controllers.v2.servers

import controllers.v2.SimpleHostingController
import com.google.inject.Inject
import components.services.serializer.JsonService
import play.api.mvc.ControllerComponents
import components.clients.controller.ControllerClientFactory
import io.github.heavypunk.controller.client.Settings
import components.clients.compositor.CompositorClientWrapper
import components.clients.compositor.CreateContainerRequest
import components.clients.compositor.models.CreateServerResponse
import components.basic.ResultMonad
import components.clients.compositor.models.CreateServerRequest
import business.services.storages.tariffs.TariffStorage
import components.basic.{ mapToMonad, zipWith }
import java.util.UUID
import java.time.Duration
import business.entities.GameServer
import business.services.storages.games.GamesStorage
import business.services.storages.locations.LocationsStorage
import business.services.storages.servers.GameServerStorage
import controllers.v2.JsonCannotBeParsed
import controllers.v2.JsonNotFoundForRequestBody
import controllers.v2.RequestBodyNotFound
import business.services.storages.tariffs.TariffNotFoundException
import business.services.storages.users.UserNotFoundException
import business.services.storages.games.GameNotFoundException
import business.services.storages.locations.LocationNotFoundException
import controllers.v2.UserNotFoundForRequest
import components.clients.compositor.models.StartVmRequest
import components.services.retrier.Retrier
import components.clients.controller.ControllerUtils
import components.services.log.Log
import business.entities.GameServerPort
import components.clients.compositor.models.StartServerResponse
import components.clients.compositor.models.PortDescription
import scala.collection.immutable.ArraySeq
import business.services.storages.servers.GameServerNotFoundException
import components.basic.serializeForLog
import components.clients.compositor.models.StopServerRequest
import components.clients.compositor.models.StopServerResponse
import components.clients.compositor.models.UpdateServerRequest
import components.clients.compositor.models.RemoveServerRequest
import components.clients.compositor.models.RemoveServerResponse
import components.clients.compositor.ContainerNotRemoved
import components.clients.compositor.ContainerNotCreated
import components.basic.ErrorMonad
import components.clients.compositor.models.ServerInfo
import components.clients.compositor.models.GetUserServersRequest
import components.clients.compositor.models.GetServersList
import components.clients.controller.GetServerLogsOnPageRequest
import io.github.heavypunk.controller.client.contracts.server.GetServerLogsRequest
import io.github.heavypunk.controller.client.contracts.server.GetServerLogsResponse
import components.clients.controller.SendServerMessageRequest
import io.github.heavypunk.controller.client.contracts.server.SendMessageRequest
import io.github.heavypunk.controller.client.contracts.server.SendMessageResponse
import io.github.heavypunk.controller.client.contracts.server.GetServerInfoResponse
import components.clients.controller.GetServerInfoRequest
import components.clients.controller.StartGameServerRequest
import io.github.heavypunk.controller.client.contracts.server.StartServerRequest
import components.clients.controller.StopGameServerRequest
import components.services.business.servers.ServersManagementService
import components.services.business.servers.CreateVmRequest
import components.services.business.servers.StopVmRequest
import components.services.business.servers.UpdateVmRequest
import components.services.business.servers.RemoveVmRequest
import components.services.business.servers.VmNotFound
import components.clients.compositor.models.CreateVmWithGameServerRequest
import components.clients.compositor.models.CreateVmWithGameServerResponse
import components.clients.compositor.models.StopVmWithGameServerRequest
import components.services.business.servers.RestartVmRequest

class TariffHasInvalidFormat
class AccessDenied
class ErrorWhenGettingLogs(val response: GetServerLogsResponse)
class ErrorWhenSendingMessageToServer(val response: SendMessageResponse)
class ErrorWhenGettingServerInfo(val response: GetServerInfoResponse)
class ErrorWhenStartingServer(val response: io.github.heavypunk.controller.client.contracts.server.StartServerResponse)
class ErrorWhenStoppingServer(val response: io.github.heavypunk.controller.client.contracts.server.StopServerResponse)

class GameServerControlControllerV2 @Inject() (
    val controllerComponents: ControllerComponents,
    val serversManagementService: ServersManagementService,
    val controllerClientFactory: ControllerClientFactory,
    val controllerClientSettings: Settings,
    val tariffStorage: TariffStorage,
    val gamesStorage: GamesStorage,
    val locationsStorage: LocationsStorage,
    val gameServerStorage: GameServerStorage,
    val jsonizer: JsonService,
    val log: Log
) extends SimpleHostingController(jsonizer) {

    def createVmWithGameServer() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[CreateVmWithGameServerRequest](request)
        val user = findUserForCurrentRequest(request)
        val tariffId = req.flatMap(r => r.tariffId.toLongOption.mapToMonad(TariffHasInvalidFormat()))
        val vmSlug = UUID.randomUUID()
        val tariff = tariffId.flatMap(tId => tariffStorage.findTariffById(tId))
        val game = req.flatMap(r => gamesStorage.findGameById(r.gameId))
        val location = req.flatMap(r => locationsStorage.findLocationById(r.locationId))
        val vmCreated = req.zipWith(user, tariff, game, location)
            .flatMap((req, user, tariff, game, location) => serversManagementService.createVm(CreateVmRequest(
                tariff,
                req.vmName,
                vmSlug,
                user,
                game,
                location
            )))
        val vmStarted = vmCreated.zipWith(user)
            .flatMap((vmCreated, user) => serversManagementService.startVm(components.services.business.servers.StartVmRequest(
                user,
                vmCreated.container.vmId
            )))
        val gameServerStarted = vmStarted.zipWith(vmCreated, req, user)
            .flatMap((_, vmCreated, req, user) => serversManagementService.startGameServer(components.services.business.servers.StartGameServerRequest(
                user,
                vmCreated.container.vmId,
                req.saveStdout,
                req.saveStderr
            ))).zipWith(vmCreated)

        val (err, result) = gameServerStarted.tryGetValue
        if (err != null)
            err match
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("Cannot parse JSON")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("JSON in body not found")))
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request body not found")))
                case _: TariffHasInvalidFormat => wrapToFuture(BadRequest(serializeError("Tariff id has invalid format")))
                case _: TariffNotFoundException => wrapToFuture(NotFound(serializeError("Tariff not found")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("User not found for request")))
                case _: GameNotFoundException => wrapToFuture(NotFound(serializeError("Game not found")))
                case _: LocationNotFoundException => wrapToFuture(NotFound(serializeError("Location not found")))
                case _: ContainerNotCreated => wrapToFuture(InternalServerError(serializeError("InternalServerError")))
                case _: VmNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case e: ErrorWhenStartingServer => wrapToFuture(InternalServerError(jsonizer.serialize(e)))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError(serializeError("InternalServerError")))
        else wrapToFuture(Ok(jsonizer.serialize(CreateVmWithGameServerResponse(
            result._2.container.vmId,
            result._1.success,
            result._1.error
        ))))
    }}

    def stopVmWithGameServer() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[StopVmWithGameServerRequest](request)
        val user = findUserForCurrentRequest(request)
        val gamesServerStopped = req.zipWith(user)
            .flatMap((req, user) => serversManagementService.stopGameServer(components.services.business.servers.StopGameServerRequest(
                user,
                req.gameServerHash,
                req.force
            )))
        val vmStopped = gamesServerStopped.zipWith(req, user)
            .flatMap((_, req, user) => serversManagementService.stopVm(components.services.business.servers.StopVmRequest(
                user,
                req.gameServerHash
            )))

        val (err, result) = vmStopped.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("User not found for request body")))
                case _: VmNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case e: ErrorWhenStoppingServer => wrapToFuture(InternalServerError(jsonizer.serialize(e)))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(result)))
    }}

    def createVm() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[CreateServerRequest](request)
        val user = findUserForCurrentRequest(request)
        val tariffId = req.flatMap(r => r.tariffId.toLongOption.mapToMonad(TariffHasInvalidFormat()))
        val vmSlug = UUID.randomUUID()
        val tariff = tariffId.flatMap(tId => tariffStorage.findTariffById(tId))
        val game = req.flatMap(r => gamesStorage.findGameById(r.gameId))
        val location = req.flatMap(r => locationsStorage.findLocationById(r.locationId))

        val result = req.zipWith(user, tariff, game, location)
            .flatMap((request, user, tariff, game, location) => serversManagementService.createVm(CreateVmRequest(
                tariff,
                request.vmName,
                vmSlug,
                user,
                game,
                location
            )))
        
        val (err, r) = result.tryGetValue
        if (err != null)
            err match
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("Cannot parse JSON")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("JSON in body not found")))
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request body not found")))
                case _: TariffHasInvalidFormat => wrapToFuture(BadRequest(serializeError("Tariff id has invalid format")))
                case _: TariffNotFoundException => wrapToFuture(NotFound(serializeError("Tariff not found")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("User not found for request")))
                case _: GameNotFoundException => wrapToFuture(NotFound(serializeError("Game not found")))
                case _: LocationNotFoundException => wrapToFuture(NotFound(serializeError("Location not found")))
                case _: ContainerNotCreated => wrapToFuture(InternalServerError(serializeError("InternalServerError")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError(serializeError("InternalServerError")))
        else wrapToFuture(Ok(jsonizer.serialize(CreateServerResponse(r.container.vmId, true, ""))))
    }}

    def startVm() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[StartVmRequest](request)
        val user = findUserForCurrentRequest(request)
        
        val result = req.zipWith(user)
            .flatMap((request, user) => serversManagementService.startVm(components.services.business.servers.StartVmRequest(
                user,
                request.gameServerHash
            )))


        val (err, startServerResponse) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("User not found for request body")))
                case _: VmNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(startServerResponse)))
    }}

    def stopVm() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[StopServerRequest](request)
        val user = findUserForCurrentRequest(request)
        val result = req.zipWith(user)
            .flatMap((req, user) => serversManagementService.stopVm(StopVmRequest(
                user,
                req.gameServerHash
            )))
        val (err, resp) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("User not found for request body")))
                case _: VmNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(resp)))
    }}

    def removeVm(containerHash: String) = Action.async { implicit request => {
        val user = findUserForCurrentRequest(request)
        val result = user.flatMap(user => serversManagementService.removeVm(RemoveVmRequest(
            user,
            containerHash
        )))
        val (err, resp) = result.tryGetValue
        if (err != null)
            err match 
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: VmNotFound => wrapToFuture(BadRequest(serializeError("Server not found. Maybe you don't have access to this server")))
                case _: ContainerNotRemoved => wrapToFuture(InternalServerError(("Remove server error")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(""))
    }}

    def restartVm(containerHash: String) = Action.async { implicit request => {
        val user = findUserForCurrentRequest(request)
        val result = user.flatMap(user => serversManagementService.restartVm(RestartVmRequest(
            user,
            containerHash
        )))
        val (err, resp) = result.tryGetValue
        if (err != null)
            err match 
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: VmNotFound => wrapToFuture(BadRequest(serializeError("Server not found. Maybe you don't have access to this server")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(""))
    }}

    def updateVm() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[UpdateServerRequest](request)
        val user = findUserForCurrentRequest(request)
        val result = req.zipWith(user)
            .flatMap((req, user) => serversManagementService.updateVm(UpdateVmRequest(
                user,
                req.gameServerHash,
                req.isPublic
            )))
        val (err, updated) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: VmNotFound => wrapToFuture(BadRequest(serializeError("Server not found. Maybe you don't have access to this server")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok)
    }}

    def getUserServers() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[GetUserServersRequest](request)
        val user = findUserForCurrentRequest(request)
        val result = req.zipWith(user).flatMap((r, u) => {
            if(r.isPublic)
                gameServerStorage.findPublicServers(r.kind)
            else gameServerStorage.findServersByOwner(u)
        })
        .flatMap(servers => ResultMonad(GetServersList(servers map { s => ServerInfo(
            s.uuid,
            s.name,
            s.kind,
            s.ip,
            s.ports,
            ControllerUtils.checkForServerRunning(controllerClientFactory, Settings(
                controllerClientSettings.scheme,
                controllerClientSettings.host,
                s.ports.find(_.portKind.equals("controller")).getOrElse(GameServerPort()).port
            ))
        )})))

        val (err, servers) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(servers)))
    }}

    def getUserServerByHash(serverHash: String) = Action.async { implicit request => {
        val user = findUserForCurrentRequest(request)
        val server = gameServerStorage.findByHash(serverHash)
        val result = server.zipWith(user)
            .flatMap((s, u) => {
                if (s.isPublic)
                    ResultMonad(s)
                else if (s.owner.id == u.id) ResultMonad(s) else ErrorMonad(AccessDenied())
            })
            .flatMap(s => ResultMonad(ServerInfo(
                s.uuid,
                s.name,
                s.kind,
                s.ip,
                s.ports,
                s.isActiveServer
            )))
        
        val (err, serv) = result.tryGetValue
        if (err != null)
            err match
                case _: GameServerNotFoundException => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case e: Exception => wrapToFuture(InternalServerError(serializeError("InternalServerError")))
        else wrapToFuture(Ok(jsonizer.serialize(serv)))
    }}

    def startGameServer() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[StartGameServerRequest](request)
        val user = findUserForCurrentRequest(request)
        
        val result = req.zipWith(user)
            .flatMap((req, user) => serversManagementService.startGameServer(components.services.business.servers.StartGameServerRequest(
                user,
                req.gameServerHash,
                req.saveStdout,
                req.saveStderr
            )))
        
        val (err, response) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: VmNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case e: ErrorWhenStartingServer => wrapToFuture(InternalServerError(jsonizer.serialize(e)))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(response)))
    }}

    def stopGameServer() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[StopGameServerRequest](request)
        val gameServer = req.flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val user = findUserForCurrentRequest(request)
        
        val result = req.zipWith(user)
            .flatMap((req, user) => serversManagementService.stopGameServer(components.services.business.servers.StopGameServerRequest(
                user,
                req.gameServerHash,
                req.force
            )))
        
        val (err, response) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: VmNotFound => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case e: ErrorWhenStoppingServer => wrapToFuture(InternalServerError(jsonizer.serialize(e)))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(response)))
    }}

    def getServerInfo() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[GetServerInfoRequest](request)
        val gameServer = req.flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val user = findUserForCurrentRequest(request)
        val grantedGameServer = gameServer.zipWith(user)
            .flatMap((s, u) => if (s.owner.id == u.id) ResultMonad(s) else ErrorMonad(AccessDenied()))
        
        val controllerClient = grantedGameServer.flatMap(s =>
            ResultMonad(controllerClientFactory.getControllerClient(Settings(
                controllerClientSettings.scheme,
                s.ip,
                s.ports.find(_.portKind.equalsIgnoreCase("controller")).getOrElse(GameServerPort()).port
            )))
        )
        val result = controllerClient.zipWith(req)
            .flatMap((cc, r) => {
                val response = cc.servers.getServerInfo(io.github.heavypunk.controller.client.contracts.server.GetServerInfoRequest(
                    r.postSystem
                ), Duration.ofMinutes(2))
                if (response.success) ResultMonad(response)
                else ErrorMonad(ErrorWhenGettingServerInfo(response))
        })
        
        val (err, response) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: GameServerNotFoundException => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case e: ErrorWhenGettingServerInfo => wrapToFuture(InternalServerError(jsonizer.serialize(e)))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(response)))
    }}

    def sendServerMessage() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[SendServerMessageRequest](request)
        val gameServer = req.flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val user = findUserForCurrentRequest(request)
        val grantedGameServer = gameServer.zipWith(user)
            .flatMap((s, u) => if (s.owner.id == u.id) ResultMonad(s) else ErrorMonad(AccessDenied()))
        
        val controllerClient = grantedGameServer.flatMap(s =>
            ResultMonad(controllerClientFactory.getControllerClient(Settings(
                controllerClientSettings.scheme,
                s.ip,
                s.ports.find(_.portKind.equalsIgnoreCase("controller")).getOrElse(GameServerPort()).port
            )))
        )
        val result = controllerClient.zipWith(req)
            .flatMap((cc, r) => {
                val response = cc.servers.sendMessage(SendMessageRequest(
                    r.message,
                    r.postSystem
                ), Duration.ofMinutes(2))
                if (response.success) ResultMonad(response)
                else ErrorMonad(ErrorWhenSendingMessageToServer(response))
        })
        
        val (err, response) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: GameServerNotFoundException => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case e: ErrorWhenSendingMessageToServer => wrapToFuture(InternalServerError(jsonizer.serialize(e)))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(response)))
    }}

    def getServerLogs() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[GetServerLogsOnPageRequest](request)
        val user = findUserForCurrentRequest(request)
        val gameServer = req
            .flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val grantedGameServer = gameServer.zipWith(user)
            .flatMap((s, u) => {
                if (s.owner.id == u.id)
                    ResultMonad(s)
                else ErrorMonad(AccessDenied())
            })

        val controllerClient = grantedGameServer.flatMap(s =>
            ResultMonad(controllerClientFactory.getControllerClient(Settings(
                controllerClientSettings.scheme,
                s.ip,
                s.ports.find(_.portKind.equalsIgnoreCase("controller")).getOrElse(GameServerPort()).port
            )))
        )
        val result = controllerClient.zipWith(req)
            .flatMap((cc, r) => {
                val response = if (r.isLastLogs) cc.servers.getServerLogsLastPage(Duration.ofMinutes(2))
                else cc.servers.getServerLogs(GetServerLogsRequest(r.page), Duration.ofMinutes(2))
                if (response.success) ResultMonad(response)
                else ErrorMonad(ErrorWhenGettingLogs(response))
            })
        val (err, logs) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You must provide user token in X-Auth-Token header")))
                case _: GameServerNotFoundException => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case _: AccessDenied => wrapToFuture(Forbidden(serializeError("You don't have access to this server")))
                case e: ErrorWhenGettingLogs => wrapToFuture(InternalServerError(jsonizer.serialize(e)))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(logs)))
    }}
}

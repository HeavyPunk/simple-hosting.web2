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

class TariffHasInvalidFormat
class VmNotFound
class AccessDenied
class ErrorWhenGettingLogs(val response: GetServerLogsResponse)
class ErrorWhenSendingMessageToServer(val response: SendMessageResponse)
class ErrorWhenGettingServerInfo(val response: GetServerInfoResponse)
class ErrorWhenStartingServer(val response: io.github.heavypunk.controller.client.contracts.server.StartServerResponse)
class ErrorWhenStoppingServer(val response: io.github.heavypunk.controller.client.contracts.server.StopServerResponse)

class GameServerControlControllerV2 @Inject() (
    val controllerComponents: ControllerComponents,
    val controllerClientFactory: ControllerClientFactory,
    val controllerClientSettings: Settings,
    val compositorWrapper: CompositorClientWrapper,
    val tariffStorage: TariffStorage,
    val gamesStorage: GamesStorage,
    val locationsStorage: LocationsStorage,
    val gameServerStorage: GameServerStorage,
    val jsonizer: JsonService,
    val log: Log
) extends SimpleHostingController(jsonizer) {
    def createVm() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[CreateServerRequest](request)
        val user = findUserForCurrentRequest(request)
        val tariffId = req.flatMap(r => r.tariffId.toLongOption.mapToMonad(TariffHasInvalidFormat()))
        val vmSlug = UUID.randomUUID()
        val tariff = tariffId.flatMap(tId => tariffStorage.findTariffById(tId))
        val game = req.flatMap(r => gamesStorage.findGameById(r.gameId))
        val location = req.flatMap(r => locationsStorage.findLocationById(r.locationId))

        val container = tariff
            .zipWith(req)
            .flatMap((t, r) => compositorWrapper.createContainerMonad(CreateContainerRequest(
                t.specification.imageUri,
                vmSlug.toString,
                t.specification.availableRamBytes,
                t.specification.availableDiskBytes,
                t.specification.availableSwapBytes,
                t.specification.vmExposePorts.map(_.port)
            )))
        val result = container
            .zipWith(req, tariff, user, game, location)
            .flatMap((c, r, t, u, g, l) => {
                val server = GameServer()
                server.name = r.vmName
                server.slug = vmSlug.toString
                server.tariff = t
                server.owner = u
                server.uuid = c.vmId
                server.kind = g.name
                server.location = l
                gameServerStorage.add(server)
            })
            .zipWith(container)
        
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
        else wrapToFuture(Ok(jsonizer.serialize(CreateServerResponse(r._2.vmId, true, ""))))
    }}

    def startVm() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[StartVmRequest](request)
        val startContainerReq = req.flatMap(r => compositorWrapper.startContainerMonad(r.gameServerHash))
        val gameServer = startContainerReq.flatMap(r => gameServerStorage.findByHash(r.vmId))
        val ports = startContainerReq
            .zipWith(gameServer)
            .flatMap((r, g) => 
                {
                    val searchRes = Retrier.work(
                        action = {
                            ControllerUtils.findControllerPort(
                                ArraySeq.unsafeWrapArray(r.vmWhitePorts), //Non-copy array
                                controllerClientFactory,
                                Settings(controllerClientSettings.scheme, r.vmWhiteIp, controllerClientSettings.port)
                            )
                        },
                        isSuccess = p => p.isDefined,
                        delay = Duration.ofSeconds(2),
                        onAttempt = a => log.info(s"Attempting to connect to controller of server ${g.slug}, attempt $a")
                    )
                    val controllerPort = if (searchRes.isDefined) searchRes.get.get else -1
                    val serializedControllerPort = GameServerPort()
                    serializedControllerPort.port = controllerPort
                    serializedControllerPort.portKind = "controller"
                    val ports = Array(serializedControllerPort) ++ (r.vmWhitePorts map { p => {
                        val port = GameServerPort()
                        port.portKind = "none"
                        port.port = p.toInt
                        port
                    }})
                    ResultMonad(ports)
                })
        val serverUpdated = startContainerReq.zipWith(ports, gameServer)
            .flatMap((container, ports, server) =>  {
                server.ip = container.vmWhiteIp
                server.ports = ports
                server.isActiveVm = true
                gameServerStorage.update(server)
            })
        val result = startContainerReq.zipWith(ports, serverUpdated)
            .flatMap((container, ports, _) => {
                ResultMonad(StartServerResponse(
                    container.vmId,
                    container.vmWhiteIp,
                    ports map { p => PortDescription(s"${p.portKind}-${p.port}", p.portKind, p.port.toString)},
                    true,
                    ""
                ))
            })

        val (err, startServerResponse) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: GameServerNotFoundException => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(startServerResponse)))
    }}

    def stopVm() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[StopServerRequest](request)
        val stopServerResponse = req
            .flatMap(r => compositorWrapper.stopContainerMonad(r.gameServerHash))
        val gameServer = req.flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val result = stopServerResponse.zipWith(gameServer)
            .flatMap((_, gameServer) => {
                gameServer.isActiveVm = false
                gameServerStorage.update(gameServer)
            })
            .zipWith(stopServerResponse)
            .flatMap((_, r) => ResultMonad(StopServerResponse(r, "")))
        val (err, resp) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: GameServerNotFoundException => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(resp)))
    }}

    def updateVm() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[UpdateServerRequest](request)
        val user = findUserForCurrentRequest(request)
        val servers = user.flatMap(u => gameServerStorage.findServersByOwner(u))
        val server = servers.zipWith(req)
            .flatMap((s, r) => s find {i => i.uuid.equals(r.gameServerHash)} mapToMonad(VmNotFound()))
        val result = server.zipWith(req)
            .flatMap((s, r) => {
                s.isPublic = r.isPublic
                gameServerStorage.update(s)
            })
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

    def removeVm() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[RemoveServerRequest](request)
        val removeResponse = req.flatMap(r => compositorWrapper.removeContainerMonad(r.gameServerHash))
        val gameServer = req
            .flatMap(r => gameServerStorage.findByHash(r.gameServerHash))
        val result = gameServer
            .zipWith(removeResponse)
            .flatMap((server, _) => gameServerStorage.remove(server))
        val (err, removed) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("Request don't contains body")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("Json not found for request body")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("JSON cannot be parsed")))
                case _: GameServerNotFoundException => wrapToFuture(BadRequest(serializeError("Game server not found")))
                case e: ContainerNotRemoved => wrapToFuture(BadRequest(jsonizer.serialize(RemoveServerResponse(false, e.error))))
                case e: Exception => wrapToFuture(InternalServerError(serializeError("InternalServerError")))
        else wrapToFuture(Ok(jsonizer.serialize(RemoveServerResponse(true, ""))))
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
                val response = cc.servers.startServer(io.github.heavypunk.controller.client.contracts.server.StartServerRequest(
                    r.saveStdout,
                    r.saveStderr
                ), Duration.ofMinutes(2))
                if (response.success) ResultMonad(response)
                else ErrorMonad(ErrorWhenStartingServer(response))
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
                case e: ErrorWhenStartingServer => wrapToFuture(InternalServerError(jsonizer.serialize(e)))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(response)))
    }}

    def stopGameServer() = Action.async { implicit request => {
        val req = getModelFromJsonRequest[StopGameServerRequest](request)
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
                val response = cc.servers.stopServer(io.github.heavypunk.controller.client.contracts.server.StopServerRequest(
                    r.force
                ), Duration.ofMinutes(2))
                if (response.success) ResultMonad(response)
                else ErrorMonad(ErrorWhenStoppingServer(response))
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

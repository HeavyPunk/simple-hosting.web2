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
import components.basic.UserTypedKey
import components.clients.controller.ControllerUtils
import components.clients.controller.ControllerClientFactory
import io.github.heavypunk.controller.client.Settings
import business.entities.GameServerPort

class ServerControlController @Inject()(
    val controllerComponents: ControllerComponents,
    val compositorClient: CompositorClient,
    val controllerClientFactory: ControllerClientFactory,
    val controllerClientSettings: Settings,
    val jsonizer: JsonService,
    val tariffsGetter: TariffGetter,
    val tariffStorage: TariffStorage,
    val gameServerStorage: GameServerStorage
) extends BaseController {

    def findUserForCurrentRequest(request: Request[AnyContent]): Option[User] = {
        val user = request.attrs.get(UserTypedKey.key)
        user
    }

    def createServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest
        
        val user = findUserForCurrentRequest(request)
        if (user.isEmpty)
            BadRequest(s"User not found")
        
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[CreateServerRequest])
        val tariffId = reqObj.tariffId.toLongOption
        if (tariffId.isEmpty)
            BadRequest("Tariff id has invalid format")
        var tariffOption = tariffsGetter.findTariffById(tariffId.get)
        if (tariffOption.isEmpty)
            BadRequest(s"Tariff with id ${tariffId.get} not found")
        val tariff = tariffOption.get
        val resp = compositorClient.createServer(new io.github.heavypunk.compositor.client.models.CreateServerRequest(
            tariffOption.get.hadrware.imageUri,
            reqObj.vmName,
            tariff.hadrware.availableRamBytes,
            tariff.hadrware.availableDiskBytes,
            tariff.hadrware.availableSwapBytes,
            tariff.hadrware.vmExposePorts,
        ), Duration.ofMinutes(2))
        val databaseTariff = tariffStorage.get(tariff.id)
            
        val server = GameServer()
        server.name = reqObj.vmName
        server.tariff = databaseTariff
        server.owner = user.get
        server.uuid = resp.vmId
        gameServerStorage.add(server) 

        Ok(jsonizer.serialize(new CreateServerResponse(resp.vmId, true, "")))
    }}

    def stopServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StopServerRequest])
        val resp = compositorClient.stopServer(new io.github.heavypunk.compositor.client.models.StopServerRequest(reqObj.gameServerId), Duration.ofMinutes(2))
        
        Ok(jsonizer.serialize(new StopServerResponse(resp.success, "")))
    }}

    def startServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StartServerRequest])
        val resp = compositorClient.startServer(new io.github.heavypunk.compositor.client.models.StartServerRequest(reqObj.gameServerId), Duration.ofMinutes(2))

        val gameServer = gameServerStorage.findByUUID(resp.vmId)
        if (gameServer.isEmpty)
            InternalServerError(s"Game server not found: ${resp.vmId}")
        val controllerPortOption = ControllerUtils.findControllerPort(resp.vmWhitePorts, controllerClientFactory, controllerClientSettings)
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
        gameServerStorage.update(gameServer.get)

        Ok(jsonizer.serialize(new StartServerResponse(
            resp.vmId,
            resp.vmWhiteIp,
            ports map (p => new PortDescription(p.portKind, p.port.toString)),
            true,
            ""
        )))
    }}

    def removeServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBody = request.body.asJson

        if (!rawBody.isDefined)
            BadRequest
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[RemoveServerRequest])
        val resp = compositorClient.removeServer(new io.github.heavypunk.compositor.client.models.RemoveServerRequest(
            reqObj.gameServerId
        ), Duration.ofMinutes(2))
        Ok(jsonizer.serialize(new RemoveServerResponse(resp.success, resp.error)))
    }}
}

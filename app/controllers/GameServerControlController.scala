package controllers

import com.google.inject.Inject
import io.github.heavypunk.controller.client.ControllerClient
import components.services.serializer.JsonService
import play.api.mvc.{ ControllerComponents, BaseController, AnyContent, Request }
import io.github.heavypunk.controller.client.contracts.server.{
    StartServerResponse,
    StartServerRequest,
    StopServerRequest,
}
import components.clients.controller.StartGameServerRequest
import java.time.Duration
import components.clients.controller.ControllerClientFactory
import business.services.storages.servers.GameServerStorage
import io.github.heavypunk.controller.client.Settings
import components.clients.controller.StopGameServerRequest

class GameServerControlController @Inject() (
    val controllerComponents: ControllerComponents,
    val controllerClientFactory: ControllerClientFactory,
    val controllerClientSettings: Settings,
    val jsonizer: JsonService,
    val gameServerStorage: GameServerStorage
) extends BaseController {


    def startServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest
        
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StartGameServerRequest])
        val gameServer = gameServerStorage.findByUUID(reqObj.gameServerId)
        if (gameServer.isEmpty)
            BadRequest(s"Game server with key ${reqObj.gameServerId} not found")
        
        val controllerClient = controllerClientFactory.getControllerClient(
            new Settings(
                controllerClientSettings.scheme,
                gameServer.get.ip,
                gameServer.get.ports.find(_.portKind.equalsIgnoreCase("controller")).get.port
            )
        )

        val resp = controllerClient.servers.startServer(
            new StartServerRequest(reqObj.saveStdout, reqObj.saveStderr),
            Duration.ofMinutes(2)
        )
        Ok(jsonizer.serialize(resp))
    }}

    def stopServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest

        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StopGameServerRequest])

        val gameServer = gameServerStorage.findByUUID(reqObj.gameServerId)
        if (gameServer.isEmpty)
            BadRequest(s"Game server with key ${reqObj.gameServerId} not found")
        

        val controllerClient = controllerClientFactory.getControllerClient(
            new Settings(
                controllerClientSettings.scheme,
                gameServer.get.ip,
                gameServer.get.ports.find(_.portKind.equalsIgnoreCase("controller")).get.port
            )
        )
        
        val resp = controllerClient.servers.stopServer(
            new StopServerRequest(reqObj.force),
            Duration.ofMinutes(2))
        Ok(jsonizer.serialize(resp))
    }}
}

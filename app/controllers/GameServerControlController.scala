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
import play.api.mvc
import components.basic.UserTypedKey
import business.entities.User
import scala.concurrent.Future

class GameServerControlController @Inject() (
    val controllerComponents: ControllerComponents,
    val controllerClientFactory: ControllerClientFactory,
    val controllerClientSettings: Settings,
    val jsonizer: JsonService,
    val gameServerStorage: GameServerStorage
) extends BaseController {

    def findUserForCurrentRequest(request: Request[AnyContent]): Option[User] = {
        val user = request.attrs.get(UserTypedKey.key)
        user
    }

    def startServer(): mvc.Action[AnyContent] = Action.async { implicit request =>
        if (!request.hasBody)
            Future.successful(BadRequest("Request body is missing"))
        else {
            val rawBody = request.body.asJson
            if (!rawBody.isDefined)
                Future.successful(BadRequest("Invalid request body"))
            else {
                val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StartGameServerRequest])

                val gameServer = gameServerStorage.findByHash(reqObj.gameServerId)
                if (gameServer.isEmpty)
                    Future.successful(BadRequest(s"Game server with key ${reqObj.gameServerId} not found"))
                else {
                    val user = findUserForCurrentRequest(request)
                    if (user.isEmpty)
                        Future.successful(BadRequest("User must be specified"))
                    else if (!gameServer.get.owner.id.equals(user.get.id))
                        Future.successful(Forbidden("You don't have permission to manipulate this game server"))
                    else {
                        val controllerClient = controllerClientFactory.getControllerClient(
                            new Settings(
                                controllerClientSettings.scheme,
                                gameServer.get.ip,
                                gameServer.get.ports.find(_.portKind.equalsIgnoreCase("controller")).get.port
                            )
                        )

                        val startFuture = controllerClient.servers.startServer(
                            new StartServerRequest(reqObj.saveStdout, reqObj.saveStderr),
                            Duration.ofMinutes(2)
                        )
                        if (!startFuture.success) Future.successful(InternalServerError(jsonizer.serialize(startFuture)))
                        else {
                            gameServer.get.isActiveServer = true
                            gameServerStorage.update(gameServer.get)
                            Future.successful(Ok(jsonizer.serialize(startFuture)))
                        }
                    }
                }
            }
        }
    }

    def stopServer(): mvc.Action[AnyContent] = Action.async { implicit request =>
    if (!request.hasBody)
        Future.successful(BadRequest("Request body is missing"))
    else {
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            Future.successful(BadRequest("Invalid request body"))
        else {
            val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StopGameServerRequest])

            val gameServer = gameServerStorage.findByHash(reqObj.gameServerId)
            if (gameServer.isEmpty)
                Future.successful(BadRequest(s"Game server with key ${reqObj.gameServerId} not found"))
            else {
                val user = findUserForCurrentRequest(request)
                if (user.isEmpty)
                    Future.successful(BadRequest("User must be specified"))
                else if (!gameServer.get.owner.id.equals(user.get.id))
                    Future.successful(Forbidden("You don't have permission to manipulate this game server"))
                else {
                    val controllerClient = controllerClientFactory.getControllerClient(
                        new Settings(
                            controllerClientSettings.scheme,
                            gameServer.get.ip,
                            gameServer.get.ports.find(_.portKind.equalsIgnoreCase("controller")).get.port
                        )
                    )

                    val stopFuture = controllerClient.servers.stopServer(
                        new StopServerRequest(reqObj.force),
                        Duration.ofMinutes(2)
                    )
                    if (!stopFuture.success) Future.successful(InternalServerError(jsonizer.serialize(stopFuture)))
                    else {
                        gameServer.get.isActiveServer = false
                        gameServerStorage.update(gameServer.get)
                        Future.successful(Ok(jsonizer.serialize(stopFuture)))
                    }
                }
            }
        }
    }
  }
}

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
import java.time.Duration

class GameServerController @Inject() (
    val controllerComponents: ControllerComponents,
    val controllerClient: ControllerClient,
    val jsonizer: JsonService
) extends BaseController {

    def startServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest
        
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StartServerRequest])
        val resp = controllerClient.servers.startServer(reqObj, Duration.ofMinutes(2))
        Ok(jsonizer.serialize(resp))
    }}

    def stopServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest

        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StopServerRequest])
        val resp = controllerClient.servers.stopServer(reqObj, Duration.ofMinutes(2))
        Ok(jsonizer.serialize(resp))
    }}
}

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

class ServerControlController @Inject()(
    val controllerComponents: ControllerComponents,
    val compositorClient: CompositorClient,
    val jsonizer: JsonService 
) extends BaseController {

    def createServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest
        
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[CreateServerRequest])
        val resp = compositorClient.createServer(new io.github.heavypunk.compositor.client.models.CreateServerRequest(
            "",
            reqObj.vmName,
            0,
            0,
            0,
            Array("")
        ), Duration.ofMinutes(2))
        
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
        Ok(jsonizer.serialize(new StartServerResponse(resp.vmId, resp.vmWhiteIp, Array(new PortDescription("", "")), true, "")))
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

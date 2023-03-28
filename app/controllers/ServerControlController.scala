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
import components.clients.compositor.models.StopServerResponse
import components.clients.compositor.models.StartServerRequest
import components.clients.compositor.models.StartServerResponse
import components.clients.compositor.models.RemoveServerRequest
import components.clients.compositor.models.RemoveServerResponse

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
            reqObj.vmImageUri,
            reqObj.vmName,
            reqObj.vmAvailableRamBytes,
            reqObj.vmAvailableDiskBytes,
            reqObj.vmAvailableSwapBytes,
            reqObj.vmExposePorts
        ), Duration.ofMinutes(2))
        
        Ok(jsonizer.serialize(new CreateServerResponse(resp.vmId)))
    }}

    def stopServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StopServerRequest])
        val resp = compositorClient.stopServer(new io.github.heavypunk.compositor.client.models.StopServerRequest(reqObj.vmId), Duration.ofMinutes(2))
        
        Ok(jsonizer.serialize(new StopServerResponse(resp.success, "")))
    }}

    def startServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        
        val rawBody = request.body.asJson
        if (!rawBody.isDefined)
            BadRequest
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[StartServerRequest])
        val resp = compositorClient.startServer(new io.github.heavypunk.compositor.client.models.StartServerRequest(reqObj.vmId), Duration.ofMinutes(2))
        Ok(jsonizer.serialize(new StartServerResponse(resp.vmId, resp.vmWhiteIp, resp.vmWhitePorts, true, "")))
    }}

    def removeServer() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBody = request.body.asJson

        if (!rawBody.isDefined)
            BadRequest
        val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[RemoveServerRequest])
        val resp = compositorClient.removeServer(new io.github.heavypunk.compositor.client.models.RemoveServerRequest(
            reqObj.vmId
        ), Duration.ofMinutes(2))
        Ok(jsonizer.serialize(new RemoveServerResponse(resp.success, resp.error)))
    }}
}

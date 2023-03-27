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
import components.clients.compositor.models.CreateServerResponse

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
}

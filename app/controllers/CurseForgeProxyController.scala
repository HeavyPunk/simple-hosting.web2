package controllers

import javax.inject.Inject
import play.api.mvc.ControllerComponents
import play.api.mvc.BaseController
import play.api.mvc._
import components.clients.curseforge.CurseForgeClient
import components.clients.curseforge.models.GetModsRequest
import dtos.curseforge.CallExternalApiResponse
import dtos.curseforge.CallExternalApiRequest
import com.google.gson.Gson
import components.clients.curseforge.CommonCurseForgeClient
import components.clients.curseforge.ClientSettings

class CurseForgeProxyController @Inject()(
    val controllerComponents: ControllerComponents,
    // val curseForgeClient: CurseForgeClient
) extends BaseController{
    val jsoner = new Gson() // TODO: to json manager
    val curseForgeClient = new CommonCurseForgeClient(new ClientSettings("", ""))

    def callExternalApi() = Action {
        implicit request: Request[AnyContent] => {
            if (!request.hasBody)
                BadRequest
            val rawBody = request.body.asText
            if (!rawBody.isDefined)
                BadRequest
            val reqObj = jsoner.fromJson(rawBody.get, classOf[GetModsRequest])
            val mods = curseForgeClient.getMods(reqObj)
            Ok(jsoner.toJson(mods))
        }
    }
}

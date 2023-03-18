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
import com.google.gson.GsonBuilder
import components.services.serializer.JsonService

class CurseForgeProxyController @Inject()(
    val controllerComponents: ControllerComponents,
    val curseForgeClient: CurseForgeClient,
    val jsonizer: JsonService
) extends BaseController{

    def searchMods() = Action {
        implicit request: Request[AnyContent] => {
            if (!request.hasBody)
                BadRequest
            val rawBody = request.body.asJson
            if (!rawBody.isDefined)
                BadRequest
            val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[GetModsRequest])
            val mods = curseForgeClient.mods.getMods(reqObj)
            Ok(jsonizer.serialize(mods))
        }
    }
}

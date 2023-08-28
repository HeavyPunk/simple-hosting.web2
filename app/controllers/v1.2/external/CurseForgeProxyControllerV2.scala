package controllers.v2.external

import com.google.inject.Inject
import play.api.mvc.{
    BaseController,
    ControllerComponents,
    AnyContent,
    Request,
}
import components.clients.curseforge.CurseForgeClient
import components.services.serializer.JsonService
import components.clients.curseforge.models.SearchModsRequest
import scala.concurrent.Future
import components.clients.curseforge.models.GetMinecraftVersionsRequest
import components.clients.curseforge.models.GetMinecraftModloadersRequest
import components.clients.curseforge.models.GetCategoriesRequest
import components.clients.curseforge.models.GetModFilesByModIdRequest
import components.clients.curseforge.models.GetModFileByFileIdRequest
import views.html.defaultpages.badRequest

class CurseForgeProxyControllerV2 @Inject() (
    val controllerComponents: ControllerComponents,
    val curseForgeClient: CurseForgeClient,
    val jsonizer: JsonService
) extends BaseController {
    def searchMods() = Action.async { implicit request: Request[AnyContent] => {
        val req = request.body.asJson
            .map(r => jsonizer.deserialize(r.toString, classOf[SearchModsRequest]))
            .map(r => curseForgeClient.mods.getMods(r))
            .fold(BadRequest(""))(m => Ok(jsonizer.serialize(m)))
        Future.successful(req)
    }}

    def getModById(id: Int) = Action.async { implicit request: Request[AnyContent] => {
        val mod = curseForgeClient.mods.getModById(id)
        Future.successful(Ok(jsonizer.serialize(mod)))
    }}


    def getModFullDescription(id: Int) = Action.async { implicit request: Request[AnyContent] => {
        val description = curseForgeClient.mods.getModFullDescription(id)
        Future.successful(Ok(jsonizer.serialize(description)))
    }}

    def getMinecraftVersions() = Action.async { implicit request: Request[AnyContent] => {
        val result = request.body.asJson
            .map(r => jsonizer.deserialize(r.toString, classOf[GetMinecraftVersionsRequest]))
            .map(r => curseForgeClient.softwares.getMinecraftVersions(r))
            .fold(BadRequest(""))(v => Ok(jsonizer.serialize(v)))

        Future.successful(result)
    }}

    def getMinecraftModloaders() = Action.async { implicit request: Request[AnyContent] => {
        val result = request.body.asJson
            .map(r => jsonizer.deserialize(r.toString, classOf[GetMinecraftModloadersRequest]))
            .map(r => curseForgeClient.softwares.getMinecraftModloaders(r))
            .fold(BadRequest(""))(m => Ok(jsonizer.serialize(m)))
        
        Future.successful(result)
    }}

    def getCategories() = Action.async { implicit request: Request[AnyContent] => {
        val result = request.body.asJson
            .map(r => jsonizer.deserialize(r.toString, classOf[GetCategoriesRequest]))
            .map(r => curseForgeClient.categories.getCategories(r))
            .fold(BadRequest(""))(c => Ok(jsonizer.serialize(c)))

        Future.successful(result)
    }}

    def getCategoriesGroupedByClass() = Action.async { implicit request: Request[AnyContent] => {
        val result = request.body.asJson
            .map(r => jsonizer.deserialize(r.toString, classOf[GetCategoriesRequest]))
            .map(r => curseForgeClient.categories.getCategoriesGroupedByClass())
            .fold(BadRequest(""))(c => Ok(jsonizer.serialize(c)))
        
        Future.successful(result)
    }}

    def getModFilesByModId() = Action.async { implicit request: Request[AnyContent] => {
        val result = request.body.asJson
            .map(r => jsonizer.deserialize(r.toString, classOf[GetModFilesByModIdRequest]))
            .map(r => curseForgeClient.files.getModFilesByModId(r))
            .fold(BadRequest(""))(f => Ok(jsonizer.serialize((f))))
        
            Future.successful(result)
    }}

    def getModFileByFileId() = Action.async { implicit request: Request[AnyContent] => {
        val result = request.body.asJson
            .map(r => jsonizer.deserialize(r.toString, classOf[GetModFileByFileIdRequest]))
            .map(r => curseForgeClient.files.getModFileByFileId(r))
            .fold(BadRequest(""))(f => Ok(jsonizer.serialize(f)))
    
        Future.successful(result)
    }}

    def getFileDownloadUrls() = Action.async { implicit request: Request[AnyContent] => {
        val result = request.body.asJson
            .map(r => jsonizer.deserialize(r.toString, classOf[GetModFilesByModIdRequest]))
            .map(r => curseForgeClient.files.getFileDownloadUrls(r))
            .fold(BadRequest(""))(u => Ok(jsonizer.serialize(u)))
        
        Future.successful(result)
    }}
}

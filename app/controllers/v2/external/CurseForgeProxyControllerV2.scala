package controllers.v2.external

import play.api.mvc.ControllerComponents
import com.google.inject.Inject
import components.services.log.Log
import controllers.v2.SimpleHostingController
import components.services.serializer.JsonService
import components.clients.curseforge.CurseForgeClient
import play.api.mvc.Request
import play.api.mvc.AnyContent
import components.clients.curseforge.models.SearchModsRequest
import controllers.v2.RequestBodyNotFound
import controllers.v2.JsonNotFoundForRequestBody
import controllers.v2.JsonCannotBeParsed
import components.basic.serializeForLog
import components.clients.curseforge.ApiPaths.description
import components.clients.curseforge.models.GetMinecraftVersionsRequest
import components.clients.curseforge.models.GetMinecraftModloadersRequest
import components.clients.curseforge.models.GetCategoriesRequest
import components.clients.curseforge.models.GetModFilesByModIdRequest
import components.clients.curseforge.models.GetModFileByFileIdRequest

class CurseForgeProxyControllerV2 @Inject() (
    val controllerComponents: ControllerComponents,
    val curseForgeClient: CurseForgeClient,
    val jsonizer: JsonService,
    val log: Log
) extends SimpleHostingController(jsonizer):
    def searchMods() = Action.async { implicit request: Request[AnyContent] => {
        val result = getModelFromJsonRequest[SearchModsRequest](request)
            .flatMap(model => curseForgeClient.mods.getMods(model))
        
        val (err, mods) = result.tryGetValue

        if (err != null) {
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest("Request body not found"))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest("Body must be a JSON"))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest("Request body cannot be parsed"))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        } else wrapToFuture(Ok(jsonizer.serialize(mods)))
    }}

    def getModById(id: Int) = Action.async { implicit request: Request[AnyContent] => {
        val result = curseForgeClient.mods.getModById(id)
        val (err, mod) = result.tryGetValue
        if (err != null)
            err match
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(mod)))
    }}

    def getModFullDescription(id: Int) = Action.async { implicit request: Request[AnyContent] => {
        val result = curseForgeClient.mods.getModFullDescription(id)
        val (err, description) = result.tryGetValue
        if (err != null)
            err match
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(description)))
    }}

    def getMinecraftVersions() = Action.async { implicit request: Request[AnyContent] => {
        val result = getModelFromJsonRequest[GetMinecraftVersionsRequest](request)
            .flatMap(req => curseForgeClient.softwares.getMinecraftVersions(req))
        val (err, versions) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest("Request body not found"))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest("Body must be a JSON"))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest("Request body cannot be parsed"))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(versions)))
    }}

    def getMinecraftModloaders() = Action.async { implicit request: Request[AnyContent] => {
        val result = getModelFromJsonRequest[GetMinecraftModloadersRequest](request)
            .flatMap(req => curseForgeClient.softwares.getMinecraftModloaders(req))
        val (err, modloaders) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest("Request body not found"))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest("Body must be a JSON"))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest("Request body cannot be parsed"))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(modloaders)))
    }}

    def getCategories() = Action.async { implicit request: Request[AnyContent] => {
        val result = getModelFromJsonRequest[GetCategoriesRequest](request)
            .flatMap(req => curseForgeClient.categories.getCategories(req))
        val (err, categories) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest("Request body not found"))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest("Body must be a JSON"))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest("Request body cannot be parsed"))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(categories)))
    }}

    def getCategoriesGroupedByClass() = Action.async { implicit request: Request[AnyContent] => {
        val result = curseForgeClient.categories.getCategoriesGroupedByClass()
        val (err, categories) = result.tryGetValue
        if (err != null)
            err match
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(categories)))
    }}

    def getModFilesByModId() = Action.async { implicit request: Request[AnyContent] => {
        val result = getModelFromJsonRequest[GetModFilesByModIdRequest](request)
            .flatMap(req => curseForgeClient.files.getModFilesByModId(req))
        val (err, files) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest("Request body not found"))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest("Body must be a JSON"))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest("Request body cannot be parsed"))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(files)))
    }} 

    def getModFileByFileId() = Action.async { implicit request: Request[AnyContent] => {
        val result = getModelFromJsonRequest[GetModFileByFileIdRequest](request)
            .flatMap(req => curseForgeClient.files.getModFileByFileId(req))
        val (err, file) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest("Request body not found"))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest("Body must be a JSON"))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest("Request body cannot be parsed"))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(file)))
    }}

    def getFileDownloadUrls() = Action.async { implicit request: Request[AnyContent] => {
        val result = getModelFromJsonRequest[GetModFilesByModIdRequest](request)
            .flatMap(req => curseForgeClient.files.getFileDownloadUrls(req))
        val (err, urls) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest("Request body not found"))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest("Body must be a JSON"))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest("Request body cannot be parsed"))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else wrapToFuture(Ok(jsonizer.serialize(urls)))
    }}

package controllers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import components.clients.curseforge.CommonCurseForgeClient
import components.clients.curseforge.CurseForgeClient
import components.clients.curseforge.models.GetCategoriesRequest
import components.clients.curseforge.models.GetMinecraftModloadersRequest
import components.clients.curseforge.models.GetMinecraftVersionsRequest
import components.clients.curseforge.models.GetModFileByFileIdRequest
import components.clients.curseforge.models.GetModFilesByModIdRequest
import components.clients.curseforge.models.SearchModsRequest
import components.services.serializer.JsonService
import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import play.api.mvc._

import javax.inject.Inject

class CurseForgeProxyController @Inject() (
    val controllerComponents: ControllerComponents,
    val curseForgeClient: CurseForgeClient,
    val jsonizer: JsonService
) extends BaseController {

  def searchMods() = Action { implicit request: Request[AnyContent] =>
    {
      if (!request.hasBody)
        BadRequest
      val rawBody = request.body.asJson
      if (!rawBody.isDefined)
        BadRequest
      val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[SearchModsRequest])
      val mods   = curseForgeClient.mods.getMods(reqObj)
      Ok(jsonizer.serialize(mods))
    }
  }

  def getModById(id: Int) = Action { implicit request: Request[AnyContent] =>
    {
      val mod = curseForgeClient.mods.getModById(id)
      Ok(jsonizer.serialize(mod))
    }
  }

  def getModFullDescription(id: Int) = Action { implicit request: Request[AnyContent] =>
    {
      val description = curseForgeClient.mods.getModFullDescription(id)
      Ok(jsonizer.serialize(description))
    }
  }

  def getMinecraftVersions() = Action { implicit request: Request[AnyContent] =>
    {
      if (!request.hasBody)
        BadRequest
      val rawBody = request.body.asJson
      if (!rawBody.isDefined)
        BadRequest
      val reqObj   = jsonizer.deserialize(rawBody.get.toString, classOf[GetMinecraftVersionsRequest])
      val versions = curseForgeClient.softwares.getMinecraftVersions(reqObj)
      Ok(jsonizer.serialize(versions))
    }
  }

  def getMinecraftModloaders() = Action { implicit request: Request[AnyContent] =>
    {
      if (!request.hasBody)
        BadRequest
      val rawBody = request.body.asJson
      if (!rawBody.isDefined)
        BadRequest
      val reqObj     = jsonizer.deserialize(rawBody.get.toString, classOf[GetMinecraftModloadersRequest])
      val modloaders = curseForgeClient.softwares.getMinecraftModloaders(reqObj)
      Ok(jsonizer.serialize(modloaders))
    }
  }

  def getCategories() = Action { implicit request: Request[AnyContent] =>
    {
      if (!request.hasBody)
        BadRequest
      val rawBody = request.body.asJson
      if (!rawBody.isDefined)
        BadRequest
      val reqObj     = jsonizer.deserialize(rawBody.get.toString, classOf[GetCategoriesRequest])
      val categories = curseForgeClient.categories.getCategories(reqObj)
      Ok(jsonizer.serialize(categories))
    }
  }

  def getCategoriesGroupedByClass() = Action { implicit request: Request[AnyContent] =>
    {
      val categories = curseForgeClient.categories.getCategoriesGroupedByClass()
      Ok(jsonizer.serialize(categories))
    }
  }

  def getModFilesByModId() = Action { implicit request: Request[AnyContent] =>
    {
      if (!request.hasBody)
        BadRequest
      val rawBody = request.body.asJson
      if (!rawBody.isDefined)
        BadRequest
      val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[GetModFilesByModIdRequest])

      val files = curseForgeClient.files.getModFilesByModId(reqObj)
      Ok(jsonizer.serialize(files))
    }
  }

  def getModFileByFileId() = Action { implicit request: Request[AnyContent] =>
    {
      if (!request.hasBody)
        BadRequest
      val rawBody = request.body.asJson
      if (!rawBody.isDefined)
        BadRequest
      val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[GetModFileByFileIdRequest])

      val file = curseForgeClient.files.getModFileByFileId(reqObj)
      Ok(jsonizer.serialize(file))
    }
  }

  def getFileDownloadUrls() = Action { implicit request: Request[AnyContent] =>
    {
      if (!request.hasBody)
        BadRequest
      val rawBody = request.body.asJson
      if (!rawBody.isDefined)
        BadRequest
      val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[GetModFilesByModIdRequest])

      val urls = curseForgeClient.files.getFileDownloadUrls(reqObj)
      Ok(jsonizer.serialize(urls))
    }
  }
}

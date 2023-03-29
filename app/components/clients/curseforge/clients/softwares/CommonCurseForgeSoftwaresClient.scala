package components.clients.curseforge.clients.softwares

import components.clients.curseforge.ApiPaths
import components.clients.curseforge.CurseForgeClientSettings
import components.clients.curseforge.models.GetMinecraftModloadersRequest
import components.clients.curseforge.models.GetMinecraftModloadersResponse
import components.clients.curseforge.models.GetMinecraftVersionsRequest
import components.clients.curseforge.models.GetMinecraftVersionsResponse
import components.clients.curseforge.models.ModloadersResponse
import components.clients.curseforge.models.business.Modloader
import components.clients.curseforge.models.business.ModloaderVersion
import components.services.serializer.JsonService
import org.apache.hc.core5.net.URIBuilder

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import javax.inject.Inject
import scala.collection.mutable

class CommonCurseForgeSoftwaresClient @Inject() (
    val settings: CurseForgeClientSettings,
    val jsonizer: JsonService
) extends CurseForgeSoftwaresClient {
  def contructBaseRequest = HttpRequest
    .newBuilder()
    .header("x-api-key", settings.apiKey)
    .header("Content-Type", "application/json")
    .header("Accept", "application/json")

  def constructBaseUri() = new URIBuilder()
    .setScheme("https")
    .setHost(settings.host)
    .setPathSegments("v1")

  override def getMinecraftVersions(request: GetMinecraftVersionsRequest): GetMinecraftVersionsResponse = {
    val query = request.toQueryString()
    val uri = constructBaseUri()
      .setCustomQuery(query)
      .appendPathSegments(ApiPaths.minecraft, ApiPaths.versions)
      .build()
    val req      = contructBaseRequest.GET().uri(uri).build()
    val client   = HttpClient.newHttpClient()
    val response = client.send(req, BodyHandlers.ofString())

    val res = jsonizer.deserialize(response.body(), classOf[GetMinecraftVersionsResponse])
    res
  }

  override def getMinecraftModloaders(request: GetMinecraftModloadersRequest): GetMinecraftModloadersResponse = {
    val query = request.toQueryString()
    val uri = constructBaseUri()
      .setCustomQuery(query)
      .appendPathSegments(ApiPaths.minecraft, ApiPaths.modloaders)
      .build()
    val req      = contructBaseRequest.GET().uri(uri).build()
    val client   = HttpClient.newHttpClient()
    val response = client.send(req, BodyHandlers.ofString())

    val modloaders    = jsonizer.deserialize(response.body(), classOf[ModloadersResponse])
    val modloadersMap = collection.mutable.Map[String, ModloaderVersion]()

    for (elem <- modloaders.data) {
      val elemNewName = elem.name.split("-")(1)

      if (elem.latest == true || elem.recommended == true) {
        val mapElem = modloadersMap.getOrElse(elem.gameVersion, null)
        if (mapElem == null) {
          modloadersMap(elem.gameVersion) = new ModloaderVersion(
            elem.gameVersion,
            Array(new Modloader(elem.gameVersion, elem.name, elemNewName, elem.recommended, elem.latest))
          )
        } else if (mapElem.versions.length == 1) {
          val modloaderVersions = mapElem.versions
          modloadersMap(elem.gameVersion) = new ModloaderVersion(
            elem.gameVersion,
            modloaderVersions.appended(
              new Modloader(elem.gameVersion, elem.name, elemNewName, elem.recommended, elem.latest)
            )
          )
        }
      }
    }

    val formattedModloaders                 = modloadersMap.values.toArray.sortBy(_.gameVersion).reverse
    val res: GetMinecraftModloadersResponse = new GetMinecraftModloadersResponse(formattedModloaders)
    res
  }
}

package components.clients.curseforge.clients.mods

import components.clients.curseforge.ApiPaths
import components.clients.curseforge.CurseForgeClientSettings
import components.clients.curseforge.models.GetModFullDescriptionResponse
import components.clients.curseforge.models.GetModResponse
import components.clients.curseforge.models.SearchModsRequest
import components.clients.curseforge.models.SearchModsResponse
import components.services.serializer.JsonService
import org.apache.hc.core5.net.URIBuilder

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import javax.inject.Inject
import components.basic.{ Monad, ErrorMonad, ResultMonad }

class CommonCurseForgeModsClient @Inject() (
    val settings: CurseForgeClientSettings,
    val jsonizer: JsonService
) extends CurseForgeModsClient {
  def contructBaseRequest = HttpRequest
    .newBuilder()
    .header("x-api-key", settings.apiKey)
    .header("Content-Type", "application/json")
    .header("Accept", "application/json")

  def constructBaseUri() = new URIBuilder()
    .setScheme("https")
    .setHost(settings.host)
    .setPathSegments("v1")

  override def getMods(request: SearchModsRequest): Monad[Exception, SearchModsResponse] = {
    try {
      val query = request.toQueryString()
      val uri = constructBaseUri()
        .setCustomQuery(query)
        .appendPathSegments(ApiPaths.mods, ApiPaths.search)
        .build()

      val req      = contructBaseRequest.GET().uri(uri).build()
      val client   = HttpClient.newHttpClient()
      val response = client.send(req, BodyHandlers.ofString())

      val res = jsonizer.deserialize(response.body(), classOf[SearchModsResponse])
      ResultMonad(res)
    } catch {
      case e: Exception => ErrorMonad(e)
    }
  }

  override def getModById(id: Int): Monad[Exception, GetModResponse] = {
    try {
      val uri = constructBaseUri()
        .appendPathSegments(ApiPaths.mods, id.toString())
        .build()
      val req      = contructBaseRequest.GET().uri(uri).build()
      val client   = HttpClient.newHttpClient()
      val response = client.send(req, BodyHandlers.ofString())

      val res = jsonizer.deserialize(response.body(), classOf[GetModResponse]);
      ResultMonad(res)
    } catch {
      case e: Exception => ErrorMonad(e)
    }
  }

  override def getModFullDescription(id: Int): Monad[Exception, GetModFullDescriptionResponse] = {
    try {
      val uri = constructBaseUri()
        .appendPathSegments(ApiPaths.mods, id.toString(), ApiPaths.description)
        .build()
      val req      = contructBaseRequest.GET().uri(uri).build()
      val client   = HttpClient.newHttpClient()
      val response = client.send(req, BodyHandlers.ofString())

      val res = jsonizer.deserialize(response.body(), classOf[GetModFullDescriptionResponse])
      ResultMonad(res)
    } catch {
      case e: Exception => ErrorMonad(e)
    }
  }
}

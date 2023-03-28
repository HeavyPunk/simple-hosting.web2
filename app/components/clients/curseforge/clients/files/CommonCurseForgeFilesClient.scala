package components.clients.curseforge.clients.files

import components.clients.curseforge.ApiPaths
import components.clients.curseforge.CurseForgeClientSettings
import components.clients.curseforge.models.GetFileDownloadUrlsResponse
import components.clients.curseforge.models.GetModFileByFileIdRequest
import components.clients.curseforge.models.GetModFileByFileIdResponse
import components.clients.curseforge.models.GetModFilesByModIdRequest
import components.clients.curseforge.models.GetModFilesByModIdResponse
import components.services.serializer.JsonService
import org.apache.hc.core5.net.URIBuilder

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import javax.inject.Inject
import scala.collection.mutable.ListBuffer

class CommonCurseForgeFilesClient @Inject() (
    val settings: CurseForgeClientSettings,
    val jsonizer: JsonService
) extends CurseForgeFilesClient {
  def contructBaseRequest = HttpRequest
    .newBuilder()
    .header("x-api-key", settings.apiKey)
    .header("Content-Type", "application/json")
    .header("Accept", "application/json")

  def constructBaseUri() = new URIBuilder()
    .setScheme("https")
    .setHost(settings.host)
    .setPathSegments("v1")

  override def getModFilesByModId(request: GetModFilesByModIdRequest): GetModFilesByModIdResponse = {
    val modId = request.modId
    val uri = constructBaseUri()
      .setCustomQuery(request.toQueryString())
      .appendPathSegments(ApiPaths.mods, modId.toString, ApiPaths.files)
      .build()
    val req      = contructBaseRequest.GET().uri(uri).build()
    val client   = HttpClient.newHttpClient()
    val response = client.send(req, BodyHandlers.ofString())

    val res = jsonizer.deserialize(response.body(), classOf[GetModFilesByModIdResponse])
    res
  }

  override def getModFileByFileId(request: GetModFileByFileIdRequest): GetModFileByFileIdResponse = {
    val modId  = request.modId
    val fileId = request.fileId
    val uri = constructBaseUri()
      .appendPathSegments(ApiPaths.mods, modId.toString, ApiPaths.files, fileId.toString)
      .build()
    val req      = contructBaseRequest.GET().uri(uri).build()
    val client   = HttpClient.newHttpClient()
    val response = client.send(req, BodyHandlers.ofString())

    val res = jsonizer.deserialize(response.body(), classOf[GetModFileByFileIdResponse])
    res
  }

  override def getFileDownloadUrls(request: GetModFilesByModIdRequest): GetFileDownloadUrlsResponse = {
    val modId = request.modId
    val files = this.getModFilesByModId(request)

    if (files.data.length == 0)
      return new GetFileDownloadUrlsResponse(new Array(0))

    val urls       = ListBuffer[String]()
    val latestFile = files.data(0)

    if (latestFile.serverPackFileId.isDefined) {
      val serverPackFileId = latestFile.serverPackFileId.get
      val serverFile       = this.getModFileByFileId(new GetModFileByFileIdRequest(modId, serverPackFileId)).data
      urls += serverFile.downloadUrl
    } else {
      urls += latestFile.downloadUrl
    }

    val res = new GetFileDownloadUrlsResponse(urls.toArray)
    res
  }
}

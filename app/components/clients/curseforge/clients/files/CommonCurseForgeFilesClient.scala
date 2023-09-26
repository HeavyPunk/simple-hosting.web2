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
import components.basic.{ Monad, ErrorMonad, ResultMonad }
import components.clients.curseforge.models.File

class ServerPackFileIdNotFound extends Exception

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

  override def getModFilesByModId(request: GetModFilesByModIdRequest): Monad[Exception, GetModFilesByModIdResponse] = {
    try {
      val modId = request.modId
      val uri = constructBaseUri()
        .setCustomQuery(request.toQueryString())
        .appendPathSegments(ApiPaths.mods, modId.toString, ApiPaths.files)
        .build()
      val req      = contructBaseRequest.GET().uri(uri).build()
      val client   = HttpClient.newHttpClient()
      val response = client.send(req, BodyHandlers.ofString())

      val res = jsonizer.deserialize(response.body(), classOf[GetModFilesByModIdResponse])
      ResultMonad(res)
    } catch {
      case e: Exception => ErrorMonad(e)
    }
  }

  override def getModFileByFileId(request: GetModFileByFileIdRequest): Monad[Exception, GetModFileByFileIdResponse] = {
    try {
      val modId  = request.modId
      val fileId = request.fileId
      val uri = constructBaseUri()
        .appendPathSegments(ApiPaths.mods, modId.toString, ApiPaths.files, fileId.toString)
        .build()
      val req      = contructBaseRequest.GET().uri(uri).build()
      val client   = HttpClient.newHttpClient()
      val response = client.send(req, BodyHandlers.ofString())

      val res = jsonizer.deserialize(response.body(), classOf[GetModFileByFileIdResponse])
      ResultMonad(res)
    } catch {
      case e: Exception => ErrorMonad(e)
    }
  }

  override def getFileDownloadUrls(request: GetModFilesByModIdRequest): Monad[Exception, GetFileDownloadUrlsResponse] = {
    val modId = request.modId
    val files = this.getModFilesByModId(request)

    val latestFile = files.flatMap(f => ResultMonad(f.data(0)))
    val result = latestFile.flatMap(f => 
      if (f.serverPackFileId.isDefined) 
        getModFileByFileId(new GetModFileByFileIdRequest(modId, f.serverPackFileId.get))
      else 
        ErrorMonad(ServerPackFileIdNotFound())
    )
    val (err, urls) = result.tryGetValue

    if (err == null)
      return ResultMonad(GetFileDownloadUrlsResponse(Array(urls.data.downloadUrl)))
    else
      err match
        case _: ServerPackFileIdNotFound => ResultMonad(GetFileDownloadUrlsResponse(Array(latestFile.tryGetValue._2.downloadUrl)))
        case e: Exception => ErrorMonad(e)
  }
}

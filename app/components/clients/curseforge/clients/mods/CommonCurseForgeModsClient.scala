package components.clients.curseforge.clients.mods

import com.google.gson.Gson
import components.clients.curseforge.ApiPaths
import components.clients.curseforge.CurseForgeClientSettings
import components.clients.curseforge.models.GetModFullDescriptionResponse
import components.clients.curseforge.models.GetModResponse
import components.clients.curseforge.models.SearchModsRequest
import components.clients.curseforge.models.SearchModsResponse
import org.apache.hc.core5.net.URIBuilder

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import components.services.serializer.JsonService
import javax.inject.Inject

class CommonCurseForgeModsClient @Inject()(
    val settings: CurseForgeClientSettings,
    val jsonizer: JsonService
) extends CurseForgeModsClient {
    var baseRequest = HttpRequest.newBuilder()
        .header("x-api-key", settings.apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")

    def constructBaseUri() = new URIBuilder()
        .setScheme("https")
        .setHost(settings.host)
        .setPathSegments("v1")

    override def getMods(request: SearchModsRequest): SearchModsResponse = { 
        val query = request.toQueryString()
        val uri = constructBaseUri()
            .setCustomQuery(query)
            .appendPathSegments(ApiPaths.mods, ApiPaths.search)
            .build()
        val req = baseRequest.GET().uri(uri).build()
        val client = HttpClient.newHttpClient()
        val response = client.send(req, BodyHandlers.ofString())

        val res = jsonizer.deserialize(response.body(), classOf[SearchModsResponse])
        res
    }

    override def getModById(id: Int): GetModResponse = {
        val uri = constructBaseUri()
                .appendPathSegments(ApiPaths.mods, id.toString())
                .build()
        val req = baseRequest.GET().uri(uri).build()
        val client = HttpClient.newHttpClient()
        val response = client.send(req, BodyHandlers.ofString())
        
        val res = jsonizer.deserialize(response.body(), classOf[GetModResponse]);
        res
    }

    override def getModFullDescription(id: Int): GetModFullDescriptionResponse = {
        val uri = constructBaseUri()
                .appendPathSegments(ApiPaths.mods, id.toString(), ApiPaths.description)
                .build()
        val req = baseRequest.GET().uri(uri).build()
        val client = HttpClient.newHttpClient()
        val response = client.send(req, BodyHandlers.ofString())
        
        val res = jsonizer.deserialize(response.body(), classOf[GetModFullDescriptionResponse])
        res
    }
}

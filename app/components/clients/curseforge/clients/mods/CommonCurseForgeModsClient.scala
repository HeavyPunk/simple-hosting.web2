package components.clients.curseforge.clients.mods

import com.google.gson.Gson
import components.clients.curseforge.ApiPaths
import components.clients.curseforge.CurseForgeClientSettings
import components.clients.curseforge.models.GetModsRequest
import components.clients.curseforge.models.GetModsResponse
import components.clients.curseforge.models.GetModResponse
import org.apache.hc.core5.net.URIBuilder

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

class CommonCurseForgeModsClient(val settings: CurseForgeClientSettings) extends CurseForgeModsClient{
    var baseRequest = HttpRequest.newBuilder()
        .header("x-api-key", settings.apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
    
    val jsoner = new Gson(); //TODO: to abstract json manager

    def constructBaseUri() = new URIBuilder()
        .setScheme("https")
        .setHost(settings.host)
        .setPathSegments("v1")

    override def getMods(request: GetModsRequest): GetModsResponse = { 
        val query = request.toQueryString()
        val uri = constructBaseUri()
            .setCustomQuery(query)
            .appendPathSegments(ApiPaths.mods, ApiPaths.search)
            .build()
        val req = baseRequest.GET().uri(uri).build()
        val client = HttpClient.newHttpClient()
        val response = client.send(req, BodyHandlers.ofString())

        val res = jsoner.fromJson(response.body(), classOf[GetModsResponse])
        res
    }

    override def getModById(id: Int): GetModResponse = {
        val uri = constructBaseUri()
                .appendPathSegments(ApiPaths.mods, id.toString())
                .build()
        val req = baseRequest.GET().uri(uri).build()
        val client = HttpClient.newHttpClient()
        val response = client.send(req, BodyHandlers.ofString())
        
        val res = jsoner.fromJson(response.body(), classOf[GetModResponse]);
        res
    }
}

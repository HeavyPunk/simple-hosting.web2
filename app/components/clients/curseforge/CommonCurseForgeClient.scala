package components.clients.curseforge

import components.clients.curseforge.models.{GetModsRequest, GetModsResponse}
import org.apache.hc.core5.net.URIBuilder
import java.net.http.HttpRequest
import java.net.http.HttpClient
import java.net.http.HttpResponse.BodyHandlers
import com.google.gson.Gson

class CommonCurseForgeClient(settings: ClientSettings) extends CurseForgeClient {
    var uriBuilder = new URIBuilder()
        .setScheme("https")
        .setHost(settings.host)
        .setPathSegments("v1")
    var baseRequest = HttpRequest.newBuilder()
        .header("x-api-key", settings.apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
    
    val jsoner = new Gson(); //TODO: to abstract json manager

    override def getMods(request: GetModsRequest): GetModsResponse = { 
        val query = request.toQueryString()
        val uri = uriBuilder
            .setCustomQuery(query)
            .appendPathSegments(ApiPaths.searchMods)
            .build()
        val r = baseRequest.GET().uri(uri).build()
        val client = HttpClient.newHttpClient()
        val response = client.send(r, BodyHandlers.ofString())

        val res = jsoner.fromJson(response.body(), classOf[GetModsResponse])
        res
    }
}

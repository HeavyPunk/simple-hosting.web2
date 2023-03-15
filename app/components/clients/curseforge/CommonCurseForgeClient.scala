package components.clients.curseforge

import components.clients.curseforge.models.{GetModsRequest, GetModsResponse}
import org.apache.hc.core5.net.URIBuilder
import java.net.http.HttpRequest
import java.net.http.HttpClient
import java.net.http.HttpResponse.BodyHandlers
import com.google.gson.Gson
import components.clients.curseforge.clients.mods.CurseForgeModsClient

class CommonCurseForgeClient(modsClient: CurseForgeModsClient) extends CurseForgeClient {
    val mods: CurseForgeModsClient = modsClient
}

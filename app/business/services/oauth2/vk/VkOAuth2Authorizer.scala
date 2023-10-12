package business.services.oauth2.vk

import business.services.oauth2.OAuth2Authorizer
import java.net.URI
import org.apache.hc.core5.net.URIBuilder
import components.basic.Monad
import business.services.oauth2.Aborted
import java.net.http.{
    HttpClient,
    HttpRequest
}
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import components.basic.ResultMonad
import components.basic.ErrorMonad
import business.services.oauth2.{
    AccessResult,
    AccessError
}
import components.services.serializer.JsonService
import business.entities.OAuthUser
import com.fasterxml.jackson.annotation.JsonProperty
import business.entities.User

enum OAuthDisplayType:
    case Page, Popup, Mobile

enum AccessRight(val scope: Int):
    case Notify extends AccessRight(1 << 0)
    case Friends extends AccessRight(1 << 1)
    case Photos extends AccessRight(1 << 2)
    case Audio extends AccessRight(1 << 3)
    case Video extends AccessRight(1 << 4)
    case Stories extends AccessRight(1 << 6)
    case Pages extends AccessRight(1 << 7)
    case Menu extends AccessRight(1 << 8)
    case Status extends AccessRight(1 << 10)
    case Notes extends AccessRight(1 << 11)
    case Messages extends AccessRight(1 << 12)
    case Wall extends AccessRight(1 << 13)
    case Ads extends AccessRight(1 << 15)
    case Offline extends AccessRight(1 << 16)
    case Docs extends AccessRight(1 << 17)
    case Groups extends AccessRight(1 << 18)
    case Notifications extends AccessRight(1 << 19)
    case Stats extends AccessRight(1 << 20)
    case Email extends AccessRight(1 << 22)
    case Market extends AccessRight(1 << 27)
    case PhoneNumber extends AccessRight(1 << 28)

    def |(other: AccessRight): Int = other.scope | scope


enum ResponseType:
    case Code

class VkAccessResult(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("expires_in") val expiresIn: Long,
    @JsonProperty("user_id") val userId: Long,
    @JsonProperty("error") val error: String,
    @JsonProperty("error_description") val errorDescription: String
)

class VkOAuth2Authorizer(
    val baseUri: URIBuilder, 
    val clientId: String, 
    val clientSecret: String,
    val redirectUri: String,
    val scopes: Int,
    val displayType: OAuthDisplayType,
    val responseType: ResponseType,
    val jsonizer: JsonService
) extends OAuth2Authorizer {

    override def getAuthorizeUrl(user: OAuthUser): Monad[Exception, URI] = {
        val httpClient = HttpClient.newHttpClient()
        val displayTypeStr = displayType.toString.toLowerCase
        val responseTypeStr = responseType.toString.toLowerCase
        val authorizeUri = baseUri
            .appendPath("authorize")
            .addParameter("client_id", clientId)
            .addParameter("redirect_uri", redirectUri)
            .addParameter("display", displayTypeStr)
            .addParameter("scope", scopes.toString)
            .addParameter("response_type", responseTypeStr)
            .addParameter("state", user.oauthKey)
            .build()
        return ResultMonad(authorizeUri)
    }

    def getAccessToken(user: OAuthUser, accessCode: String): Monad[Aborted | AccessError | Exception, AccessResult] = {
        val httpClient = HttpClient.newHttpClient()
        val accessTokenUri = baseUri
            .appendPath("access_token")
            .addParameter("client_id", clientId)
            .addParameter("client_secret", clientSecret)
            .addParameter("redirect_uri", redirectUri)
            .addParameter("code", accessCode)
            .build()
        val accessRequest = HttpRequest.newBuilder()
            .POST(BodyPublishers.noBody())
            .uri(accessTokenUri)
            .build()
        try {
            val accessResponse = httpClient.send(accessRequest, HttpResponse.BodyHandlers.ofString())
            val result = jsonizer.deserialize(accessResponse.body(), classOf[VkAccessResult])
            if (result.error != null || result.error != "") {
                ErrorMonad(AccessError(result.error, result.errorDescription))
            } else {
                ResultMonad(AccessResult(
                    result.accessToken,
                    result.expiresIn,
                    result.userId,
                ))
            }
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
}

package business.services.oauth2

import java.net.URI
import components.basic.Monad
import com.fasterxml.jackson.annotation.JsonProperty
import business.entities.OAuthUser
import business.entities.User

class AccessResult(
    val accessToken: String,
    val expiresIn: Long,
    val userId: Long,
)

class AccessError(val error: String, val errorDescription: String)
class Aborted
class AccessDenied

enum OAuth2System(system: String):
    case None extends OAuth2System("none")
    case VK extends OAuth2System("vk")

trait OAuth2Authorizer {
    def getAuthorizeUrl(user: OAuthUser): Monad[Exception, URI]
    def getAccessToken(user: OAuthUser, accessCode: String): Monad[Aborted | AccessError | Exception, AccessResult]
}

trait OAuth2Manager {
    def constructUser(oauthUser: OAuthUser): Monad[Exception | AccessDenied, User]
}

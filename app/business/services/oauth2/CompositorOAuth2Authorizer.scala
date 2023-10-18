package business.services.oauth2

import components.basic.{
    Monad,
    mapToMonad
}
import business.entities.OAuthUser
import java.net.URI
import business.entities.User


class CompositorOAuth2Authorizer(val authorizers: Map[OAuth2System, OAuth2Authorizer]) extends OAuth2Authorizer {
    override def getAuthorizeUrl(user: OAuthUser): Monad[Exception, URI] = {
        val authorizer = authorizers.get(user.oauthSystem)
        authorizer
            .mapToMonad(Exception(s"Not found oauth provider for ${user.oauthSystem}"))
            .flatMap(authorizer => authorizer.getAuthorizeUrl(user))
    }

    override def getAccessToken(user: OAuthUser, accessCode: String): Monad[Aborted | AccessError | Exception, AccessResult] = {
        val authorizer = authorizers.get(user.oauthSystem)
        authorizer
            .mapToMonad(Exception(s"Not found oauth provider for ${user.oauthSystem}"))
            .flatMap(authorizer => authorizer.getAccessToken(user, accessCode))
    }

}

class CompositorOAuth2Manager(val managers: Map[OAuth2System, OAuth2Manager]) extends OAuth2Manager {
    override def constructUser(oauthUser: OAuthUser): Monad[Exception | AccessDenied, User] = 
        val manager = managers.get(oauthUser.oauthSystem)
        manager
            .mapToMonad(Exception(s"Not found oauth manager for ${oauthUser.oauthSystem}"))
            .flatMap(manager => manager.constructUser(oauthUser))
}

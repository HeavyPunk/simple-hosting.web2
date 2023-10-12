package business.services.oauth2

import components.basic.{
    Monad,
    mapToMonad
}
import business.entities.OAuthUser
import java.net.URI


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

package business.services.oauth2.vk

import business.services.oauth2.OAuth2Manager
import business.services.oauth2.AccessDenied
import components.basic.Monad
import business.entities.OAuthUser
import business.entities.User
import com.google.inject.Inject

class VkOAuth2Manager @Inject() (
) extends OAuth2Manager:
    override def constructUser(oauthUser: OAuthUser): Monad[Exception | AccessDenied, User] = {
        ???
    }


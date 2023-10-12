package di.modules

import com.google.inject.AbstractModule
import play.api.{
    Environment,
    Configuration
}
import components.services.business.servers.{
    CommonServersManagementService,
    ServersManagementService
}
import business.services.oauth2.vk.VkOAuth2Authorizer
import business.services.oauth2.OAuth2Authorizer
import business.services.oauth2.CompositorOAuth2Authorizer
import org.apache.hc.core5.net.URIBuilder
import business.services.oauth2.vk.OAuthDisplayType
import com.google.inject.Guice
import components.services.serializer.JsonService
import business.services.oauth2.vk.ResponseType
import business.services.oauth2.vk.AccessRight
import business.services.oauth2.OAuth2System

class ServicesModule(
    environment: Environment,
    configuration: Configuration,
) extends AbstractModule {
    override def configure() = {
        bind(classOf[ServersManagementService]).to(classOf[CommonServersManagementService])

        val injector = Guice.createInjector(InfraModule())
        val vkAuthorizer = VkOAuth2Authorizer(
            URIBuilder().setPath(configuration.get[String]("app.services.oauth.vk.baseUri")),
            configuration.get[String]("app.services.oauth.vk.clientId"),
            configuration.get[String]("app.services.oauth.vk.clientSecret"),
            configuration.get[String]("app.services.oauth.vk.redirectUri"),
            AccessRight.Email | AccessRight.PhoneNumber,
            OAuthDisplayType.Page,
            ResponseType.Code,
            injector.getInstance(classOf[JsonService])
        )
        val oauth2Authorizer = CompositorOAuth2Authorizer(Map(
            OAuth2System.VK -> vkAuthorizer
        ))
        bind(classOf[OAuth2Authorizer]).toInstance(oauth2Authorizer)
    }
}

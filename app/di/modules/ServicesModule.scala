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

class ServicesModule(
    environment: Environment,
    configuration: Configuration,
) extends AbstractModule {
    override def configure() = {
        bind(classOf[ServersManagementService]).to(classOf[CommonServersManagementService])
    }
}

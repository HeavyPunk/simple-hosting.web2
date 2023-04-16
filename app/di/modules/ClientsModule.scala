package di.modules

import com.google.inject.AbstractModule
import components.clients.curseforge.CommonCurseForgeClient
import components.clients.curseforge.CurseForgeClient
import components.clients.curseforge.CurseForgeClientSettings
import components.clients.curseforge.clients.categories.CommonCurseForgeCategoriesClient
import components.clients.curseforge.clients.categories.CurseForgeCategoriesClient
import components.clients.curseforge.clients.mods.CommonCurseForgeModsClient
import components.clients.curseforge.clients.mods.CurseForgeModsClient
import components.clients.curseforge.clients.softwares.CommonCurseForgeSoftwaresClient
import components.clients.curseforge.clients.softwares.CurseForgeSoftwaresClient
import components.clients.curseforge.clients.files.CommonCurseForgeFilesClient
import components.clients.curseforge.clients.files.CurseForgeFilesClient
import io.github.heavypunk.compositor.client.{CompositorClient, CommonCompositorClient}
import io.github.heavypunk.compositor.client.settings.ClientSettings
import java.net.URI
import io.github.heavypunk.controller.client.ControllerClient
import io.github.heavypunk.controller.client.Settings
import io.github.heavypunk.controller.client.server.CommonControllerServerClient
import io.github.heavypunk.controller.client.server.ControllerServerClient
import io.github.heavypunk.controller.client.CommonControllerClient
import components.clients.controller.ControllerClientFactory

class ClientsModule extends AbstractModule {
  override def configure() = {
    val curseForgeClientSettings = CurseForgeClientSettings(
      "api.curseforge.com",
      "$2a$10$dxU8VD6turngMqT30zZNue.LoGtogy3o9FK4.ewYX/gJfTikizCK6"
    )
    bind(classOf[CurseForgeClientSettings]).toInstance(curseForgeClientSettings)
    bind(classOf[CurseForgeCategoriesClient]).to(classOf[CommonCurseForgeCategoriesClient])
    bind(classOf[CurseForgeSoftwaresClient]).to(classOf[CommonCurseForgeSoftwaresClient])
    bind(classOf[CurseForgeModsClient]).to(classOf[CommonCurseForgeModsClient])
    bind(classOf[CurseForgeFilesClient]).to(classOf[CommonCurseForgeFilesClient])
    bind(classOf[CurseForgeClient]).to(classOf[CommonCurseForgeClient])

    val compositorClientSettings = ClientSettings(new URI("http://localhost:8080"), "compositor")
    val compositorClient = new CommonCompositorClient(compositorClientSettings)
    bind(classOf[CompositorClient]).toInstance(compositorClient)

    //-----------------------------
    val controllerClientBaseSettings = new Settings("http", "127.0.0.1", 8989)
    bind(classOf[Settings]).toInstance(controllerClientBaseSettings)
  }
}

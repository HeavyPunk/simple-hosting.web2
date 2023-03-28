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

class ClientsModule extends AbstractModule {
  override def configure() = {
    val clientSettings = CurseForgeClientSettings(
      "api.curseforge.com",
      "$2a$10$dxU8VD6turngMqT30zZNue.LoGtogy3o9FK4.ewYX/gJfTikizCK6"
    )
    bind(classOf[CurseForgeClientSettings]).toInstance(clientSettings)
    bind(classOf[CurseForgeCategoriesClient]).to(classOf[CommonCurseForgeCategoriesClient])
    bind(classOf[CurseForgeSoftwaresClient]).to(classOf[CommonCurseForgeSoftwaresClient])
    bind(classOf[CurseForgeModsClient]).to(classOf[CommonCurseForgeModsClient])
    bind(classOf[CurseForgeFilesClient]).to(classOf[CommonCurseForgeFilesClient])
    bind(classOf[CurseForgeClient]).to(classOf[CommonCurseForgeClient])
  }
}

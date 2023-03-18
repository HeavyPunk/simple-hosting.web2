package di.modules

import com.google.inject.AbstractModule
import components.clients.curseforge.CommonCurseForgeClient
import components.clients.curseforge.CurseForgeClient
import components.clients.curseforge.CurseForgeClientSettings
import components.clients.curseforge.clients.mods.CommonCurseForgeModsClient

class ClientsModule extends AbstractModule {
    override def configure() = {
        val clientSettings = CurseForgeClientSettings(
            "api.curseforge.com", 
            "$2a$10$dxU8VD6turngMqT30zZNue.LoGtogy3o9FK4.ewYX/gJfTikizCK6"
        )
        bind(classOf[CurseForgeClientSettings]).toInstance(clientSettings)
        val curseForgeModsClient = new CommonCurseForgeModsClient(clientSettings)
        val curseForgeClient = new CommonCurseForgeClient(curseForgeModsClient)
        bind(classOf[CurseForgeClient]).toInstance(curseForgeClient)
    }
}

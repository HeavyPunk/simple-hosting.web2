package components.clients.curseforge

import components.clients.curseforge.clients.mods.CurseForgeModsClient

class CommonCurseForgeClient(modsClient: CurseForgeModsClient) extends CurseForgeClient {
    val mods: CurseForgeModsClient = modsClient
}

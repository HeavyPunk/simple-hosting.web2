package components.clients.curseforge

import components.clients.curseforge.clients.mods.CurseForgeModsClient

trait CurseForgeClient {
  val mods: CurseForgeModsClient
}

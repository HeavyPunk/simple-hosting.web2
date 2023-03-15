package components.clients.curseforge

import components.clients.curseforge.models.GetModsRequest
import components.clients.curseforge.models.GetModsResponse
import components.clients.curseforge.clients.mods.CurseForgeModsClient

trait CurseForgeClient {
  val mods: CurseForgeModsClient
}

package components.clients.curseforge.clients.mods

import components.clients.curseforge.models.GetModsRequest
import components.clients.curseforge.models.GetModsResponse

trait CurseForgeModsClient {
  def getMods(request: GetModsRequest): GetModsResponse
}

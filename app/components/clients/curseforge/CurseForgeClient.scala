package components.clients.curseforge

import components.clients.curseforge.models.GetModsRequest
import components.clients.curseforge.models.GetModsResponse

trait CurseForgeClient {
  def getMods(request: GetModsRequest): GetModsResponse
}

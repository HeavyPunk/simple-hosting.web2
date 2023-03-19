package components.clients.curseforge.clients.mods

import components.clients.curseforge.models.GetModResponse
import components.clients.curseforge.models.SearchModsResponse
import components.clients.curseforge.models.SearchModsRequest
import components.clients.curseforge.models.GetModFullDescriptionResponse

trait CurseForgeModsClient {
  def getMods(request: SearchModsRequest): SearchModsResponse
  def getModById(id: Int): GetModResponse
  def getModFullDescription(id: Int): GetModFullDescriptionResponse
}

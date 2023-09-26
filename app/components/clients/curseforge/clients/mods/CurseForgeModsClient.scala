package components.clients.curseforge.clients.mods

import components.clients.curseforge.models.GetModResponse
import components.clients.curseforge.models.SearchModsResponse
import components.clients.curseforge.models.SearchModsRequest
import components.clients.curseforge.models.GetModFullDescriptionResponse
import components.basic.Monad

trait CurseForgeModsClient {
  def getMods(request: SearchModsRequest): Monad[Exception, SearchModsResponse]
  def getModById(id: Int): Monad[Exception, GetModResponse]
  def getModFullDescription(id: Int): Monad[Exception, GetModFullDescriptionResponse]
}

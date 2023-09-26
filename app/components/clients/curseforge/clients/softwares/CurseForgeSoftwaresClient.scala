package components.clients.curseforge.clients.softwares

import components.clients.curseforge.models.GetMinecraftVersionsRequest
import components.clients.curseforge.models.GetMinecraftVersionsResponse
import components.clients.curseforge.models.GetMinecraftModloadersRequest
import components.clients.curseforge.models.GetMinecraftModloadersResponse
import components.basic.Monad

trait CurseForgeSoftwaresClient {
  def getMinecraftVersions(request: GetMinecraftVersionsRequest): Monad[Exception, GetMinecraftVersionsResponse]
  def getMinecraftModloaders(request: GetMinecraftModloadersRequest): Monad[Exception, GetMinecraftModloadersResponse]
}
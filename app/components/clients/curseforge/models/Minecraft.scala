package components.clients.curseforge.models

import components.clients.curseforge.models.business.MinecraftVersion
import components.clients.curseforge.models.business.Modloader
import components.clients.curseforge.models.business.ModloaderVersion

class GetMinecraftVersionsResponse (
  	val data: Array[MinecraftVersion]
)

class GetMinecraftVersionsRequest (
    val sortDescending: Option[Boolean]) {
    def toQueryString(): String = {
        val builder = new StringBuilder()
        if (sortDescending != null && sortDescending.isDefined) builder.addAll(s"sortDescending=${sortDescending.get}")
        builder.toString
    }
}

class GetMinecraftModloadersRequest (
    val version: Option[String],
    val includeAll: Option[Boolean]) {
    def toQueryString(): String = {
        val builder = new StringBuilder()
        if (version != null && version.isDefined) builder.addAll(s"version=${version.get}")
        if (includeAll != null && includeAll.isDefined) builder.addAll(s"&includeAll=${includeAll}")
        builder.toString()
    }
}

class ModloadersResponse (
    val data: Array[Modloader]
)

class GetMinecraftModloadersResponse (
  	val data: Array[ModloaderVersion]
)
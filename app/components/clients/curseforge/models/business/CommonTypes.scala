package components.clients.curseforge.models.business

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

class Mod (
    val id: Int,
    val name: String,
    val slug: String,
    val summary: String,
    val downloadCount: Int,
    val classId: Int,
    val links: Links,
    val categories: Array[Category],
    val authors: Array[Author],
    val logo: Picture,
    val screenshots: Array[Picture],
    val dateCreated: String,
    val dateModified: String,
)

class Picture (
    val id: Int,
    val title: String,
    val description: String,
    val url: String,
    val thumbnailUrl: String
)

class Links (
    val websiteUrl: String,
)

class ModDescription (
    val description: String
)

class Category (
    val id: String,
    val name: String,
    val slug: String,
    val classId: Int
)

class Author (
    val id: Int,
    val name: String,
    val url: String
)

class MinecraftVersion (
    @JsonProperty("version") @JsonAlias(Array("versionString"))
    val versionString: String,
    val gameVersionId: Int
)

class Modloader (
    val gameVersion: String,
    @JsonProperty("modloaderVersion") @JsonAlias(Array("name")) 
    val name: String,
    val latest: Boolean,
    val recommended: Boolean,
)

class ModloaderVersion (
    val gameVersion: String,
    val slug: String,
    val versions: Array[Modloader]
)
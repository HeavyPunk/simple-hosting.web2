package components.clients.curseforge.models.business

import com.google.gson.annotations.SerializedName

class Mod (
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("downloadCount") val downloadCount: Int,
    @SerializedName("classId") val classId: Int,
    @SerializedName("links") val links: Links,
    @SerializedName("categories") val categories: Array[Category],
    @SerializedName("authors") val authors: Array[Author],
    @SerializedName("logo") val logo: Picture,
    @SerializedName("screenshots") val screenshots: Array[Picture],
    @SerializedName("dateCreated") val dateCreated: String,
    @SerializedName("dateModified") val dateModified: String,
)

class Picture (
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("url") val url: String,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String
)

class Links (
    @SerializedName("websiteUrl") val websiteUrl: String,
)

class ModDescription (
    @SerializedName("data") val description: String
)

class Category (
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("classId") val classId: Int
)

class Author (
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("url") val url: String
)

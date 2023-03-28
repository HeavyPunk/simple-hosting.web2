package components.clients.curseforge.models

import components.clients.curseforge.models.business.Mod

class GetModResponse(
    val data: Mod
)

class GetModFullDescriptionResponse(
    val data: String
)

class SearchModsRequest(
    val gameId: Int,
    val searchFilter: Option[String],
    val sortField: Option[Int],
    val sortOrder: Option[String],
    val slug: Option[String],
    val modLoaderType: Option[Int],
    val gameVersion: Option[String],
    val gameVersionTypeId: Option[Int],
    val index: Option[Int],
    val pageSize: Option[Int],
    val categoryId: Option[Int],
    val classId: Option[Int]
) {
  def toQueryString(): String = {
    val builder = new StringBuilder(s"gameId=${gameId}")
    if (searchFilter != null && searchFilter.isDefined)
      builder.addAll(s"&searchFilter=${searchFilter.get}")
    if (sortField != null && sortField.isDefined) builder.addAll(s"&sortField=${sortField.get}")
    if (sortOrder != null && sortOrder.isDefined) builder.addAll(s"&sortOrder=${sortOrder.get}")
    if (slug != null && slug.isDefined) builder.addAll(s"&slug=${slug.get}")
    if (modLoaderType != null && modLoaderType.isDefined)
      builder.addAll(s"&modLoaderType=${modLoaderType.get}")
    if (gameVersion != null && gameVersion.isDefined)
      builder.addAll(s"&gameVersion=${gameVersion.get}")
    if (gameVersionTypeId != null && gameVersionTypeId.isDefined)
      builder.addAll(s"&gameVersion=${gameVersionTypeId.get}")
    if (index != null && index.isDefined) builder.addAll(s"&index=${index.get}")
    if (pageSize != null && pageSize.isDefined) builder.addAll(s"&pageSize=${pageSize.get}")
    if (categoryId != null && categoryId.isDefined) builder.addAll(s"&categoryId=${categoryId.get}")
    if (classId != null && classId.isDefined) builder.addAll(s"&classId=${classId.get}")
    builder.toString
  }
}

class SearchModsResponse(
    val data: Array[Mod]
)

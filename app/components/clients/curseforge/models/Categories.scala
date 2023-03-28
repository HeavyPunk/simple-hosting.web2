package components.clients.curseforge.models

import components.clients.curseforge.models.business.Category

class GetCategoriesRequest(
    val gameId: Int,
    val classId: Option[Int],
    val classesOnly: Option[Boolean]
) {
  def toQueryString(): String = {
    val builder = new StringBuilder(s"gameId=${gameId}")
    if (classId != null && classId.isDefined)
      builder.addAll(s"&classId=${classId.get.toString}")
    if (classesOnly != null && classesOnly.isDefined)
      builder.addAll(s"&classesOnly=${classesOnly.get.toString}")
    builder.toString
  }
}

class GetCategoriesResponse(
    val data: Array[Category]
)

class GroupedCategory(
    val classId: Int,
    val className: String,
    val categories: Array[Category]
)

class GetCategoriesGroupedByClassResponse(
    val data: Array[GroupedCategory]
)

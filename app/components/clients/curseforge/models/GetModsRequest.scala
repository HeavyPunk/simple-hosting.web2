package components.clients.curseforge.models

class GetModsRequest(
    val gameId: Int,
    val searchFilter: Option[String],
    val sortField: Option[Int],
    val sortOrder: Option[String],
    val slug: Option[String],
    val modloaderType: Option[Int],
    val gameVersion: Option[String],
    val gameVersionTypeId: Option[Int],
    val index: Option[Int],
    val pageSize: Option[Int],
    val categoryId: Option[Int],
    val classId: Option[Int]){
    def toQueryString(): String = {
        val builder = new StringBuilder(s"gameId=${gameId}")
        if (searchFilter != null) builder.addAll(s"&searchFilter=${searchFilter.get}")
        if (sortField != null) builder.addAll(s"&sortField=${sortField.get}")
        if (sortOrder != null) builder.addAll(s"&sortOrder=${sortOrder.get}")
        if (slug != null) builder.addAll(s"&slug=${slug.get}")
        if (modloaderType != null) builder.addAll(s"&modloaderType=${modloaderType.get}")
        if (gameVersion != null) builder.addAll(s"&gameVersion=${gameVersion.get}")
        if (gameVersionTypeId != null) builder.addAll(s"&gameVersion=${gameVersionTypeId.get}")
        if (index != null) builder.addAll(s"&index=${index.get}")
        if (pageSize != null) builder.addAll(s"&pageSize=${pageSize.get}")
        if (categoryId != null) builder.addAll(s"&categoryId=${categoryId.get}")
        if (classId != null) builder.addAll(s"&classId=${classId.get}")
        builder.toString
    }
}

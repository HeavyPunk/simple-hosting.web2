package components.clients.curseforge.models

class GetModsRequest(
    val gameId: Int,
    val searchFilter: Option[String],
    val sortField: Option[Int],
    val sortOrder: Option[Int],
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
        if (searchFilter.isDefined) builder.addAll(s"&searchFilter=${searchFilter.get}")
        if (sortField.isDefined) builder.addAll(s"&sortField=${sortField.get}")
        if (sortOrder.isDefined) builder.addAll(s"&sortOrder=${sortOrder.get}")
        if (slug.isDefined) builder.addAll(s"&slug=${slug}")
        if (modloaderType.isDefined) builder.addAll(s"&modloaderType=${modloaderType.get}")
        if (gameVersion.isDefined) builder.addAll(s"&gameVersion=${gameVersion.get}")
        if (gameVersionTypeId.isDefined) builder.addAll(s"&gameVersion=${gameVersionTypeId.get}")
        if (index.isDefined) builder.addAll(s"&index=${index.get}")
        if (pageSize.isDefined) builder.addAll(s"&pageSize=${pageSize}")
        if (categoryId.isDefined) builder.addAll(s"&categoryId=${categoryId.get}")
        if (classId.isDefined) builder.addAll(s"&classId=${classId.get}")
        builder.toString
    }
}

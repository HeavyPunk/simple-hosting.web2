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
        if (searchFilter != null) builder.addAll(s"&searchFilter=${searchFilter}")
        if (sortField != null) builder.addAll(s"&sortField=${sortField}")
        if (sortOrder != null) builder.addAll(s"&sortOrder=${sortOrder}")
        if (slug != null) builder.addAll(s"&slug=${slug}")
        if (modloaderType != null) builder.addAll(s"&modloaderType=${modloaderType}")
        if (gameVersion != null) builder.addAll(s"&gameVersion=${gameVersion}")
        if (gameVersionTypeId != null) builder.addAll(s"&gameVersion=${gameVersionTypeId}")
        if (index != null) builder.addAll(s"&index=${index}")
        if (pageSize != null) builder.addAll(s"&pageSize=${pageSize}")
        if (categoryId != null) builder.addAll(s"&categoryId=${categoryId}")
        if (classId != null) builder.addAll(s"&classId=${classId}")
        builder.toString
    }
}

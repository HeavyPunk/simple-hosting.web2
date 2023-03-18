package dtos.curseforge

class CallExternalApiRequest (
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
    val classId: Option[Int]
)

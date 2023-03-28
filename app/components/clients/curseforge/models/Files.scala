package components.clients.curseforge.models

class GetModFilesByModIdRequest(
    val modId: Int,
    val gameVersion: Option[String],
    val modLoaderType: Option[Int],
    val gameVersionTypeId: Option[Int],
    val index: Option[Int],
    val pageSize: Option[Int]
) {
  def toQueryString(): String = {
    val builder = new StringBuilder()
    if (gameVersion != null && gameVersion.isDefined)
      builder.addAll(s"gameVersion=${gameVersion.get.toString}")
    if (modLoaderType != null && modLoaderType.isDefined)
      builder.addAll(s"&modLoaderType=${modLoaderType.get.toString}")
    if (gameVersionTypeId != null && gameVersionTypeId.isDefined)
      builder.addAll(s"&gameVersionTypeId=${gameVersionTypeId.get.toString}")
    if (index != null && index.isDefined)
      builder.addAll(s"&index=${index.get.toString}")
    if (pageSize != null && pageSize.isDefined)
      builder.addAll(s"&pageSize=${pageSize.get.toString}")
    builder.toString
  }
}

class GetModFilesByModIdResponse(
    val data: Array[File]
)

class GetModFileByFileIdRequest(
    val modId: Int,
    val fileId: Int
)

class GetModFileByFileIdResponse(
    val data: File
)

class GetFileDownloadUrlsResponse(
    val data: Array[String]
)

class File(
    val id: Int,
    val gameId: Int,
    val modId: Int,
    val isAvailable: Boolean,
    val displayName: String,
    val fileDate: String,
    val fileName: String,
    val fileLength: Int,
    val downloadCount: Int,
    val gameVersions: Array[String],
    val downloadUrl: String,
    val dependencies: Array[ModDependency],
    val isServerPack: Option[Boolean],
    val serverPackFileId: Option[Int]
)

class ModDependency(
    val modId: Int,
    val relationType: Int
)

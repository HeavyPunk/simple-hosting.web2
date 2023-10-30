package business.entities.slick

import java.util.Date
import scala.collection.mutable
import components.clients.curseforge.ApiPaths.description

case class DatabaseUser(
  val id: Long,
  val creationDate: String,
  var login: String,
  var email: String,
  var passwdHash: String,
  var isAdmin: Boolean,
  var avatarUrl: Option[String],
  var isTestPeriodAvailable: Boolean,
)
case class DatabaseGameServer(
  val id: Long,
  val creationDate: String,
  val ownerId: Long,
  var hostId: Long,
  var name: String,
  val slug: String,
  var ip: String,
  var uuid: String,
  val kind: String,
  val version: String,
  var locationId: Long,
  var isPublic: Boolean,
  var isActiveVm: Boolean,
  var isActiveServer: Boolean,
  val tariffId: Long,
)
case class DatabaseLocation(
  val id: Long,
  val creationDate: String,
  var name: String,
  var description: String,
  var testIp: String,
)
case class DatabaseGameServerPort(
  val id: Long,
  val creationDate: String,
  val port: Int,
  val portKind: String,
)
case class DatabaseHost(
  val id: Long,
  val creationDate: String,
  var name: String,
  var ip: String,
)
case class DatabaseFileBucket(
  val id: Long,
  val creationDate: String,
  val storageId: Long,
  val serverId: Long,
)
case class DatabaseFileBucketFile(
  val id: Long,
  val creationDate: String,
)
case class DatabaseUserFileStorage(
  val id: Long,
  val creationDate: String,
  val ownerId: Long,
)
case class DatabaseGame(
  val id: Long,
  val creationDate: String,
  val name: String,
  val description: String,
  val iconUri: String,
)
case class DatabaseTariff(
  val id: Long,
  val creationDate: String,
  var name: String,
  val gameId: Long,
  val specificationId: Long,
  var description: String,
)
case class DatabaseTariffSpecification(
  val id: Long,
  val creationDate: String,
  var monthPrice: Double,
  var isPricePerPlayer: Boolean,
  var isMemoryPerSlot: Boolean,
  var isCpuPerSlot: Boolean, 
  var minSlots: Int,
  var maxSlots: Int,
  var imageUri: String,
  var availableDiskBytes: Long,
  var availableRamBytes: Long,
  var availableSwapBytes: Long,
  var availableCpu: Long,
  var cpuFrequency: Long,
  var cpuName: String
)
case class DatabaseTariffSpecificationPort(
  val id: Long,
  val creationDate: String,
  val port: String,
  val kind: String,
)
case class DatabaseUserSession(
  val id: Long,
  val creationDate: String,
  val userId: Long,
  val token: String,
  var data: Option[String]
)

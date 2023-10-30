package business.entities.newEntity

import java.util.Date
import components.clients.curseforge.ApiPaths.description
import java.util.UUID

case class User(
    val id: Long,
    val creationDate: Date,
    var login: String,
    var email: String,
    var passwdHash: String,
    var session: UserSession,
    var isAdmin: Boolean,
    var avatarUrl: Option[String],
    var isTestPeriodAvailable: Boolean,
)

case class UserSession(
    val id: Long,
    val creationDate: Date,
    var token: UUID,
    var data: Option[String]
)

case class GameServer(
    val id: Long,
    val creationDate: Date,
    val owner: User,
    var host: Host,
    var name: String,
    val slug: String,
    var ip: String,
    var uuid: String,
    val kind: String,
    val version: String,
    var location: Location,
    var isPublic: Boolean,
    var isActiveVm: Boolean,
    var isActiveServer: Boolean,
    val tariff: Tariff,
    val ports: Seq[GameServerPort]
)

case class Host(
    val id: Long,
    val creationDate: Date,
    val name: String,
    val ip: String,
)

case class FileBucket(
    val id: Long,
    val creationDate: Date,
    val storage: UserFileStorage,
    val server: GameServer,
    val files: Seq[FileBucketFile]
)

case class FileBucketFile(
    val id: Long,
    val creationDate: Date
)

case class UserFileStorage(
    val id: Long,
    val creationDate: Date,
    val owner: User,
    val buckets: Seq[FileBucket]
)

case class Game(
    val id: Long,
    val creationDate: Date,
    val name: String,
    val description: String,
    val iconUri: String,
    val tariffs: Seq[Tariff]
)

case class Location(
    val id: Long,
    val creationDate: Date,
    var name: String,
    var description: String,
    var testIp: String,
)

case class Tariff(
    val id: Long,
    val creationDate: Date,
    val name: String,
    val game: Game,
    val specification: TariffSpecification
)

case class TariffSpecification(
    val id: Long,
    val creationDate: Date,
    val monthPrice: Double,
    val isPricePerPlayer: Boolean,
    val isMemoryPerSlot: Boolean,
    val isCpuPerSlot: Boolean,
    val minSlots: Int,
    val maxSlots: Int,
    var availableDiskBytes: Long,
    var availableRamBytes: Long,
    var availableSwapBytes: Long,
    var availableCpu: Long,
    val vmExposePorts: Seq[TariffSpecificationPort],
    var cpuFrequency: Long,
    var cpuName: String
)

case class TariffSpecificationPort(
    val id: Long,
    val creationDate: Date,
    val port: String,
    val kind: String,
)

case class GameServerPort(
    val id: Long,
    val creationDate: Date,
    val port: Int,
    val portKind: String,
)

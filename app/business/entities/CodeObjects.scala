package business.entities.newEntity

import business.entities.Observator
import business.services.slickStorages.games.GameNotFound
import business.services.slickStorages.locations.LocationNotFound
import business.services.slickStorages.tariff.TariffNotFound
import business.services.slickStorages.user.UserNotFound
import components.basic.Monad
import components.clients.curseforge.ApiPaths.description

import java.util.Date
import java.util.UUID

case class User(
    val id: Long,
    val creationDate: Date,
    var login: String,
    var email: String,
    var passwdHash: String,
    var session: Observator[Exception, UserSession],
    var isAdmin: Boolean,
    var avatarUrl: Option[String],
    var isTestPeriodAvailable: Boolean,
)

case class UserSession(
    val id: Long,
    var creationDate: Date,
    var token: UUID,
    var data: Option[String]
)

case class GameServer(
    val id: Long,
    val creationDate: Date,
    val owner: Observator[Exception | UserNotFound, User],
    var name: String,
    val slug: String,
    var ip: String,
    var uuid: String,
    val kind: String,
    val version: String,
    var location: Observator[Exception | LocationNotFound, Location],
    var isPublic: Boolean,
    var isActiveVm: Boolean,
    var isActiveServer: Boolean,
    val tariff: Observator[Exception | TariffNotFound, Tariff],
    var ports: Observator[Exception, Seq[GameServerPort]]
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
    val storage: Observator[Exception, UserFileStorage],
    val server: Observator[Exception, GameServer],
    val files: Observator[Exception, Seq[FileBucketFile]]
)

case class FileBucketFile(
    val id: Long,
    val creationDate: Date
)

case class UserFileStorage(
    val id: Long,
    val creationDate: Date,
    val owner: Observator[Exception | UserNotFound, User],
    val buckets: Observator[Exception, Seq[FileBucket]]
)

case class Game(
    val id: Long,
    val creationDate: Date,
    val name: String,
    val description: String,
    val iconUri: String,
    val tariffs: Observator[Exception, Seq[Tariff]]
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
    val description: String,
    val game: Observator[Exception | GameNotFound, Game],
    val specification: Observator[Exception, TariffSpecification]
)

case class TariffSpecification(
    val id: Long,
    val creationDate: Date,
    val imageUri: String,
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
    val vmExposePorts: Observator[Exception, Seq[TariffSpecificationPort]],
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

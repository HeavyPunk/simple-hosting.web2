package business.entities.slick

import slick.jdbc.PostgresProfile.api._
import java.util.Date
import java.time.Instant
import slick.lifted.ProvenShape

class UsersTable(tag: Tag) extends Table[DatabaseUser](tag, "users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def login = column[String]("login")
  def email = column[String]("email")
  def passwdHash = column[String]("passwd")
  def isAdmin = column[Boolean]("is_admin", O.Default(false))
  def avatarUrl = column[Option[String]]("avatar_url", O.Default(None))
  def isTestPeriodAvailable = column[Boolean]("is_test_period_period_available", O.Default(false))
  
  override def * = (id, creationDate,login, email, passwdHash, isAdmin, avatarUrl, isTestPeriodAvailable).mapTo[DatabaseUser]
}

class UserSessionsTable(tag: Tag) extends Table[DatabaseUserSession](tag, "user_sessions") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def userId = column[Long]("user_id")
  def token = column[String]("token")
  def data = column[Option[String]]("data", O.Default(None))
  
  def userFK = foreignKey("SES_USER_FK", userId, TableQuery[UsersTable])(_.id, onDelete = ForeignKeyAction.Cascade)
  override def * = (id, creationDate, userId, token, data).mapTo[DatabaseUserSession]
}

class GameServerTable(tag: Tag) extends Table[DatabaseGameServer](tag, "game_servers") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def ownerId = column[Long]("owner_id")
  def hostId = column[Long]("host_id")
  def name = column[String]("name")
  def slug = column[String]("slug")
  def ip = column[String]("ip")
  def uuid = column[String]("uuid")
  def kind = column[String]("kind")
  def version = column[String]("version")
  def locationId = column[Long]("location_id")
  def isPublic = column[Boolean]("is_public", O.Default(false))
  def isActiveVm = column[Boolean]("is_active_vm", O.Default(false))
  def isActiveServer = column[Boolean]("is_active_server", O.Default(false))
  def tariffId = column[Long]("tariff_id")
  def * = (id, creationDate, ownerId, hostId, name, slug, ip, uuid, kind, version, locationId, isPublic, isActiveVm, isActiveServer, tariffId).mapTo[DatabaseGameServer]

  def owner = foreignKey("SERVER_OWNER_FK", ownerId, TableQuery[UsersTable])(_.id, onDelete = ForeignKeyAction.Cascade)
  def host = foreignKey("SERVER_HOST_FK", hostId, TableQuery[HostsTable])(_.id, onDelete = ForeignKeyAction.Cascade)
  def location = foreignKey("SERVER_LOC_FK", locationId, TableQuery[LocationsTable])(_.id, onDelete = ForeignKeyAction.Cascade)
  def tariff = foreignKey("SERVER_TARIFF_FK", tariffId, TableQuery[TariffsTable])(_.id, onDelete = ForeignKeyAction.Cascade)

  def ports = ??? //one to many
}

class LocationsTable(tag: Tag) extends Table[DatabaseLocation](tag, "locations") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def name = column[String]("name")
  def description = column[String]("description")
  def testIp = column[String]("test_ip")
  def * = (id, creationDate, name, description, testIp).mapTo[DatabaseLocation]
}

class GameServerPortsTable(tag: Tag) extends Table[DatabaseGameServerPort](tag, "game_server_port") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def port = column[Int]("port")
  def portKind = column[String]("port_kind")
  def * = (id, creationDate, port, portKind).mapTo[DatabaseGameServerPort]
}

class HostsTable(tag: Tag) extends Table[DatabaseHost](tag, "host") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def name = column[String]("name")
  def ip = column[String]("ip")
  def * = (id, creationDate, name, ip).mapTo[DatabaseHost]
}

class FileBucketsTable(tag: Tag) extends Table[DatabaseFileBucket](tag, "bucket") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def storageId = column[Long]("storage_id")
  def serverId = column[Long]("server_id")

  def * = (id, creationDate, storageId, serverId).mapTo[DatabaseFileBucket]

  def storage = foreignKey("BUCK_STOR_FK", id, TableQuery[UserFileStorageTable])
  def server = foreignKey("BUCK_SERV_FK", id, TableQuery[GameServerTable])

  def files = ??? //one to many
}

class FileBucketFilesTable(tag: Tag) extends Table[DatabaseFileBucketFile](tag, "bucket_files") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def * = (id, creationDate).mapTo[DatabaseFileBucketFile]
}

class UserFileStorageTable(tag: Tag) extends Table[DatabaseUserFileStorage](tag, "storage") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))
  
  def ownerId = column[Long]("owner_id")

  def * = (id, creationDate, ownerId).mapTo[DatabaseUserFileStorage]

  def owner = foreignKey("STOR_OWNER_FK", id, TableQuery[UsersTable])
  def buckets = ??? //one to many
}

class GamesTable(tag: Tag) extends Table[DatabaseGame](tag, "games") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def name = column[String]("name")
  def description = column[String]("description")
  def iconUri = column[String]("icon_uri")

  def * = (id, creationDate, name, description, iconUri).mapTo[DatabaseGame]

  def tariffs = ??? //one to many
}

class TariffsTable(tag: Tag) extends Table[DatabaseTariff](tag, "tariffs") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def name = column[String]("name")
  def gameId = column[Long]("game_id")
  def specificationId = column[Long]("specification_id")
  def description = column[String]("description")

  def * = (id, creationDate, name, gameId, specificationId, description).mapTo[DatabaseTariff]

  def game = foreignKey("TARIFF_GAME_FK", gameId, TableQuery[GamesTable])(_.id, onDelete = ForeignKeyAction.Cascade)
  def specification = foreignKey("TARIFF_SPEC_FK", specificationId, TableQuery[TariffSpecificationsTable])(_.id, onDelete = ForeignKeyAction.Cascade)
}

class TariffSpecificationsTable(tag: Tag) extends Table[DatabaseTariffSpecification](tag, "tariff_specifications") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def monthPrice = column[Double]("month_price", O.Default(0.0))
  def isPricePerPlayer = column[Boolean]("is_price_per_player")
  def isMemoryPerSlot = column[Boolean]("is_memory_per_slot")
  def isCpuPerSlot = column[Boolean]("is_cpu_per_slot")
  def minSlots = column[Int]("min_slots")
  def maxSlots = column[Int]("max_slots")
  def imageUri = column[String]("image_uri")
  def availableDiskBytes = column[Long]("available_disk_bytes")
  def availableRamBytes = column[Long]("available_ram_bytes")
  def availableSwapBytes = column[Long]("available_swap_bytes")
  def availableCpu = column[Long]("available_cpu")
  def cpuFrequency = column[Long]("cpu_frequency")
  def cpuName = column[String]("cpu_name")
  def * = (
    id,
    creationDate,
    monthPrice,
    isPricePerPlayer,
    isMemoryPerSlot,
    isCpuPerSlot,
    minSlots,
    maxSlots,
    imageUri,
    availableDiskBytes,
    availableRamBytes,
    availableSwapBytes,
    availableCpu,
    cpuFrequency,
    cpuName
    ).mapTo[DatabaseTariffSpecification]

  def vmExposePorts = ??? //one to many
}

class TariffSpecificationPortsTable(tag: Tag) extends Table[DatabaseTariffSpecificationPort](tag, "tariff_specifications_ports") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def creationDate = column[String]("creationDate", O.Default(Date.from(Instant.now).toGMTString()))

  def port = column[String]("port")
  def kind = column[String]("kind")
  def * = (id, creationDate, port, kind).mapTo[DatabaseTariffSpecificationPort]
}

package components.services.database

import slick.jdbc.PostgresProfile.api._
import business.entities.slick.{
    UsersTable,
    UserSessionsTable,
    GameServerTable,
    LocationsTable,
    GameServerPortsTable,
    HostsTable,
    FileBucketsTable,
    FileBucketFilesTable,
    UserFileStorageTable,
    GamesTable,
    TariffsTable,
    TariffSpecificationsTable,
    TariffSpecificationPortsTable
}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.jdbc.meta.MTable

class SlickDatabaseInitializer {
    def initDatabase(db: Database, timeout: Duration) = {
        val users = TableQuery[UsersTable]
        val sessions = TableQuery[UserSessionsTable]
        val gameServer = TableQuery[GameServerTable]
        val locations = TableQuery[LocationsTable]
        val portsTable = TableQuery[GameServerPortsTable]
        val hostsTable = TableQuery[HostsTable]
        val fileBuckets = TableQuery[FileBucketsTable]
        val fileBucketFile = TableQuery[FileBucketFilesTable]
        val userFileStorage = TableQuery[UserFileStorageTable]
        val games = TableQuery[GamesTable]
        val tariffs = TableQuery[TariffsTable]
        val tariffSpecifications = TableQuery[TariffSpecificationsTable]
        val tariffSpecificationPorts = TableQuery[TariffSpecificationPortsTable]

        Await.result(db.run(DBIO.seq(
            users.schema.createIfNotExists,
            sessions.schema.createIfNotExists,
            locations.schema.createIfNotExists,
            hostsTable.schema.createIfNotExists,
            fileBuckets.schema.createIfNotExists,
            fileBucketFile.schema.createIfNotExists,
            userFileStorage.schema.createIfNotExists,
            games.schema.createIfNotExists,
            tariffs.schema.createIfNotExists,
            tariffSpecifications.schema.createIfNotExists,
            tariffSpecificationPorts.schema.createIfNotExists,
            gameServer.schema.createIfNotExists,
            portsTable.schema.createIfNotExists,
        )), timeout)
    }
}
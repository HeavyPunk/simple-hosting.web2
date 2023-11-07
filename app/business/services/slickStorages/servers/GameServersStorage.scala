package business.services.slickStorages.servers

import business.services.slickStorages.BaseStorage
import business.entities.newEntity.{
    GameServer,
    User,
    UserSession,
    Game,
    Tariff,
    Location
}
import business.entities.slick.{
    GameServerTable,
    DatabaseGameServer,
    GameServerPortsTable,
    DatabaseGameServerPort,
    HostsTable,
    LocationsTable,
    TariffsTable,
    UsersTable,
    UserSessionsTable,
    TariffSpecificationPortsTable,
    GamesTable,
    TariffSpecificationsTable
}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.duration.Duration
import components.basic.{
    Monad,
    ErrorMonad,
    ResultMonad,
    mapToMonad
}
import slick.lifted.Rep
import scala.concurrent.Await
import java.util.Date
import java.util.UUID
import components.clients.curseforge.ApiPaths.description
import business.entities.newEntity.TariffSpecification
import business.services.slickStorages.user.UserStorage
import org.checkerframework.checker.units.qual.m
import business.entities.DatabaseObservator
import business.services.slickStorages.tariff.{
    TariffStorage,
    TariffNotFound,
}
import business.entities.newEntity.GameServerPort
import java.time.Instant

class GameServerNotFound
class LocationNotFound

trait GameServersStorage extends BaseStorage[GameServer, GameServerTable, GameServerNotFound]

class SlickGameServersStorage(
    db: Database,
    operationTimeout: Duration,
    userStorage: UserStorage,
    tariffStorage: TariffStorage,
) extends GameServersStorage{
    override def create(modifier: GameServer => Unit = null): GameServer = {
        val creationDate = Date.from(Instant.now())
        val gameServer = GameServer(
            id = 0,
            creationDate = creationDate,
            owner = null,
            name = "",
            slug = "",
            ip = "",
            uuid = "",
            kind = "",
            version = "",
            game = null,
            location = null,
            isPublic = false,
            isActiveVm = false,
            isActiveServer = false,
            tariff = null,
            ports = null
        )
        if (modifier != null)
            modifier(gameServer)
        gameServer
    }

    override def add(item: GameServer): Monad[Exception, Boolean] = {
        val databaseServer = DatabaseGameServer(
            0,
            item.creationDate.toGMTString(),
            item.owner.get.tryGetValue._2.id,
            item.name,
            item.slug,
            item.ip,
            item.uuid,
            item.kind,
            item.version,
            item.location.get.tryGetValue._2.id,
            item.isPublic,
            item.isActiveVm,
            item.isActiveServer,
            item.tariff.get.tryGetValue._2.id,
        )
        val databaseServerPorts = item.ports.get.tryGetValue._2 map {i => DatabaseGameServerPort(
            0, i.creationDate.toGMTString(), item.id, i.port, i.portKind
        )}

        try {
            val serversTable = TableQuery[GameServerTable]
            val portsTable = TableQuery[GameServerPortsTable]
            val addServerAction = serversTable += databaseServer
            val addPortsAction = portsTable ++= databaseServerPorts
            Await.result(db.run(addServerAction andThen addPortsAction), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

    override def update(item: GameServer): Monad[Exception, Boolean] = {
        val databaseServer = DatabaseGameServer(
            item.id,
            item.creationDate.toGMTString(),
            item.owner.get.tryGetValue._2.id,
            item.name,
            item.slug,
            item.ip,
            item.uuid,
            item.kind,
            item.version,
            item.location.get.tryGetValue._2.id,
            item.isPublic,
            item.isActiveVm,
            item.isActiveServer,
            item.tariff.get.tryGetValue._2.id,
        )
        val databaseServerPorts = item.ports.get.tryGetValue._2 map {i => DatabaseGameServerPort(
            i.id, i.creationDate.toGMTString(), item.id, i.port, i.portKind
        )}

        try {
            val serversTable = TableQuery[GameServerTable]
            val portsTable = TableQuery[GameServerPortsTable]
            val updateServerAction = serversTable filter {_.id === databaseServer.id} update databaseServer
            val updatePortsActions = DBIO.sequence(databaseServerPorts map {p => portsTable filter {_.id === p.id} update p})
            Await.result(db.run(updateServerAction andThen updatePortsActions), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

    override def find(predicate: GameServerTable => Rep[Boolean]): Monad[Exception | GameServerNotFound, Seq[GameServer]] = {
        val gameServers = TableQuery[GameServerTable]
        try {
            val action = gameServers filter(predicate)
            val servers = Await.result(db.run(action.result), operationTimeout)
            val result = servers map {ser => GameServer(
                id = ser.id,
                creationDate = Date(ser.creationDate),
                owner = DatabaseObservator(() => userStorage.find(_.id === ser.ownerId) >>= {ss => ResultMonad(ss.head)}),
                name = ser.name,
                slug = ser.slug,
                ip = ser.ip,
                uuid = ser.uuid,
                kind = ser.kind,
                version = ser.version,
                game = DatabaseObservator(() => {
                    try {
                        ???
                    } catch {
                        case e: Exception => ErrorMonad(e)
                    }
                }),
                location = DatabaseObservator(() => {
                    try {
                        val locations = TableQuery[LocationsTable]
                        val location = Await.result(db.run((locations filter(_.id === ser.locationId)).result), operationTimeout).headOption
                            .flatMap(l => Some(Location(
                                id = l.id,
                                creationDate = Date(l.creationDate),
                                name = l.name,
                                description = l.description,
                                testIp = l.testIp
                            )))
                        location match
                            case None => ErrorMonad(LocationNotFound())
                            case Some(value) => ResultMonad(value)
                    } catch {
                        case e: Exception => ErrorMonad(e)
                    }
                }),
                isPublic = ser.isPublic,
                isActiveVm = ser.isActiveVm,
                isActiveServer = ser.isActiveServer,
                tariff = DatabaseObservator(() => tariffStorage.find(t => t.id === ser.tariffId).flatMap(tariffs => tariffs.headOption.mapToMonad(TariffNotFound()))),
                ports = DatabaseObservator(() => {
                    try {
                        val gameServerPorts = TableQuery[GameServerPortsTable]
                        val searchAction = gameServerPorts.filter(p => p.gameServerId === ser.id)
                        val databasePorts = Await.result(db.run(searchAction.result), operationTimeout)
                        val ports = databasePorts map { dbp => GameServerPort(
                            dbp.id,
                            Date(dbp.creationDate),
                            dbp.port,
                            dbp.portKind
                        )}
                        ResultMonad(ports)
                    } catch {
                        case e: Exception => ErrorMonad(e)
                    }
                })
            )}
            ResultMonad(result)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

    override def remove(predicate: GameServerTable => Rep[Boolean]): Monad[Exception, Boolean] = {
        val servers = TableQuery[GameServerTable]
        try {
            val removeServersAction = Await.result(db.run(servers.filter(predicate).delete), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
}

extension (storage: GameServersStorage){
    def findById(id: Long): Monad[GameServerNotFound | Exception, GameServer] = 
        storage.find(s => s.id === id).flatMap(s => if s.length == 0 then ErrorMonad(GameServerNotFound()) else ResultMonad(s.head))
    def findByName(name: String): Monad[GameServerNotFound | Exception, GameServer] = 
        storage.find(s => s.name === name).flatMap(s => if s.length == 0 then ErrorMonad(GameServerNotFound()) else ResultMonad(s.head))
    def removeAll(): Monad[Exception, Boolean] = storage.remove(st => st.id =!= 0L)
}

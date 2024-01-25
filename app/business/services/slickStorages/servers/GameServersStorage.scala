package business.services.slickStorages.servers

import business.entities.DatabaseObservator
import business.entities.newEntity.Game
import business.entities.newEntity.GameServer
import business.entities.newEntity.GameServerPort
import business.entities.newEntity.Location
import business.entities.newEntity.Tariff
import business.entities.newEntity.TariffSpecification
import business.entities.newEntity.User
import business.entities.newEntity.UserSession
import business.entities.slick.DatabaseGameServer
import business.entities.slick.DatabaseGameServerPort
import business.entities.slick.GameServerPortsTable
import business.entities.slick.GameServerTable
import business.entities.slick.GamesTable
import business.entities.slick.HostsTable
import business.entities.slick.LocationsTable
import business.entities.slick.TariffSpecificationPortsTable
import business.entities.slick.TariffSpecificationsTable
import business.entities.slick.TariffsTable
import business.entities.slick.UserSessionsTable
import business.entities.slick.UsersTable
import business.services.slickStorages.BaseStorage
import business.services.slickStorages.games.GamesStorage
import business.services.slickStorages.games.{findById => findGameById}
import business.services.slickStorages.locations.LocationNotFound
import business.services.slickStorages.tariff.TariffNotFound
import business.services.slickStorages.tariff.TariffStorage
import business.services.slickStorages.user.UserNotFound
import business.services.slickStorages.user.UserStorage
import components.basic.ErrorMonad
import components.basic.Monad
import components.basic.ResultMonad
import components.basic.mapToMonad
import components.basic.zipWith
import components.clients.curseforge.ApiPaths.description
import org.checkerframework.checker.units.qual.m
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep

import java.time.Instant
import java.util.Date
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.google.inject.Inject

class GameServerNotFound

trait GameServersStorage extends BaseStorage[
    GameServer,
    GameServerTable,
    LocationNotFound | UserNotFound | TariffNotFound | Exception,
    LocationNotFound | UserNotFound | TariffNotFound | Exception,
    Exception,
    Exception
]

class SlickGameServersStorage @Inject() (
    db: Database,
    operationTimeout: Duration,
    userStorage: UserStorage,
    tariffStorage: TariffStorage,
    gamesStorage: GamesStorage,
) extends GameServersStorage {
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

    override def add(item: GameServer): Monad[LocationNotFound | UserNotFound | TariffNotFound | Exception, Boolean] =
        item.location.get.zipWith(item.owner.get, item.tariff.get, item.ports.get)
        .flatMap((location, owner, tariff, ports) => {
            val databaseServer = DatabaseGameServer(
                0,
                item.creationDate.toGMTString(),
                owner.id,
                item.name,
                item.slug,
                item.ip,
                item.uuid,
                item.kind,
                item.version,
                location.id,
                item.isPublic,
                item.isActiveVm,
                item.isActiveServer,
                tariff.id,
            )
            
            try {
                val serversTable = TableQuery[GameServerTable]
                val portsTable = TableQuery[GameServerPortsTable]
                val addServerAction = serversTable returning serversTable.map(_.id) += databaseServer
                val serverId = Await.result(db.run(addServerAction), operationTimeout)

                val databaseServerPorts = ports map {i => DatabaseGameServerPort(
                    0, i.creationDate.toGMTString(), serverId, i.port, i.portKind
                )}

                val addPortsAction = portsTable ++= databaseServerPorts
                Await.result(db.run(addPortsAction), operationTimeout)
                ResultMonad(true)
            } catch {
                case e: Exception => ErrorMonad(e)
            }
        })
    

    override def update(item: GameServer): Monad[LocationNotFound | UserNotFound | TariffNotFound | Exception, Boolean] =
        item.owner.get.zipWith(item.location.get, item.tariff.get, item.ports.get)
        .flatMap((owner, location, tariff, ports) => {
            val databaseServer = DatabaseGameServer(
                item.id,
                item.creationDate.toGMTString(),
                owner.id,
                item.name,
                item.slug,
                item.ip,
                item.uuid,
                item.kind,
                item.version,
                location.id,
                item.isPublic,
                item.isActiveVm,
                item.isActiveServer,
                tariff.id
            )
            val databaseServerPorts = ports map {i => DatabaseGameServerPort(
                i.id, i.creationDate.toGMTString(), item.id, i.port, i.portKind
            )}

            try {
                val serversTable = TableQuery[GameServerTable]
                val portsTable = TableQuery[GameServerPortsTable]
                val updateServerAction = serversTable filter {_.id === databaseServer.id} update databaseServer
                Await.result(db.run(updateServerAction), operationTimeout)
                if (item.ports.initialized)
                {
                    val ports = item.ports.get.tryGetValue._2
                    val removeOldPortsAction = portsTable.filter(_.gameServerId === item.id).delete
                    val addNewPortsAction = portsTable ++= ports.map {p => DatabaseGameServerPort(
                        id = 0,
                        creationDate = p.creationDate.toGMTString(),
                        gameServerId = item.id,
                        port = p.port,
                        portKind = p.portKind
                    )}
                    Await.result(db.run(removeOldPortsAction andThen addNewPortsAction), operationTimeout)
                }
                ResultMonad(true)
            } catch {
                case e: Exception => ErrorMonad(e)
            }
        })

    override def find(predicate: GameServerTable => Rep[Boolean]): Monad[Exception, Seq[GameServer]] = {
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
    def findServersByOwner(owner: User): Monad[Exception, Seq[GameServer]] =
        storage.find(s => s.ownerId === owner.id)
    def findPublicServers(kind: String): Monad[Exception, Seq[GameServer]] = 
        storage.find(s => s.isPublic === true)
    def findByHash(hash: String): Monad[GameServerNotFound | Exception, GameServer] =
        storage.find(s => s.uuid === hash).flatMap(s => if s.isEmpty then ErrorMonad(GameServerNotFound()) else ResultMonad(s.head))
    def removeById(serverId: Long): Monad[Exception, Boolean] = 
        storage.remove(s => s.id === serverId)
    def removeAll(): Monad[Exception, Boolean] = storage.remove(st => st.id =!= 0L)
}

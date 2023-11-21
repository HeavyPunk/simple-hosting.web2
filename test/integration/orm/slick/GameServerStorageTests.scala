package integration.orm.slick

import business.entities.DatabaseObservator
import business.entities.ObjectObservator
import business.entities.newEntity.Game
import business.entities.newEntity.GameServer
import business.entities.newEntity.Location
import business.entities.newEntity.Tariff
import business.entities.newEntity.TariffSpecification
import business.entities.newEntity.TariffSpecificationPort
import business.entities.newEntity.User
import business.entities.newEntity.UserSession
import business.services.slickStorages.games.GamesStorage
import business.services.slickStorages.games.{findById => findGameById}
import business.services.slickStorages.locations.LocationsStorage
import business.services.slickStorages.locations.{findById => findLocationById}
import business.services.slickStorages.servers.GameServersStorage
import business.services.slickStorages.servers.SlickGameServersStorage
import business.services.slickStorages.servers.findByHash
import business.services.slickStorages.servers.findByName
import business.services.slickStorages.servers.removeAll
import business.services.slickStorages.tariff.TariffStorage
import business.services.slickStorages.tariff.findById
import business.services.slickStorages.user.UserStorage
import business.services.slickStorages.user.findByLogin
import components.basic.zipWith
import components.clients.curseforge.ApiPaths.description
import components.services.database.SlickDatabaseInitializer
import components.services.hasher.PasswordHasher
import org.checkerframework.checker.units.qual.m
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import java.util.Date
import java.util.UUID
import scala.concurrent.duration.Duration
import components.basic.ErrorMonad
import business.services.slickStorages.games.GameNotFound
import components.basic.ResultMonad
import business.services.slickStorages.locations.LocationNotFound
import business.services.slickStorages.tariff.TariffNotFound
import business.services.slickStorages.tariff.{findById => findTariffById}
import business.entities.newEntity.GameServerPort

class GameServerStorageTests extends munit.FunSuite{
    var db: Database = null
    val timeout = Duration.create(10, "sec")
    val injector = GuiceApplicationBuilder().injector()

    override def beforeAll(): Unit = {
        db = Database.forConfig("test-pg")
        val databaseInitializer = SlickDatabaseInitializer()
        databaseInitializer.initDatabase(db, timeout)
    }
    override def afterAll(): Unit = {
        val gameServersStorage = injector.instanceOf(classOf[GameServersStorage])
        gameServersStorage.removeAll()
    }

    def createTestUserIfNeeded(login: String) = {
        val userStorage = injector.instanceOf(classOf[UserStorage])
        val user = User(
            0,
            Date.from(Instant.now()),
            login,
            s"$login@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            ObjectObservator(UserSession(0, Date.from(Instant.now()), UUID.randomUUID(), None)),
            false,
            None,
            false
        )
        userStorage.add(user)
            .flatMap(_ => userStorage.findByLogin(login))
    }

    def addOrUpdateDefaultGame(gamesStorage: GamesStorage) =
        val game = Game(
                    id = 1,
                    creationDate = Date.from(Instant.now()),
                    name = "Default-game",
                    description = "Default game",
                    iconUri = "",
                    tariffs = ObjectObservator(Seq.empty)
                )
        (gamesStorage.findGameById(1) match
            case _: ErrorMonad[GameNotFound, _] => gamesStorage.add(game)
            case _: ResultMonad[_, _] => gamesStorage.update(game))
        .flatMap(_ => gamesStorage.findGameById(1))

    def addOrUpdateDefaultLocationToDatabase(locationsStorage: LocationsStorage) = {
        val location = Location(
            1,
            creationDate = Date.from(Instant.now()),
            name = "Default",
            description = "Yandex, Russia",
            testIp = "127.0.0.1"
        )
        (locationsStorage.findLocationById(1) match
            case _: ErrorMonad[LocationNotFound, _] => locationsStorage.add(location)
            case _: ResultMonad[_, _] => locationsStorage.update(location))
        .flatMap(_ => locationsStorage.findLocationById(1))
    }

    def addOrUpdateDefaultTariffToDatabase(tariffStorage: TariffStorage, gamesStorage: GamesStorage) = {
        val tariff = Tariff(
            id = 1,
            creationDate = Date.from(Instant.now()),
            name = "Default",
            description = "Test tariff",
            game = DatabaseObservator(() => gamesStorage.findGameById(1)),
            specification = ObjectObservator(TariffSpecification(
                id = 1,
                creationDate = Date.from(Instant.now()),
                imageUri = "image",
                monthPrice = 1.0,
                isPricePerPlayer = false,
                isMemoryPerSlot = false,
                isCpuPerSlot = false,
                minSlots = 0,
                maxSlots = 100,
                availableDiskBytes = 1,
                availableRamBytes = 1,
                availableSwapBytes = 1,
                availableCpu = 1,
                vmExposePorts = ObjectObservator(Seq(TariffSpecificationPort(
                    id = 1,
                    creationDate = Date.from(Instant.now()),
                    port = "10000",
                    kind = "some-game"
                ))),
                cpuFrequency = 1,
                cpuName = "default"
            ))
        )
        (tariffStorage.findTariffById(1) match
            case _: ErrorMonad[TariffNotFound, _] => tariffStorage.add(tariff)
            case _: ResultMonad[_, _] => tariffStorage.update(tariff))
        .flatMap(_ => tariffStorage.findTariffById(1))
    }

    test("Should get location from game server"){
        val gameServerStorage = injector.instanceOf(classOf[GameServersStorage])
        val tariffsStorage = injector.instanceOf(classOf[TariffStorage])
        val locationStorage = injector.instanceOf(classOf[LocationsStorage])
        val gamesStorage = injector.instanceOf(classOf[GamesStorage])
        val user = createTestUserIfNeeded("should_get_location_from_game_server")
        val locationMonad = user.zipWith(
            addOrUpdateDefaultLocationToDatabase(locationStorage),
            addOrUpdateDefaultGame(gamesStorage),
            addOrUpdateDefaultTariffToDatabase(tariffsStorage, gamesStorage),
        )
        .flatMap((u, _, _, _) => {
            gameServerStorage.add(GameServer(
                id = 0,
                Date.from(Instant.now()),
                owner = ObjectObservator(u),
                name = "should_get_location_from_game_server",
                slug = "1234",
                ip = "127.0.0.1",
                uuid = "1234",
                kind = "lalala",
                version = "1",
                location = DatabaseObservator(() => locationStorage.findLocationById(1)),
                isPublic = false,
                isActiveVm = false,
                isActiveServer = false,
                tariff = DatabaseObservator(() => tariffsStorage.findById(1)),
                ports = ObjectObservator(Seq.empty)
            ))
        })
        .flatMap(_ => gameServerStorage.findByHash("1234"))
        .flatMap(s => s.location.get)
        
        val (err, location) = locationMonad.tryGetValue
        if (err != null)
            fail(err.toString())
        else {
            assert(location.description == "Yandex, Russia")
        }
    }
}

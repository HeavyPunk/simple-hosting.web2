package integration.orm.slick

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.duration.Duration
import components.services.database.SlickDatabaseInitializer
import business.services.slickStorages.servers.SlickGameServersStorage
import play.api.inject.guice.GuiceApplicationBuilder
import business.services.slickStorages.servers.{
    GameServersStorage,
    removeAll
}
import business.entities.newEntity.{
    GameServer,
    User,
    UserSession,
}
import java.util.Date
import java.time.Instant
import business.entities.ObjectObservator
import business.services.slickStorages.user.UserStorage
import components.services.hasher.PasswordHasher
import java.util.UUID
import business.services.slickStorages.user.findByLogin
import business.entities.newEntity.Location
import business.entities.newEntity.Game
import business.services.slickStorages.tariff.TariffStorage
import components.clients.curseforge.ApiPaths.description
import business.services.slickStorages.tariff.findById
import business.entities.DatabaseObservator

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

    test("Should get location from game server"){
        val gameServerStorage = injector.instanceOf(classOf[GameServersStorage])
        val tariffsStorage = injector.instanceOf(classOf[TariffStorage])
        val user = createTestUserIfNeeded("should_get_location_from_game_server")
        user.flatMap(u => {
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
                game = ObjectObservator(Game(
                    id = 0,
                    creationDate = Date.from(Instant.now()),
                    name = "Minecraft",
                    description = "minecraft",
                    iconUri = "none",
                    tariffs = ???
                )),
                location = ObjectObservator(Location(
                    1,
                    Date.from(Instant.now()),
                    "Default",
                    "Yandex, Russia",
                    "127.0.0.1"
                )),
                isPublic = false,
                isActiveVm = false,
                isActiveServer = false,
                tariff = DatabaseObservator(() => tariffsStorage.findById(0)),
                ports = ???
            ))
        })
    }
}

package di.modules

import business.services.slickStorages.games.GamesStorage
import business.services.slickStorages.games.SlickGameStorage
import business.services.slickStorages.locations.LocationsStorage
import business.services.slickStorages.locations.SlickLocationsStorage
import business.services.slickStorages.servers.GameServersStorage
import business.services.slickStorages.servers.SlickGameServersStorage
import business.services.slickStorages.tariff.SlickTariffStorage
import business.services.slickStorages.tariff.TariffStorage
import business.services.slickStorages.user.SlickUserStorage
import business.services.slickStorages.user.UserStorage
import com.google.inject.AbstractModule
import components.services.database.SlickDatabaseInitializer
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration.Duration

class SlickStoragesModule extends AbstractModule {
    override def configure() = {
        val db = Database.forConfig("database")
        val operationTimeout = Duration.create(10, "sec")
        val databaseInitializer = SlickDatabaseInitializer()
        databaseInitializer.initDatabase(db, operationTimeout)

        val userStorage = SlickUserStorage(db, operationTimeout)
        val gamesStorage = SlickGameStorage(db, operationTimeout)
        val locationsStorage = SlickLocationsStorage(db, operationTimeout)
        val tariffStorage = SlickTariffStorage(db, operationTimeout, gamesStorage)
        val gameServersStorage = SlickGameServersStorage(db, operationTimeout, userStorage, tariffStorage, gamesStorage)
        bind(classOf[UserStorage]).toInstance(userStorage)
        bind(classOf[GamesStorage]).toInstance(gamesStorage)
        bind(classOf[LocationsStorage]).toInstance(locationsStorage)
        bind(classOf[TariffStorage]).toInstance(tariffStorage)
        bind(classOf[GameServersStorage]).toInstance(gameServersStorage)
    }
}

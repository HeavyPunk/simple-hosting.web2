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

        bind(classOf[Database]).toInstance(db)
        bind(classOf[Duration]).toInstance(operationTimeout) //TODO: Не круто регать почти примитив в общий скоуп
        bind(classOf[UserStorage]).to(classOf[SlickUserStorage])
        bind(classOf[GamesStorage]).to(classOf[SlickGameStorage])
        bind(classOf[LocationsStorage]).to(classOf[SlickLocationsStorage])
        bind(classOf[TariffStorage]).to(classOf[SlickTariffStorage])
        bind(classOf[GameServersStorage]).to(classOf[SlickGameServersStorage])
    }
}

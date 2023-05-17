package di.modules

import com.google.inject.AbstractModule
import jakarta.persistence.Persistence
import business.services.storages.session.SessionStorage
import business.services.storages.users.UserStorage
import business.services.storages.tariffs.StupidTariffProvider
import business.services.storages.tariffs.TariffGetter
import business.services.storages.servers.GameServerStorage
import business.services.storages.hosts.HostStorage
import business.services.storages.tariffs.TariffStorage
import business.services.storages.games.GamesStorage
import business.services.storages.locations.LocationsStorage

class StoragesModule extends AbstractModule {
    override def configure(): Unit = {
        val relationEntityManager = Persistence
            .createEntityManagerFactory("com.simplehosting.relation.jpa")
            .createEntityManager()
        val userStorage = new UserStorage(relationEntityManager)
        val sessionStorage = new SessionStorage(relationEntityManager)
        val tariffsStorage = new StupidTariffProvider(relationEntityManager)
        val gameServerStorage = new GameServerStorage(relationEntityManager)
        val hostStorage = new HostStorage(relationEntityManager)
        val tariffStorage = new TariffStorage(relationEntityManager)
        val gamesStorage = new GamesStorage(relationEntityManager)
        val locationsStorage = new LocationsStorage(relationEntityManager)

        bind(classOf[SessionStorage]).toInstance(sessionStorage)
        bind(classOf[UserStorage]).toInstance(userStorage)
        bind(classOf[TariffGetter]).toInstance(tariffsStorage)
        bind(classOf[TariffStorage]).toInstance(tariffStorage)
        bind(classOf[GameServerStorage]).toInstance(gameServerStorage)
        bind(classOf[GamesStorage]).toInstance(gamesStorage)
        bind(classOf[LocationsStorage]).toInstance(locationsStorage)
    }
}

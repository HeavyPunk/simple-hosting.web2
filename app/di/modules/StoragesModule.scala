package di.modules

import com.google.inject.AbstractModule
import jakarta.persistence.Persistence
import business.services.storages.session.SessionStorage
import business.services.storages.users.UserStorage
import business.services.storages.servers.GameServerStorage
import business.services.storages.hosts.HostStorage
import business.services.storages.tariffs.TariffStorage
import business.services.storages.games.GamesStorage
import business.services.storages.locations.LocationsStorage
import com.google.inject.Guice
import components.services.log.Log
import business.services.storages.userGroups.UserGroupsStorage

class StoragesModule extends AbstractModule {
    override def configure(): Unit = {
        val relationEntityManager = Persistence
            .createEntityManagerFactory("com.simplehosting.relation.jpa")
            .createEntityManager()
        val injector = Guice.createInjector(new InfraModule)
        val log = injector.getInstance(classOf[Log])

        val userStorage = new UserStorage(relationEntityManager, log.forContext("UserStorage"))
        val sessionStorage = new SessionStorage(relationEntityManager, log.forContext("SessionStorage"))
        val gameServerStorage = new GameServerStorage(relationEntityManager, log.forContext("GameServerStorage"))
        val hostStorage = new HostStorage(relationEntityManager, log.forContext("HostStorage"))
        val tariffStorage = new TariffStorage(relationEntityManager, log.forContext("TariffStorage"))
        val gamesStorage = new GamesStorage(relationEntityManager, log.forContext("GameStorage"))
        val locationsStorage = new LocationsStorage(relationEntityManager, log.forContext("LocationStorage"))
        val groupsStorage = new UserGroupsStorage(relationEntityManager, log.forContext("GroupStorage"))

        bind(classOf[SessionStorage]).toInstance(sessionStorage)
        bind(classOf[UserStorage]).toInstance(userStorage)
        bind(classOf[TariffStorage]).toInstance(tariffStorage)
        bind(classOf[GameServerStorage]).toInstance(gameServerStorage)
        bind(classOf[GamesStorage]).toInstance(gamesStorage)
        bind(classOf[LocationsStorage]).toInstance(locationsStorage)
        bind(classOf[UserGroupsStorage]).toInstance(groupsStorage)
    }
}

package di.modules

import com.google.inject.AbstractModule
import slick.jdbc.PostgresProfile.api._
import business.services.slickStorages.user.UserStorage
import scala.concurrent.duration.Duration
import business.services.slickStorages.user.SlickUserStorage

class SlickStoragesModule extends AbstractModule {
    override def configure() = {
        val db = Database.forConfig("database")
        val operationTimeout = Duration.create(10, "sec")
        bind(classOf[UserStorage]).toInstance(SlickUserStorage(db, operationTimeout))
    }
}

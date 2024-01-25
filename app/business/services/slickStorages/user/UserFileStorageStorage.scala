package business.services.slickStorages.user

import slick.jdbc.PostgresProfile.api._
import business.services.slickStorages.BaseStorage
import business.entities.newEntity.UserFileStorage
import business.entities.slick.UserFileStorageTable
import scala.concurrent.duration.Duration
import components.basic.{
    Monad,
    zipWith
}
import slick.lifted.Rep
import components.basic.ErrorMonad

trait UserFileStorageStorage extends BaseStorage[
    UserFileStorage,
    UserFileStorageTable,
    Exception | UserNotFound,
    Exception,
    Exception,
    Exception
]

class SlickGameServersStorage(
    db: Database,
    operationTimeout: Duration,
    usersStorage: UserStorage,
) extends UserFileStorageStorage {

    override def create(modifier: UserFileStorage => Unit): UserFileStorage = ???

    override def update(item: UserFileStorage): Monad[Exception, Boolean] = {
        ???
    }

    override def add(item: UserFileStorage): Monad[Exception | UserNotFound, Boolean] = {
        item.owner.get.zipWith(item.buckets.get)
        .flatMap((owner, buckets) => {
            try {
                ???
            } catch {
                case e: Exception => ErrorMonad(e)
            }
        })
    }

    override def find(predicate: UserFileStorageTable => Rep[Boolean]): Monad[Exception, Seq[UserFileStorage]] = ???

    override def remove(predicate: UserFileStorageTable => Rep[Boolean]): Monad[Exception, Boolean] = ???
}

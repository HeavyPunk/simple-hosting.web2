package business.services.slickStorages.locations

import slick.jdbc.PostgresProfile.api._
import business.services.slickStorages.BaseStorage
import business.entities.slick.LocationsTable
import business.entities.newEntity.Location
import components.basic.Monad
import slick.lifted.Rep
import slick.lifted.TableQuery
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.Date
import components.basic.{
    ErrorMonad,
    ResultMonad,
}

class LocationNotFound

trait LocationsStorage extends BaseStorage[Location, LocationsTable, LocationNotFound]

class SlickLocationsStorage(db: Database, operationTimeout: Duration) extends LocationsStorage {
    override def create(modifier: Location => Unit = null): Location = ???
    override def add(item: Location): Monad[Exception, Boolean] = ???
    override def update(item: Location): Monad[Exception, Boolean] = ???
    override def find(predicate: LocationsTable => Rep[Boolean]): Monad[Exception | LocationNotFound, Seq[Location]] = {

        try {
            val locations = TableQuery[LocationsTable]
            val location = Await.result(db.run((locations filter(predicate)).result), operationTimeout)
                .map(l => Location(
                    id = l.id,
                    creationDate = Date(l.creationDate),
                    name = l.name,
                    description = l.description,
                    testIp = l.testIp
                ))
            ResultMonad(location)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def remove(predicate: LocationsTable => Rep[Boolean]): Monad[Exception, Boolean] = ???
}

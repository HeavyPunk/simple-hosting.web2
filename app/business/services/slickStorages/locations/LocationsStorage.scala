package business.services.slickStorages.locations

import business.entities.newEntity.Location
import business.entities.slick.DatabaseLocation
import business.entities.slick.LocationsTable
import business.services.slickStorages.BaseStorage
import components.basic.ErrorMonad
import components.basic.Monad
import components.basic.ResultMonad
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep
import slick.lifted.TableQuery

import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.google.inject.Inject

class LocationNotFound

trait LocationsStorage extends BaseStorage[Location, LocationsTable, Exception, Exception, Exception, Exception]

class SlickLocationsStorage @Inject()(db: Database, operationTimeout: Duration) extends LocationsStorage {
    override def create(modifier: Location => Unit = null): Location = ???
    override def add(item: Location): Monad[Exception, Boolean] = {
        try {
            val locations = TableQuery[LocationsTable]
            val databaseLocation = DatabaseLocation(
                id = 0,
                creationDate = item.creationDate.toGMTString(),
                name = item.name,
                description = item.description,
                testIp = item.testIp,
            )
            Await.result(db.run(locations += databaseLocation), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def update(item: Location): Monad[Exception, Boolean] = {
        try {
            val locations = TableQuery[LocationsTable]
            val databaseLocation = DatabaseLocation(
                id = item.id,
                creationDate = item.creationDate.toGMTString(),
                name = item.name,
                description = item.description,
                testIp = item.testIp,
            )
            Await.result(db.run(locations.update(databaseLocation)), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def find(predicate: LocationsTable => Rep[Boolean]): Monad[Exception, Seq[Location]] = {
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
    override def remove(predicate: LocationsTable => Rep[Boolean]): Monad[Exception, Boolean] = {
        try {
            val locations = TableQuery[LocationsTable]
            Await.result(db.run((locations filter(predicate)).delete), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
}

extension (storage: LocationsStorage)
    def findById(id: Long): Monad[LocationNotFound | Exception, Location] =
        storage.find(_.id === id).flatMap(ls => if ls.isEmpty then ErrorMonad(LocationNotFound()) else ResultMonad(ls.head))
    def getAll(): Monad[Exception, Seq[Location]] = storage.find(_.id =!= 0L)

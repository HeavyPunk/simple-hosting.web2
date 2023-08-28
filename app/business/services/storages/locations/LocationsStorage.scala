package business.services.storages.locations

import scala.jdk.CollectionConverters._
import business.services.storages.BaseStorage
import business.entities.Location
import jakarta.persistence.EntityManager
import components.services.log.Log
import components.basic.Monad
import components.basic.ErrorMonad
import components.basic.ResultMonad

class LocationNotFoundException extends Exception

class LocationsStorage(em: EntityManager, logger: Log) extends BaseStorage[Location]:
    override val entityManager: EntityManager = em
    override val log = logger

    def getAll = query("from Location")

    def findLocationById[TKey](id: TKey): Monad[Exception | LocationNotFoundException, Location] = 
        val result = findById(id)
        result match
            case r: ErrorMonad[Exception, Location] => r
            case r: ResultMonad[Exception, Location] => if r.obj != null then r else ErrorMonad(LocationNotFoundException())

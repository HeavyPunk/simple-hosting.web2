package business.services.storages.locations

import scala.jdk.CollectionConverters._
import business.services.storages.BaseStorage
import business.entities.Location
import jakarta.persistence.EntityManager

class LocationsStorage(em: EntityManager) extends BaseStorage[Location] {
    override val entityManager: EntityManager = em
    def getAll = {
        val res = em.createQuery("from Location", classOf[Location])
            .getResultList()
        res.asScala.toList
    }
}

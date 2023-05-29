package business.services.storages.locations

import scala.jdk.CollectionConverters._
import business.services.storages.BaseStorage
import business.entities.Location
import jakarta.persistence.EntityManager
import components.services.log.Log

class LocationsStorage(em: EntityManager, logger: Log) extends BaseStorage[Location] {
    override val entityManager: EntityManager = em
    override val log = logger
    def getAll = {
        val enm = em.getEntityManagerFactory.createEntityManager
        val res = try {
            enm.createQuery("from Location", classOf[Location])
                .getResultList()
        } finally {
            enm.close
        }
        if (res == null || res.isEmpty) None else Some(res.asScala.toList)
    }
}

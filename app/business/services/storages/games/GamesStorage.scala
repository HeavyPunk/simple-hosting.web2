package business.services.storages.games

import scala.jdk.CollectionConverters._
import business.services.storages.BaseStorage
import business.entities.Game
import jakarta.persistence.EntityManager
import components.services.log.Log

class GamesStorage(em: EntityManager, logger: Log) extends BaseStorage[Game] {

    override val log: Log = logger

    override val entityManager: EntityManager = em

    def getAll: List[Game] = {
        val res = em.createQuery("from Game", classOf[Game])
            .getResultList
        res.asScala.toList
    }
}

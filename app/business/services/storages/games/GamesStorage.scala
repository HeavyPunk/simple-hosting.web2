package business.services.storages.games

import scala.jdk.CollectionConverters._
import business.services.storages.BaseStorage
import business.entities.Game
import jakarta.persistence.EntityManager

class GamesStorage(em: EntityManager) extends BaseStorage[Game] {
    override val entityManager: EntityManager = em

    def getAll: List[Game] = {
        val res = em.createQuery("from Game", classOf[Game])
            .getResultList
        res.asScala.toList
    }
}

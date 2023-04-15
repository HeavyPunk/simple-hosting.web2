package business.services.storages.servers

import business.services.storages.BaseStorage
import business.entities.GameServer
import jakarta.persistence.EntityManager

class GameServerStorage(
    val em: EntityManager,
) extends BaseStorage[GameServer] {

    override val entityManager: EntityManager = em
    
    def findByUUID(id: String): Option[GameServer] = {
        val res = em.createQuery("from GameServer where uuid=:id", classOf[GameServer])
            .setParameter("id", id)
            .getResultList()
        if (res == null || res.isEmpty) None else Some(res.get(0))
    }
}

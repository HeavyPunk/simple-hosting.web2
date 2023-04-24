package business.services.storages.servers

import business.services.storages.BaseStorage
import business.entities.GameServer
import jakarta.persistence.EntityManager
import java.{util => ju}
import business.entities.User

class GameServerStorage(
    val em: EntityManager,
) extends BaseStorage[GameServer] {

    override val entityManager: EntityManager = em
    
    def findByHash(id: String): Option[GameServer] = {
        val res = em.createQuery("from GameServer where uuid=:id", classOf[GameServer])
            .setParameter("id", id)
            .getResultList()
        if (res == null || res.isEmpty) None else Some(res.get(0))
    }

    def findPublicServers(kind: String): Option[ju.List[GameServer]] = {
        val res = em.createQuery("from GameServer where kind=:kind and isPublic=true", classOf[GameServer])
            .setParameter("kind", kind)
            .getResultList()
        if (res == null || res.isEmpty) None else Some(res)
    }

    def findServersByOwner(owner: User): Option[ju.List[GameServer]] = {
        val res = em.createQuery("from GameServer where owner=:owner", classOf[GameServer])
            .setParameter("owner", owner)
            .getResultList()
        if (res == null || res.isEmpty) None else Some(res)
    }
}

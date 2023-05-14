package business.services.storages.session

import business.entities.User
import com.google.inject.Inject
import jakarta.persistence.EntityManager
import business.entities
import java.util.UUID
import org.hibernate
import business.services.storages.BaseStorage


class SessionStorage @Inject() (
    em: EntityManager
) extends BaseStorage[entities.UserSession] {

    override val entityManager: EntityManager = em

    def FindByToken(token: String): Option[entities.UserSession] = {
        val res = em.createQuery("from UserSession where token=:token", classOf[entities.UserSession])
            .setParameter("token", token)
            .getResultList()
        if (res == null || res.isEmpty) None else Some(res.get(0))
    }
}


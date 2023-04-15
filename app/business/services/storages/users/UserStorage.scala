package business.services.storages.users

import business.services.storages.BaseStorage
import business.entities.User
import com.google.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.criteria.Expression
import org.hibernate.Session
import business.entities.UserSession

class UserStorage @Inject()(
    em: EntityManager,
) extends BaseStorage[User] {

    val entityManager: EntityManager = em
    def FindByLogin(login: String): Option[User] = {
        val res = em.createQuery("from User where login=:login", classOf[User])
            .setParameter("login", login)
            .getResultList()
        if (res == null || res.isEmpty) None else Some(res.get(0))
    }

    def FindBySession[TId](session: UserSession): Option[User] = {
        val res = em.createQuery("from User where session=:session", classOf[User])
            .setParameter("session", session)
            .getResultList()
        if (res == null || res.isEmpty) None else Some(res.get(0))
    }
}

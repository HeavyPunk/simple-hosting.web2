package business.services.storages.users

import business.services.storages.BaseStorage
import business.entities.User
import com.google.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.criteria.Expression
import org.hibernate.Session
import business.entities.UserSession
import components.services.log.Log

class UserStorage @Inject()(
    em: EntityManager,
    logger: Log,
) extends BaseStorage[User] {

    val entityManager: EntityManager = em
    val log = logger

    def findByLogin(login: String): Option[User] = {
        val res = em.createQuery("from User where login=:login", classOf[User])
            .setParameter("login", login)
            .getResultList
        if (res == null || res.isEmpty) None else Some(res.get(0))
    }

    def findBySession[TId](session: UserSession): Option[User] = {
        val res = em.createQuery("from User where session=:session", classOf[User])
            .setParameter("session", session)
            .getResultList
        if (res == null || res.isEmpty) None else Some(res.get(0))
    }

    def findByEmail(email: String): Option[User] = {
        val res = em.createQuery("from User where email=:email", classOf[User])
            .setParameter("email", email)
            .getResultList
        if (res == null || res.isEmpty) None else Some(res.get(0))
    }
}

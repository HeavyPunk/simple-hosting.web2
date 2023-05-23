package integration.orm

import jakarta.persistence.Persistence
import business.entities.User
import java.util.Date
import java.time.Instant
import business.entities.GameServer
import business.entities.UserSession

class HibernateTests extends munit.FunSuite {
    val testDescContext = "[Hibernate]"
    val em = Persistence
        .createEntityManagerFactory("com.simplehosting.relation.jpa")
        .createEntityManager()
    
    test(s"$testDescContext Insert user") {
        assert(em.isOpen())
        val user = new User()
        user.login = "user"
        user.email = "user@simplehosting.com"
        user.passwdHash = "password"
        user.session = new UserSession()
        em.getTransaction().begin()
        em.persist(user)
        em.getTransaction().commit()
    }

    test(s"$testDescContext Insert game server") {
        assert(em.isOpen())

        val server = new GameServer()
        val user = em.find(classOf[User], 1)

        server.ip = "127.0.0.1"
        server.name = s"$testDescContext test-server"
        server.owner = user
        em.getTransaction().begin()
        em.persist(server)
        em.getTransaction().commit()
    }

    test(s"$testDescContext Read game server") {
        assert(em.isOpen())

        val user = em.find(classOf[GameServer], 1)
        assert(user != null)
        assert(user.ip == "127.0.0.1")
        em.detach(user)
    }
}

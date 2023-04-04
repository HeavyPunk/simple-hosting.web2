package orm

import jakarta.persistence.Persistence
import business.entities.User
import java.util.Date
import java.time.Instant
import business.entities.GameServer

class HibernateTests extends munit.FunSuite {
    val testDescContext = "[Hibernate]"
    
    test(s"$testDescContext Insert user") {
        val em = Persistence
            .createEntityManagerFactory("com.simplehosting.relation.jpa")
            .createEntityManager()
        assert(em.isOpen())
        val user = new User()
        user.login = "user"
        user.email = "user@simplehosting.com"
        user.passwdHash = "password"
        user.creationDate = Date.from(Instant.now())
        em.getTransaction().begin()
        em.persist(user)
        em.getTransaction().commit()
    }

    test(s"$testDescContext Insert game server") {
        val em = Persistence
            .createEntityManagerFactory("com.simplehosting.relation.jpa")
            .createEntityManager()
        assert(em.isOpen())

        val server = new GameServer()
        server.creationDate = Date.from(Instant.now())
        server.ip = "127.0.0.1"
        server.host = s"$testDescContext host-1"
        server.name = s"$testDescContext test-server"
        server.owner = 3
        server.ports = Array(0, 1)
        em.getTransaction().begin()
        em.persist(server)
        em.getTransaction().commit()
    }

    test(s"$testDescContext Read game server") {
        val em = Persistence
            .createEntityManagerFactory("com.simplehosting.relation.jpa")
            .createEntityManager()
        assert(em.isOpen())

        val user = em.find(classOf[GameServer], 1)
        assert(user != null)
        assert(user.ip == "127.0.0.1")
        em.detach(user)
    }
}

package integration.orm

import business.services.storages.users.UserStorage
import jakarta.persistence.Persistence
import business.services.storages.session.SessionStorage
import business.entities.UserSession
import components.services.log.FakeLog
import components.basic.ResultMonad

class OrmServicesTests extends munit.FunSuite {
    val em = Persistence
        .createEntityManagerFactory("com.simplehosting.relation.jpa")
        .createEntityManager()
    val log = new FakeLog()
    val userStorage = new UserStorage(em, log)
    val sessionStorage = new SessionStorage(em, log)

    test("UserRepository test") {
        val userMonad = userStorage.findByLogin("user")
        val (err, user) = userMonad.tryGetValue
        assert(err == null)
        assert(user != null)
    }

    test("Session storage test") {
        val user = userStorage.findByLogin("user")
        val session = new UserSession()
        val isSuccess = sessionStorage.add(session)
        val (err, result) = isSuccess.tryGetValue
        assert(err == null)
        assert(result)
    }
}

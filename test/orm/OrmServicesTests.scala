package orm

import business.services.storages.users.UserStorage
import jakarta.persistence.Persistence
import business.services.storages.session.SessionStorage
import business.entities.UserSession

class OrmServicesTests extends munit.FunSuite {
    val em = Persistence
        .createEntityManagerFactory("com.simplehosting.relation.jpa")
        .createEntityManager()
    val userStorage = new UserStorage(em)
    val sessionStorage = new SessionStorage(em)

    test("UserRepository test") {
        val user = userStorage.findByLogin("user")
        assert(user != null && user.isDefined)
    }

    test("Session storage test") {
        val user = userStorage.findByLogin("user")
        val session = new UserSession()
        val isSuccess = sessionStorage.add(session)
        assert(isSuccess)
    }
}

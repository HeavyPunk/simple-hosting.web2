package integration.orm.slick

import business.services.slickStorages.user.SlickUserStorage
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.duration.Duration
import business.entities.newEntity.{
    User,
    UserSession
}
import java.util.Date
import java.time.Instant
import components.services.hasher.PasswordHasher
import components.services.database.SlickDatabaseInitializer
import java.util.UUID
import business.services.slickStorages.user.findByEmail
import scala.concurrent.Future
import munit.GenericAfterEach
import business.services.slickStorages.user.removeAll
import components.basic.{
    ResultMonad,
    zipWith
}

class UserStorageTests extends munit.FunSuite {
    var db: Database = null
    val timeout = Duration.create(10, "sec")

    override def beforeAll(): Unit = {
        db = Database.forConfig("test-pg")
        val databaseInitializer = SlickDatabaseInitializer()
        databaseInitializer.initDatabase(db, timeout)
    }
    override def afterAll(): Unit = {
        val userStorage = SlickUserStorage(db, timeout)
        userStorage.removeAll()
    }

    test("UserStorage adds user to the database"){
        val userStorage = SlickUserStorage(db, timeout)
        val user = User(
            0,
            Date.from(Instant.now()),
            "test-user_add_test",
            "test-user_add_test@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            UserSession(0, Date.from(Instant.now()), UUID.randomUUID(), None),
            false,
            None,
            false
        )
        val (err, _) = userStorage.add(user).tryGetValue
        assert(err == null)
    }

    test("UserStorage removes user with deps"){
        val userStorage = SlickUserStorage(db, timeout)
        val user = User(
            0,
            Date.from(Instant.now()),
            "test-user_remove_test",
            "test-user_remove_test@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            UserSession(0, Date.from(Instant.now()), UUID.randomUUID(), None),
            false,
            None,
            false
        )
        val (err, _) = userStorage.add(user)
            .flatMap(u => userStorage.remove(ut => ut.login === user.login))
            .tryGetValue
        if (err != null) {
            fail(err.toString())
        }
    }

    test("UserStorage gets session from storage"){
        val userStorage = SlickUserStorage(db, timeout)
        val sessionToken = UUID.randomUUID()
        val user = User(
            0,
            Date.from(Instant.now()),
            "test-user_get_session_test",
            "test-user_get_session_test@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            UserSession(0, Date.from(Instant.now()), sessionToken, None),
            false,
            None,
            false
        )
        val (err, result) = userStorage.add(user)
            .flatMap(_ => userStorage.findByEmail("test-user_get_session_test@simplehosting.com"))
            .tryGetValue
        if (err != null) {
            fail(err.toString())
        }
        assert(result.session.token == sessionToken)
    }
    
    test("UserStorage updates user data"){
        val userStorage = SlickUserStorage(db, timeout)
        val sessionToken = UUID.randomUUID()
        val user = User(
            0,
            Date.from(Instant.now()),
            "test-user_updates_self_test",
            "test-user_updates_self_test@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            UserSession(0, Date.from(Instant.now()), sessionToken, None),
            false,
            None,
            false
        )
        val (err, result) = userStorage.add(user)
            .flatMap(_ => userStorage.findByEmail("test-user_updates_self_test@simplehosting.com"))
            .flatMap(u => {
                u.email = "test-user_updates_self_test_new_email@simplehosting.com"
                userStorage.update(u)
            })
            .flatMap(_ => userStorage.findByEmail("test-user_updates_self_test_new_email@simplehosting.com"))
            .tryGetValue
        if (err != null) {
            fail(err.toString())
        }
        assert(result.session.token == sessionToken)
    }

    test("UserStorage updates reference data"){
        val userStorage = SlickUserStorage(db, timeout)
        val sessionToken = UUID.randomUUID()
        val newSessionToken = UUID.randomUUID()
        val user = User(
            0,
            Date.from(Instant.now()),
            "test-user_updates_session_test",
            "test-user_updates_session@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            UserSession(0, Date.from(Instant.now()), sessionToken, None),
            false,
            None,
            false
        )
        val (err, result) = userStorage.add(user)
            .flatMap(_ => userStorage.findByEmail("test-user_updates_session@simplehosting.com"))
            .flatMap(u => {
                u.session.token = newSessionToken
                userStorage.update(u)
            })
            .flatMap(_ => {
                userStorage.findByEmail("test-user_updates_session@simplehosting.com")
            })
            .tryGetValue
        if (err != null) {
            fail(err.toString())
        }
        assert(result.session.token == newSessionToken)
    }
}

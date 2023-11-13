package integration.orm.mongoDb

import business.services.mongoStorages.user.UsersStorage
import com.mongodb.MongoClientSettings
import org.mongodb.scala.ConnectionString
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import scala.concurrent.duration.Duration
import business.entities.newEntity.User
import java.util.Date
import components.services.hasher.PasswordHasher
import business.entities.ObjectObservator
import business.entities.newEntity.UserSession
import java.util.UUID
import java.time.Instant
import business.services.mongoStorages.user.findByLogin
import scala.concurrent.Future
import munit.GenericAfterEach
import business.services.mongoStorages.user.removeAll

class UserStorageTest extends munit.FunSuite {
    val serverApi = ServerApi.builder().version(ServerApiVersion.V1).build()
    val mongoSettings = MongoClientSettings
        .builder()
        .applyConnectionString(ConnectionString("mongodb://simplehosting:simplehosting@127.0.0.1:27017/admin"))
        .serverApi(serverApi)
        .build()
    
    override def afterEach(context: GenericAfterEach[Future[Any]]): Unit = {
        val userStorage = UsersStorage(mongoSettings, "simplehosting", Duration(10, "sec"))
        userStorage.removeAll()
    }

    test("Adds a user to the database") {
        val userStorage = UsersStorage(mongoSettings, "simplehosting", Duration(10, "sec"))
        val user = User(
            0,
            Date.from(Instant.now()),
            "test-user_add_test",
            "test-user_add_test@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            ObjectObservator(UserSession(0, Date.from(Instant.now()), UUID.randomUUID(), None)),
            false,
            None,
            false
        )
        val (err, res) = userStorage.add(user).tryGetValue
        if (err != null) {
            fail(err.getMessage())
        }
    }

    test("Updates a user in the database") {
        val userStorage = UsersStorage(mongoSettings, "simplehosting", Duration(10, "sec"))
        val user = User(
            0,
            Date.from(Instant.now()),
            "test-user_update_test",
            "test-user_update_test@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            ObjectObservator(UserSession(0, Date.from(Instant.now()), UUID.randomUUID(), None)),
            false,
            None,
            false
        )
        val (err, _) = userStorage.add(user)
            .flatMap(_ => {
                user.passwdHash = PasswordHasher.hash("qwerty123")
                userStorage.update(user)
            })
            .tryGetValue
        
        if (err != null) {
            fail(err.getMessage())
        }
    }

    test("Get added users from database") {
        val userStorage = UsersStorage(mongoSettings, "simplehosting", Duration(10, "sec"))
        val user1 = User(
            0,
            Date.from(Instant.now()),
            "test-user_get_test_1",
            "test-user_get_test_1@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            ObjectObservator(UserSession(0, Date.from(Instant.now()), UUID.randomUUID(), None)),
            false,
            None,
            false
        )

        val user2 = User(
            0,
            Date.from(Instant.now()),
            "test-user_get_test_2",
            "test-user_get_test_2@simplehosting.com",
            PasswordHasher.hash("qwerty"),
            ObjectObservator(UserSession(0, Date.from(Instant.now()), UUID.randomUUID(), None)),
            false,
            None,
            false
        )
        val (err, res) = userStorage.add(user1)
            .flatMap(_ => userStorage.add(user2))
            .flatMap(_ => userStorage.findByLogin("test-user_get_test_1"))
            .tryGetValue
        if (err != null)
            fail(err.toString())
        else {
            assert(res.login == "test-user_get_test_1")
        }
    }
}

package controllers.v2

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting
import controllers.v2.account.LoginControllerV2
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceInjectorBuilder
import di.modules.InfraModule
import di.modules.StoragesModule
import play.api.inject.guice.GuiceApplicationBuilder
import components.services.serializer.JsonService
import components.services.log.Log
import play.api.mvc.Headers
import components.services.business.RegisterUserRequest
import java.util.UUID
import business.services.storages.users.UserNotFoundException
import play.api.Play.materializer
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import business.services.slickStorages.user.{
    UserStorage,
    UserNotFound,
    findByEmail
}
import business.entities.newEntity.User
import java.util.Date
import java.time.Instant
import business.entities.newEntity.UserSession
import business.entities.ObjectObservator

class LoginControllerV2Spec extends PlaySpec with GuiceOneAppPerTest with Injecting:
    "LoginControllerV2" should {
        "register non-existing user must be OK" in {
            val userEmail = s"user-${UUID.randomUUID().toString()}@test.com"
            val userLogin = s"user-${UUID.randomUUID().toString()}"
            val userPassword = s"user-${UUID.randomUUID().toString()}"
            val controller = inject[LoginControllerV2]
            val registerResponse = controller.register().apply(FakeRequest(POST, "/api/v2/account/register").withJsonBody(Json.parse(s"""
            {
                "login" : "${userLogin}",
                "email" : "${userEmail}",
                "password" : "${userPassword}"
            }
            """)))
            status(registerResponse) mustBe CREATED

            val userStorage = inject[UserStorage]
            val result = userStorage.findByEmail(userEmail)
            val (err, user) = result.tryGetValue
            if (err != null)
                err match
                    case _: UserNotFound => fail(s"User ${userEmail} not found")
                    case e: Exception => fail(s"Error then read database: ${e.getMessage}")
            else
                user.email must be (userEmail)
                user.login must be (userLogin)
        }
        
        "login existing user must be OK" in {
            val userEmail = s""
            val user = User(
                0,
                Date.from(Instant.now()),
                s"user-${UUID.randomUUID().toString()}",
                s"user-${UUID.randomUUID().toString()}@simplehosting.com",
                "qwerty",
                ObjectObservator(UserSession(0, Date.from(Instant.now()), UUID.randomUUID(), None)),
                false,
                None,
                true
            )
            val userStorage = inject[UserStorage]
            val (err, _) = userStorage.add(user).tryGetValue
            if (err != null)
                fail(err.toString())
            val controller = inject[LoginControllerV2]
            val loginRequest = controller.login().apply(FakeRequest(POST, "/api/v2/account/login").withJsonBody(Json.parse(s"""
            {
                "login": "${user.login}",
                "password": "${"qwerty"}"
            }
            """)))
            status(loginRequest) mustBe OK
        }
    }

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
import business.services.storages.users.UserStorage
import business.services.storages.session.SessionStorage
import components.services.serializer.JsonService
import components.services.log.Log
import play.api.mvc.Headers
import components.services.business.RegisterUserRequest
import java.util.UUID
import business.services.storages.users.UserNotFoundException
import play.api.Play.materializer

class LoginControllerV2Spec extends PlaySpec with GuiceOneAppPerTest with Injecting:

    "LoginControllerV2" should {
        "register non-existing user must be OK" in {
            val userEmail = s"user-${UUID.randomUUID().toString()}@test.com"
            val userLogin = s"user-${UUID.randomUUID().toString()}"
            val userPassword = s"user-${UUID.randomUUID().toString()}"
            val controller = inject[LoginControllerV2]
            val registerResponse = controller.register().apply(FakeRequest(
                method = "POST",
                uri = "/api/v2/account/register",
                Headers(("Accept-Type" -> "application/json")),
                body = RegisterUserRequest(userLogin, userEmail, userPassword)
            ))
            status(registerResponse) mustBe CREATED

            val userStorage = inject[UserStorage]
            val result = userStorage.findByEmail(userEmail)
            val (err, user) = result.tryGetValue
            if (err != null)
                err match
                    case _: UserNotFoundException => fail(s"User ${userEmail} not found")
                    case e: Exception => fail(s"Error then read database: ${e.getMessage}")
            else
                user.email must be (userEmail)
                user.login must be (userLogin)
        }
    }

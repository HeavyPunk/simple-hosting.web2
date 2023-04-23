package controllers

import play.api.mvc.BaseController
import components.services.serializer.JsonService
import play.api.mvc.{ ControllerComponents, Action, Request, AnyContent }
import com.google.inject.Inject
import business.services.storages.users.UserStorage
import business.services.storages.session.SessionStorage
import components.services.business.{ RegisterUserRequest, LogoutUserRequest }
import business.entities.User
import components.services.hasher.PasswordHasher
import components.services.business.LoginUserRequest
import play.api.mvc.Cookie
import business.entities.UserSession
import java.util.UUID
import components.services.business.LoginUserResponse
import play.filters.csrf.CSRF
import scala.concurrent.Future

class LoginController @Inject() (
    val controllerComponents: ControllerComponents,
    val userStorage: UserStorage,
    val sessionStorage: SessionStorage,
    val jsonizer: JsonService
) extends BaseController {
    def register() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        val rawBodyJson = request.body.asJson
        if (rawBodyJson.isEmpty)
            BadRequest
        val req = jsonizer.deserialize(rawBodyJson.get.toString, classOf[RegisterUserRequest])
        val user = User()
        user.email = req.email // TODO: replace this ugly hack
        user.login = req.login
        user.passwdHash = PasswordHasher.hash(req.password)
        val res = userStorage.add(user)
        if (res) Created else InternalServerError
    }}

    def login() = Action.async { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            Future.successful(BadRequest)
        else {
            val rawBodyJson = request.body.asJson
            if (rawBodyJson.isEmpty)
                Future.successful(BadRequest)
            else {
                val req = jsonizer.deserialize(rawBodyJson.get.toString, classOf[LoginUserRequest])
                val passHash = PasswordHasher.hash(req.password)
                val user = userStorage.FindByLogin(req.login)
                if (!user.isDefined)
                    Future.successful(Unauthorized)
                else {
                    if (!user.get.passwdHash.equals(passHash))
                        Future.successful(Unauthorized)
                    else {
                        //Set cookie
                        if (user.get.session != null){
                            val session = user.get.session
                            user.get.session = null
                            userStorage.update(user.get)
                            sessionStorage.remove(session)
                        }

                        val newSession = new UserSession()
                        newSession.token = UUID.randomUUID().toString
                        sessionStorage.add(newSession)
                        user.get.session = newSession
                        userStorage.update(user.get)

                        val resp = jsonizer.serialize(LoginUserResponse(newSession.token))
                        Future.successful(Ok(resp))
                    }
                }
            }
        }
    }}

    def logout() = Action { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            BadRequest
        else {
            val rawBody = request.body.asJson
            if (!rawBody.isDefined)
                BadRequest
            else {
                val reqObj = jsonizer.deserialize(rawBody.get.toString, classOf[LogoutUserRequest])
                val session = sessionStorage.FindByToken(reqObj.authToken)
                if (session.isEmpty)
                    Ok
                else {
                    val user = userStorage.FindBySession(session.get)
                    if (user.isEmpty) {
                        sessionStorage.remove(session.get)
                        Ok
                    }
                    else {
                        user.get.session = null
                        userStorage.update(user.get)
                        sessionStorage.remove(session.get)
                        Ok
                    }
                }
            }
        }
    }}
}

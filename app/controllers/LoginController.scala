package controllers

import play.api.mvc.BaseController
import components.services.serializer.JsonService
import play.api.mvc.{ ControllerComponents, Action, Request, AnyContent }
import com.google.inject.Inject
import business.services.storages.users.UserStorage
import business.services.storages.session.SessionStorage
import components.services.business.{ RegisterUserRequest, LogoutUserRequest, GetCurrentUserResponse }
import business.entities.User
import components.services.hasher.PasswordHasher
import components.services.business.LoginUserRequest
import play.api.mvc.Cookie
import business.entities.UserSession
import java.util.UUID
import components.services.business.{ LoginUserResponse, UserModel }
import play.filters.csrf.CSRF
import scala.concurrent.Future
import components.basic.UserTypedKey

class LoginController @Inject() (
    val controllerComponents: ControllerComponents,
    val userStorage: UserStorage,
    val sessionStorage: SessionStorage,
    val jsonizer: JsonService
) extends BaseController {

    def findUserForCurrentRequest(request: Request[AnyContent]): Option[User] = {
        val user = request.attrs.get(UserTypedKey.key)
        user
    }

    def register() = Action.async { implicit request: Request[AnyContent] => {
        if (!request.hasBody)
            Future.successful(BadRequest(s"You should specify a user's password and login"))
        else {
            val rawBodyJson = request.body.asJson
            if (rawBodyJson.isEmpty)
                Future.successful(BadRequest(s"Request body must be a json"))
            else {
                val req = jsonizer.deserialize(rawBodyJson.get.toString, classOf[RegisterUserRequest])
                val loginIsExist = userStorage.findByLogin(req.login)
                val emailIsExist = userStorage.findByEmail(req.email)
                if (loginIsExist.isDefined || emailIsExist.isDefined)
                    Future.successful(BadRequest(s"User with this login or email already exists"))
                else {
                    var user = User()
                    user.email = req.email // TODO: replace this ugly hack
                    user.login = req.login
                    user.passwdHash = PasswordHasher.hash(req.password)
                    user.isTestPeriodAvailable = true
                    val userCreated = userStorage.add(user)
                    if (!userCreated)
                        Future.successful(InternalServerError)
                    else {
                        user = userStorage.findByLogin(user.login).get
                        val newSession = new UserSession()
                        newSession.token = UUID.randomUUID().toString
                        user.session = newSession
                        userStorage.update(user)
                        Future.successful(Created(jsonizer.serialize(LoginUserResponse(
                            newSession.token,
                            UserModel(
                                0,
                                user.email,
                                user.login,
                                user.isAdmin,
                                user.avatarUrl,
                            )
                        ))))
                    }
                }
            }
        }
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
                val user = userStorage.findByLogin(req.login)
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
                        user.get.session = newSession
                        userStorage.update(user.get)

                        val resp = jsonizer.serialize(LoginUserResponse(
                            newSession.token,
                            UserModel(
                                user.get.id,
                                user.get.email,
                                user.get.login,
                                user.get.isAdmin,
                                user.get.avatarUrl,
                            )
                        ))
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
                val session = sessionStorage.findByToken(reqObj.authToken)
                if (session.isEmpty)
                    Ok
                else {
                    val user = userStorage.findBySession(session.get)
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

    def me() = Action.async { implicit request: Request[AnyContent] => {
        val user = findUserForCurrentRequest(request)
        if (user.isEmpty)
            Future.successful(NotFound(s"Cannot get user from request. Maybe you forgot put it to an auth header"))
        else {
            Future.successful(Ok(
                jsonizer.serialize(GetCurrentUserResponse(user.get.email, user.get.login, ""))
            ))
        }
    }}
}

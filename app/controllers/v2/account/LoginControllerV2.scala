package controllers.v2.account

import com.google.inject.Inject
import components.services.serializer.JsonService
import play.api.mvc.ControllerComponents
import business.services.storages.users.UserStorage
import business.services.storages.session.SessionStorage
import controllers.v2.SimpleHostingController
import play.api.mvc.AnyContent
import play.api.mvc.Request
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import play.api.mvc.BaseController
import scala.concurrent.Future
import components.services.business.RegisterUserRequest
import components.basic.{
    mapToMonad,
    zipWith,
    serializeForLog
}
import business.entities.User
import components.services.hasher.PasswordHasher
import components.basic.ResultMonad
import business.entities.UserSession
import java.util.UUID
import components.services.business.LoginUserResponse
import components.services.business.UserModel
import controllers.v2.RequestBodyNotFound
import controllers.v2.JsonNotFoundForRequestBody
import controllers.v2.JsonCannotBeParsed
import controllers.v2.UserNotFoundForRequest
import components.services.business.GetCurrentUserResponse
import components.services.business.LogoutUserRequest
import business.services.storages.session.SessionNotFoundError
import components.services.log.Log
import business.services.storages.users.UserNotFoundException
import components.services.business.LoginUserRequest

class LoginControllerV2 @Inject() (
    val controllerComponents: ControllerComponents,
    val userStorage: UserStorage,
    val sessionStorage: SessionStorage,
    val jsonizer: JsonService,
    val log: Log
) extends SimpleHostingController(jsonizer):
    def me() = Action.async { implicit request: Request[AnyContent] => {
        val result = findUserForCurrentRequest(request)
            .flatMap(u => ResultMonad(GetCurrentUserResponse(u.email, u.login, u.avatarUrl)))

        val (err, user) = result.tryGetValue
        if (err != null)
            err match
                case _: UserNotFoundForRequest => wrapToFuture(NotFound(s"Cannot get user from request. Maybe you forgot put it to an auth header"))
        else
            wrapToFuture(Ok(jsonizer.serialize(user)))
    }}

    def register() = Action.async { implicit request: Request[AnyContent] => {
        val model = this.getModelFromJsonRequest[RegisterUserRequest](request)
        val loginIsExist = model.flatMap(m => userStorage.findByLogin(m.login))
        val emailIsExist = model.flatMap(m => userStorage.findByEmail(m.email))

        if (emailIsExist.tryGetValue._2 != null || loginIsExist.tryGetValue._2 != null) {
            wrapToFuture(BadRequest("This login or email is already in use"))
        } else {
            val user = model
                .flatMap(m => {
                    var u = User()
                    u.email = m.email
                    u.login = m.login
                    u.passwdHash = PasswordHasher.hash(m.password)
                    u.isTestPeriodAvailable = true
                    ResultMonad(u)
                })

            val sessionId = UUID.randomUUID().toString()

            val sessionSaved = user
                .flatMap(u => {
                    val newSession = UserSession()
                    newSession.token = sessionId
                    u.session = newSession
                    userStorage.update(u)
                })
            
            val result = 
                user.zipWith(sessionSaved)
                .flatMap((u, _) => ResultMonad(jsonizer.serialize(LoginUserResponse(
                    sessionId,
                    UserModel(
                        0,
                        u.email,
                        u.login,
                        u.isAdmin,
                        u.avatarUrl
                    )
                ))))
            
            val (err, response) = result.tryGetValue
            if (err != null)
                err match
                    case _: RequestBodyNotFound => wrapToFuture(BadRequest(s"You should specify a user's password and login"))
                    case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(s"You should specify a user's password and login"))
                    case _: JsonCannotBeParsed => wrapToFuture(BadRequest(s"Request body must be a json"))
                    case _: Exception => wrapToFuture(InternalServerError("Server error"))
            else
                wrapToFuture(Created(response))
        }
    }}

    def login() = Action.async { implicit request: Request[AnyContent] => {
        val user = getModelFromJsonRequest[LoginUserRequest](request)
            .flatMap(req => userStorage.findByLogin(req.login))
        val result = user
            .flatMap(u => {
                if (u.session != null){
                    val session = u.session
                    u.session = null
                    userStorage.update(u).zipWith(sessionStorage.remove(session))
                } else ResultMonad((true, true))
            }).zipWith(user)
            .flatMap((_, u) => {
                var session = UserSession()
                session.token = UUID.randomUUID().toString
                u.session = session
                userStorage.update(u)
            }).zipWith(user)
        val (err, (_, u)) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(s"You should specify a user's password and login"))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(s"You should specify a user's password and login"))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(s"Request body must be a json"))
                case _: UserNotFoundException => wrapToFuture(BadRequest("User with this login not found"))
                case _: Exception => wrapToFuture(InternalServerError("Server error"))
        else wrapToFuture(Ok(jsonizer.serialize(LoginUserResponse(
            u.session.token,
            UserModel(
                u.id,
                u.email,
                u.login,
                u.isAdmin,
                u.avatarUrl
            )
        )))) 
    }}

    def logout() = Action.async { implicit request: Request[AnyContent] => {
        val session = getModelFromJsonRequest[LogoutUserRequest](request)
            .flatMap(req => sessionStorage.findByToken(req.authToken))

        val user = session
            .flatMap(s => if (s == null) null else userStorage.findBySession(s))

        val result = session.zipWith(user)
            .flatMap((s, u) => sessionStorage.remove(s))
            .zipWith(user)
            .flatMap((sessionSaved, u) => userStorage.update(u))
        
        val (err, _) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest("Request must have a body"))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest("Request body must be json object"))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest("Json is invalid"))
                case _: SessionNotFoundError => wrapToFuture(BadRequest("You should specify user session token"))
                case _: UserNotFoundException => wrapToFuture(BadRequest("User for this token not found"))
                case e: Exception => log.error(e.serializeForLog); wrapToFuture(InternalServerError("InternalServerError"))
        else
            wrapToFuture(Ok)
    }}

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
import components.services.hasher.PasswordHasher
import components.basic.ResultMonad
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
import business.services.slickStorages.user.{
    UserStorage => SUserStorage,
    findByEmail,
    findByLogin,
}
import business.entities.newEntity.User
import java.util.Date
import java.time.Instant
import business.entities.newEntity.UserSession
import business.services.slickStorages.user.UserNotFound
import business.entities.ObjectObservator

class LoginControllerV2 @Inject() (
    val controllerComponents: ControllerComponents,
    val userStorage: UserStorage,
    val sessionStorage: SessionStorage,
    val sUserStorage: SUserStorage,
    val jsonizer: JsonService,
    val log: Log
) extends SimpleHostingController(jsonizer):
    def me() = Action.async { implicit request: Request[AnyContent] => {
        val result = findUserForCurrentRequest(request)
            .flatMap(u => ResultMonad(GetCurrentUserResponse(u.email, u.login, u.avatarUrl.getOrElse(""))))

        val (err, user) = result.tryGetValue
        if (err != null)
            err match
                case _: UserNotFoundForRequest => wrapToFuture(NotFound(s"Cannot get user from request. Maybe you forgot put it to an auth header"))
        else
            wrapToFuture(Ok(jsonizer.serialize(user)))
    }}

    def register() = Action.async { implicit request: Request[AnyContent] => {
        val model = this.getModelFromJsonRequest[RegisterUserRequest](request)
        val loginIsExist = model.flatMap(m => sUserStorage.findByEmail(m.email))
        val emailIsExist = model.flatMap(m => sUserStorage.findByLogin(m.login))

        if (emailIsExist.tryGetValue._2 != null || loginIsExist.tryGetValue._2 != null) {
            wrapToFuture(BadRequest(serializeError("This login or email is already in use")))
        } else {
            val user = model
                .flatMap(m => {
                    val creationDate = Date.from(Instant.now())
                    val u = User(
                        0,
                        creationDate = creationDate,
                        m.login,
                        m.email,
                        PasswordHasher.hash(m.password),
                        ObjectObservator(UserSession(
                            0, 
                            creationDate,
                            UUID.randomUUID(),
                            None
                        )),
                        false,
                        None,
                        true,
                    )
                    ResultMonad(u)
                })

            val result = 
                user
                .flatMap(u => sUserStorage.add(u)).zipWith(user)
                .flatMap((_, u) => u.session.get).zipWith(user)
                .flatMap((session, user) => ResultMonad(jsonizer.serialize(LoginUserResponse(
                    session.token.toString(),
                    UserModel(
                        user.id,
                        user.email,
                        user.login,
                        user.isAdmin,
                        user.avatarUrl.getOrElse("")
                    )
                ))))
            
            val (err, response) = result.tryGetValue
            if (err != null)
                err match
                    case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("You should specify a user's password and login")))
                    case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("You should specify a user's password and login")))
                    case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("Request body must be a json")))
                    case _: Exception => wrapToFuture(InternalServerError(serializeError("Server error")))
            else
                wrapToFuture(Created(response))
        }
    }}

    def login() = Action.async { implicit request: Request[AnyContent] => {
        val user = getModelFromJsonRequest[LoginUserRequest](request)
            .flatMap(req => sUserStorage.findByLogin(req.login))
        val session = user
            .flatMap(u => u.session.get)
        val userUpdateAction = session
            .flatMap(session => {
                session.creationDate = Date.from(Instant.now())
                session.token = UUID.randomUUID()
                session.data = None
                ResultMonad(true)
            }).zipWith(user)
            .flatMap((_, user) => sUserStorage.update(user))
        
        val result = userUpdateAction.zipWith(user, session)
            .flatMap((_, user, session) => ResultMonad(LoginUserResponse(
                session.token.toString(),
                UserModel(
                    user.id,
                    user.email,
                    user.login,
                    user.isAdmin,
                    user.avatarUrl.getOrElse("")
                )
            )))
        
        val (err, response) = result.tryGetValue
        if (err != null)
            err match
                case _: RequestBodyNotFound => wrapToFuture(BadRequest(serializeError("You should specify a user's password and login")))
                case _: JsonNotFoundForRequestBody => wrapToFuture(BadRequest(serializeError("You should specify a user's password and login")))
                case _: JsonCannotBeParsed => wrapToFuture(BadRequest(serializeError("Request body must be a json")))
                case _: UserNotFound => wrapToFuture(BadRequest(serializeError("User with this login not found")))
                case _: Exception => wrapToFuture(InternalServerError(serializeError("Server error")))
        else wrapToFuture(Ok(jsonizer.serialize(response)))
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

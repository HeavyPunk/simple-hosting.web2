package controllers.v2.account

import com.google.inject.Inject
import play.api.mvc.ControllerComponents
import components.services.serializer.JsonService
import components.services.log.Log
import business.services.oauth2.OAuth2Authorizer
import controllers.v2.SimpleHostingController
import play.api.mvc.Request
import play.api.mvc.AnyContent
import business.services.storages.oauth.OAuthStorage
import business.entities.OAuthUser
import java.util.UUID
import components.basic.{
    ResultMonad,
    ErrorMonad,
    mapToMonad,
    zipWith
}
import business.services.oauth2.OAuth2System
import business.services.oauth2.OAuth2Manager
import components.services.business.UserModel
import business.services.oauth2.Aborted
import business.services.oauth2.AccessError
import business.services.oauth2.AccessDenied
import business.services.storages.oauth.OAuthUserNotFound
import business.services.storages.users.UserStorage

class AccessTokenError(val error: String, val description: String)
class OAuthSystemNotFound
class OAuthUserNotDefined
class UserNotRegisteredYet

class OAuth2Controller @Inject() (
    val controllerComponents: ControllerComponents,
    val oauth2Authorizer: OAuth2Authorizer,
    val oauth2Manager: OAuth2Manager,
    val oauth2Storage: OAuthStorage,
    val userStorage: UserStorage,
    val jsonizer: JsonService,
    val log: Log
) extends SimpleHostingController(jsonizer):
    def authorize() = Action.async { implicit request => {
        val user = request.getQueryString("system")
            .mapToMonad(OAuthSystemNotFound())
            .flatMap(system => {
                val user = OAuthUser()
                user.oauthKey = UUID.randomUUID().toString()
                user.oauthSystem = OAuth2System.valueOf(system)
                ResultMonad(user)
            })
        val userSaved = user.flatMap(user => oauth2Storage.add(user))
        val authorizeUrl = userSaved.zipWith(user)
            .flatMap((_, user) => oauth2Authorizer.getAuthorizeUrl(user))
            .zipWith(user)
        val (err, (responseUri, u)) = authorizeUrl.tryGetValue
        if (err != null)
            err match
                case e: OAuthSystemNotFound => wrapToFuture(BadRequest("You should set oauth system in query string as 'system'"))
                case e: Exception => wrapToFuture(InternalServerError(s"Internal Server Error: $e"))
        else
            wrapToFuture(Ok(responseUri.toString())
            .withHeaders("X-Auth-User" -> u.oauthKey))
    }}

    def checkForUserRegistered(oauthUserKey: String) = Action.async { implicit request => {
        val result = request.getQueryString("X-Auth-User").mapToMonad(OAuthUserNotDefined())
            .flatMap(oauthKey => oauth2Storage.findBySecretKey(oauthKey))
            .flatMap(oauthUser => if (oauthUser.user != null) {
                val user = oauthUser.user
                oauth2Storage.remove(oauthUser).zipWith(ResultMonad(user))
            } else ErrorMonad(UserNotRegisteredYet()))
        val (err, (_, user)) = result.tryGetValue
        if (err != null)
            err match
                case _: OAuthUserNotDefined => wrapToFuture(BadRequest(serializeError("You should specify a user by X-Auth-User")))
                case _: OAuthUserNotFound => wrapToFuture(Ok(serializeError("User not registered yet")))
                case _: UserNotRegisteredYet => wrapToFuture(Ok(serializeError("User not registered yet")))
                case e: Exception => wrapToFuture(InternalServerError(serializeError(e.toString())))
        else wrapToFuture(Ok(jsonizer.serialize(UserModel(
            user.id,
            user.email,
            user.login,
            user.isAdmin,
            user.avatarUrl
        ))))
    }}

    def saveAccessToken(oauthKey: String) = Action.async { implicit request => {
        val oauthUser = oauth2Storage.findBySecretKey(oauthKey)
        val code = request.getQueryString("code").mapToMonad(
            AccessTokenError(
                request.getQueryString("error").getOrElse(""),
                request.getQueryString("error_description").getOrElse("")
            )
        )

        val accessToken = oauthUser.zipWith(code)
            .flatMap((user, code) => oauth2Authorizer.getAccessToken(user, code))

        val userMonad = oauthUser.zipWith(code, accessToken)
            .flatMap((user, code, accessToken) => {
                user.oauthCode = code
                user.oauthToken = accessToken.accessToken
                oauth2Storage.update(user)
            })
            .zipWith(oauthUser)
            .flatMap((_, oauthUser) => oauth2Manager.constructUser(oauthUser))
        val result = userMonad.flatMap(user => userStorage.add(user))
        
        val (err, _) = result.tryGetValue
        if (err != null)
            err match
                case e: AccessTokenError => wrapToFuture(BadRequest(serializeError(s"${e.error}: ${e.description}")))
                case _: Aborted => wrapToFuture(BadRequest(serializeError("User aborted operation")))
                case e: AccessError => wrapToFuture(BadRequest(serializeError(s"${e.error}: ${e.errorDescription}")))
                case e: AccessDenied => wrapToFuture(BadRequest(serializeError(e.toString())))
                case _: OAuthUserNotFound => wrapToFuture(BadRequest(serializeError("OAuth user not found")))
                case e: Exception => wrapToFuture(InternalServerError(serializeError(e.toString())))
        else wrapToFuture(Ok(""))
    }}

import play.api.mvc.ControllerComponents
import com.google.inject.Inject
import business.services.storages.users.UserStorage
import business.services.storages.session.SessionStorage
import components.services.serializer.JsonService
import play.api.mvc.{ 
    BaseController,
    Request,
    AnyContent,
    Result
}
import components.basic.UserTypedKey
import business.entities.{
    User,
    UserSession
}
import scala.concurrent.Future
import components.services.business.{
    GetCurrentUserResponse,
    RegisterUserRequest
}
import components.services.hasher.PasswordHasher
import java.util.UUID
import components.services.business.LoginUserResponse
import components.services.business.LoginUserRequest
import components.services.business.UserModel

class LoginControllerV2 @Inject() (
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
        val req = Option(if (request.hasBody) true else null)
            .flatMap(_ => request.body.asJson)
            .flatMap(json => Some(jsonizer.deserialize(json.toString, classOf[RegisterUserRequest])))
        val loginIsExist = req.flatMap(r => userStorage.findByLogin(r.login))
        val emailIsExist = req.flatMap(r => userStorage.findByEmail(r.email))

        val result = if (loginIsExist.isDefined || emailIsExist.isDefined)
            Future.successful(BadRequest(s"User with this login or email already exists"))
        else {
            Future.successful(
                req
                    .flatMap(r => {
                        val user = User()
                        user.email = r.email
                        user.login = r.login
                        user.passwdHash = PasswordHasher.hash(r.password)
                        user.isTestPeriodAvailable = true
                        val userCreated = userStorage.add(user)
                        if (userCreated) Some(user) else None
                    })
                    .flatMap(u => userStorage.findByLogin(u.login))
                    .flatMap(u => {
                        val session = UserSession()
                        session.token = UUID.randomUUID.toString
                        u.session = session
                        if (userStorage.update(u)) Some(u) else None
                    })
                    .flatMap(u => Some(LoginUserResponse(
                        u.session.token,
                        UserModel(
                            u.id,
                            u.email,
                            u.login,
                            u.isAdmin,
                            u.avatarUrl
                        )
                    )))
                    .fold(InternalServerError("Execution pipeline returned an error"))(r => Created(jsonizer.serialize(r)))
            )
        }
        result
    }}

    def login() = Action.async { implicit request: Request[AnyContent] => {
        val req = Option(if (request.hasBody) true else null)
            .flatMap(_ => request.body.asJson)
            .flatMap(json => Some(jsonizer.deserialize(json.toString, classOf[RegisterUserRequest])))

        val result = req
            .flatMap(r => {
                val user = userStorage.findByLogin(r.login)
                if (user.isDefined)
                    Some((user.get, r))
                else None
            })
            .flatMap(u => if (u._1.passwdHash.equals(u._2.password)) Some(u._1) else None)
            .flatMap(u => {
                if (u.session != null){
                    val oldSession = u.session
                    u.session = null
                    userStorage.update(u)
                    sessionStorage.remove(oldSession)
                }
                val newSession = UserSession()
                newSession.token = UUID.randomUUID.toString
                u.session = newSession
                if (userStorage.update(u)) Some(u) else None
            })
            .flatMap(u => Some(LoginUserResponse(
                u.session.token,
                UserModel(
                    u.id,
                    u.email,
                    u.login,
                    u.isAdmin,
                    u.avatarUrl
                )
            )))
            .fold(InternalServerError("TODO: To HttpMonad"))(r => Ok(jsonizer.serialize(r)))

        Future.successful(result)
    }}

    def logout() = Action.async { implicit request: Request[AnyContent] => {
        val req = Option(if (request.hasBody) true else null)
            .flatMap(_ => request.body.asJson)
            .flatMap(json => Some(jsonizer.deserialize(json.toString, classOf[RegisterUserRequest])))
        val user = findUserForCurrentRequest(request)
        val session = user.flatMap(u => sessionStorage.findByToken(u.session.token))
        user.map(u => {
            u.session = null
            userStorage.update(u)
        }) 
        session.map(s => sessionStorage.remove(s))

        Future.successful(Ok)
    }}

    def me() = Action.async { implicit request: Request[AnyContent] => 
        Future.successful(
            findUserForCurrentRequest(request)
            .fold
                (NotFound(s"Cannot get user from request. Maybe you forgot put it to an auth header"))
                (u => Ok(jsonizer.serialize(GetCurrentUserResponse(u.email, u.login, u.avatarUrl))))
        )
    }
}
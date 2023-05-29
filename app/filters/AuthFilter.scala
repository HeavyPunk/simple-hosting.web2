package filters

import play.api.http.DefaultHttpFilters
import com.google.inject.Inject
import play.api.http.EnabledFilters
import play.api.mvc.Filter
import akka.stream.Materializer
import play.api.mvc.{RequestHeader, Result}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import business.services.storages.users.UserStorage
import business.services.storages.session.SessionStorage
import play.api.http.HttpEntity
import play.api.mvc.ResponseHeader
import play.api.libs.typedmap.TypedKey
import components.basic.UserTypedKey
import components.services.log.Log


class AuthFilter @Inject()(
    implicit val mat: Materializer,
    ec: ExecutionContext,
    val sessionStorage: SessionStorage,
    val usersStorage: UserStorage,
    val log: Log,
) extends Filter {

    override def apply(nextFilter: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
        val authHeader = "X-Auth-Token"
        val token = rh.headers.get(authHeader)
        if (token.isEmpty)
            return nextFilter(rh)
        val session = sessionStorage.findByToken(token.get)
        if (session.isEmpty) {
            log.warn(s"No found any session for ${token.get}")
            return nextFilter(rh)
        }
        val user = usersStorage.findBySession(session.get)
        if (user.isEmpty) {
            log.warn(s"No found any user for session ${token.get}")
            return nextFilter(rh)
        }
        
        nextFilter(rh.addAttr(UserTypedKey.key, user.get))
    }
}

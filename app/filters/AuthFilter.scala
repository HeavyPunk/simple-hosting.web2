package filters

import play.api.http.DefaultHttpFilters
import com.google.inject.Inject
import play.api.http.EnabledFilters
import play.api.mvc.Filter
import akka.stream.Materializer
import play.api.mvc.{RequestHeader, Result}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import play.api.http.HttpEntity
import play.api.mvc.ResponseHeader
import play.api.libs.typedmap.TypedKey
import components.basic.UserTypedKey
import components.services.log.Log


class AuthFilter @Inject()(
    implicit val mat: Materializer,
    ec: ExecutionContext,
    val log: Log,
) extends Filter {

    override def apply(nextFilter: RequestHeader => Future[Result])(rh: RequestHeader): Future[Result] = {
        // val authHeader = "X-Auth-Token"
        // val token = rh.headers.get(authHeader)
        // if (token.isEmpty)
        //     return nextFilter(rh)
        // val result = sessionStorage
        //     .findByToken(token.get)
        //     .flatMap(s => usersStorage.findBySession(s))
        
        // val (err, user) = result.tryGetValue
        // if (err != null)
        //     nextFilter(rh)
        // else nextFilter(rh.addAttr(UserTypedKey.key, user))
        nextFilter(rh)
    }
}

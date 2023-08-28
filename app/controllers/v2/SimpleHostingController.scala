package controllers.v2

import play.api.mvc.AnyContent
import components.basic.{
    Monad,
    mapToMonad
}
import play.api.mvc.Request
import business.entities.User
import components.basic.UserTypedKey
import components.basic.ErrorMonad
import components.basic.ResultMonad

class UserNotFoundForRequest extends Exception
class RequestBodyNotFound extends Exception
class JsonNotFoundForRequestBody extends Exception

class SimpleHostingController:
    def findUserForCurrentRequest(request: Request[AnyContent]): Monad[UserNotFoundForRequest, User] =
        val user = request.attrs.get(UserTypedKey.key)
        user match
            case None => ErrorMonad(UserNotFoundForRequest())
            case u: Some[User] => ResultMonad(u.get)

    def getJsonFromRequest(request: Request[AnyContent]): Monad[RequestBodyNotFound | JsonNotFoundForRequestBody, String] =
        if (!request.hasBody)
            return ErrorMonad(RequestBodyNotFound())
        request.body.asJson
            .mapToMonad(JsonNotFoundForRequestBody())
            .flatMap(js => ResultMonad(js.toString))

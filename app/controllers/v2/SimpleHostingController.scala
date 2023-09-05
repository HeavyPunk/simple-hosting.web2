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
import play.api.mvc.BaseController
import components.services.serializer.JsonService
import scala.reflect.ClassTag
import scala.concurrent.Future
import components.basic.MessageResponse

class UserNotFoundForRequest
class RequestBodyNotFound 
class JsonNotFoundForRequestBody
class JsonCannotBeParsed

abstract class SimpleHostingController(jsonizer: JsonService) extends BaseController:
    def serializeError(error: String, success: Boolean = false) = jsonizer.serialize(MessageResponse(error, success))

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
    
    def getModelFromJsonRequest[TModel: ClassTag](request: Request[AnyContent]): Monad[RequestBodyNotFound | JsonNotFoundForRequestBody | JsonCannotBeParsed, TModel] =
        getJsonFromRequest(request)
            .flatMap(json => {
                val t = implicitly[ClassTag[TModel]].runtimeClass
                val obj = jsonizer.deserialize(json, t).asInstanceOf[TModel]
                obj match
                    case null => ErrorMonad(JsonCannotBeParsed())
                    case _: TModel => ResultMonad(obj)
            })
    
    def wrapToFuture[T](obj: T): Future[T] =
        Future.successful(obj)

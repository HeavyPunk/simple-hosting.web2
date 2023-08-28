package controllers.v2.service

import components.services.serializer.JsonService
import com.google.inject.Inject
import business.services.storages.users.UserStorage
import business.services.storages.session.SessionStorage
import play.api.mvc.{
    ControllerComponents,
    BaseController,
    AnyContent,
    Request,
    Action
}

class FrontendLog @Inject() (
    val controllerComponents: ControllerComponents,
    val userStorage: UserStorage,
    val sessionStorage: SessionStorage,
    val jsonizer: JsonService
) extends BaseController {
    def log() = Action.async { implicit request: Request[AnyContent] => ???}
}

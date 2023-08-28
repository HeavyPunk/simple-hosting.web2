package controllers.v2.account

import com.google.inject.Inject
import components.services.serializer.JsonService
import play.api.mvc.ControllerComponents
import business.services.storages.users.UserStorage
import business.services.storages.session.SessionStorage
import controllers.v2.SimpleHostingController
import play.api.mvc.AnyContent
import play.api.mvc.Action

class LoginControllerV2 @Inject() (
    val controllerComponents: ControllerComponents,
    val userStorage: UserStorage,
    val sessionStorage: SessionStorage,
    val jsonizer: JsonService
) extends SimpleHostingController {
    def register() = Action.async { implicit request: Request[AnyContent] =>
    {
        getJsonFromRequest()
    }}
}

package controllers.v2.featureFlags

import play.api.mvc.ControllerComponents
import components.services.serializer.JsonService
import controllers.v2.SimpleHostingController
import business.services.storages.featureFlags.FeatureFlagsStorage
import business.services.storages.userGroups.UserGroupsStorage
import components.basic.ResultMonad
import controllers.v2.UserNotFoundForRequest
import com.google.inject.Inject
import scala.jdk.CollectionConverters.SetHasAsScala

class FeatureFlagsController @Inject() (
    val controllerComponents: ControllerComponents,
    val userGroupsStorage: UserGroupsStorage,
    val jsonizer: JsonService
) extends SimpleHostingController(jsonizer) {
    def getFeatureFlag() = Action.async { implicit request => {
        val user = findUserForCurrentRequest(request)
        val groups = user.flatMap(u => ResultMonad(u.groups))
        val featuresByGroup = groups.flatMap(gs => ResultMonad(
            Map.from(gs.map(g => (g.id, g.featureFlags.asScala.map(f => FeatureFlag(f.name, f.value, f.valueType)))))
        ))
        val (err, result) = featuresByGroup.tryGetValue
        if (err != null)
            err match
                case _: UserNotFoundForRequest => wrapToFuture(BadRequest(serializeError("You should define user-token in X-Auth-Token header")))
        else
            wrapToFuture(Ok(jsonizer.serialize(GetFeatureFlagsResponse(???))))
    }}

    def subscribeForFeatureFlag() = Action.async { implicit request => {
        ???
    }}
}

package components.basic

import play.api.libs.typedmap.TypedKey
import business.entities.newEntity.User

object UserTypedKey {
    val key: TypedKey[User] = TypedKey("user")
}

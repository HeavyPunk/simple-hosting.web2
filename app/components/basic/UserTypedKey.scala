package components.basic

import play.api.libs.typedmap.TypedKey
import business.entities.User

object UserTypedKey {
    val key: TypedKey[User] = TypedKey("user")
}

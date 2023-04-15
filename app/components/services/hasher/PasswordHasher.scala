package components.services.hasher

import com.google.common.hash.Hashing
import com.google.common.base.Charsets

object PasswordHasher {
    def hash(input: String): String = Hashing.sha512().hashString(input, Charsets.UTF_8).toString()
}

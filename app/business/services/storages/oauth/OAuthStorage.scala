package business.services.storages.oauth

import business.services.storages.BaseStorage
import business.entities.OAuthUser
import com.google.inject.Inject
import jakarta.persistence.EntityManager
import components.services.log.Log
import components.basic.Monad
import components.basic.ErrorMonad
import components.basic.ResultMonad

class OAuthUserNotFound

class OAuthStorage @Inject() (
    em: EntityManager,
    logger: Log
) extends BaseStorage[OAuthUser]:

    override val entityManager: EntityManager = em

    override val log: Log = logger

    def findBySecretKey(secretKey: String): Monad[Exception | OAuthUserNotFound, OAuthUser] = 
        val result = query(
            "from OAuthUser where oauthKey=:oauth_key",
            ("oauth_key" -> secretKey)
        )
        result match
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj.isEmpty then ErrorMonad(OAuthUserNotFound()) else ResultMonad(r.obj(0))
    def findBySecretCode(secretCode: String): Monad[Exception | OAuthUserNotFound, OAuthUser] =
        val result = query(
            "from OAuthUser where oauthCode=:oauth_code",
            ("oauth_code" -> secretCode)
        )
        result match
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj.isEmpty then ErrorMonad(OAuthUserNotFound()) else ResultMonad(r.obj(0))


package business.services.storages.session

import business.entities.User
import com.google.inject.Inject
import jakarta.persistence.EntityManager
import java.util.UUID
import org.hibernate
import business.services.storages.BaseStorage
import components.services.log.Log
import components.basic.Monad
import business.entities.UserSession
import components.basic.ErrorMonad
import components.basic.ResultMonad


class SessionNotFoundException extends Exception

class SessionStorage @Inject() (
    em: EntityManager,
    logger: Log
) extends BaseStorage[UserSession]:

    override val entityManager: EntityManager = em
    override val log = logger

    def findByToken(token: String): Monad[Exception | SessionNotFoundException, UserSession] =
        val result = query(
            "from UserSession where token=:token",
            ("token" -> token)
        )
        result match 
            case r: ErrorMonad[Exception, List[UserSession]] => ErrorMonad(r.err)
            case r: ResultMonad[Exception, List[UserSession]] => if r.obj.length == 0 then ErrorMonad(SessionNotFoundException()) else ResultMonad(r.obj(0))

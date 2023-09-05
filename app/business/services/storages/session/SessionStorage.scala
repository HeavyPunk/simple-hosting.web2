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


class SessionNotFoundError

class SessionStorage @Inject() (
    em: EntityManager,
    logger: Log
) extends BaseStorage[UserSession]:

    override val entityManager: EntityManager = em
    override val log = logger

    def findByToken(token: String): Monad[Exception | SessionNotFoundError, UserSession] =
        val result = query(
            "from UserSession where token=:token",
            ("token" -> token)
        )
        result match 
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj.length == 0 then ErrorMonad(SessionNotFoundError()) else ResultMonad(r.obj(0))
    
    def findSessionById[TId](id: TId): Monad[Exception | SessionNotFoundError, UserSession] =
        val result = this.findById(id)
        result match
            case r: ResultMonad[?, ?] => if r.obj == null then ErrorMonad(SessionNotFoundError()) else ResultMonad(r.obj)
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)

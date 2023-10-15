package business.services.storages.users

import business.services.storages.BaseStorage
import business.entities.User
import com.google.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.criteria.Expression
import org.hibernate.Session
import business.entities.UserSession
import components.services.log.Log
import components.basic.Monad
import components.basic.ErrorMonad
import components.basic.ResultMonad

class UserNotFoundException

class UserStorage @Inject()(
    em: EntityManager,
    logger: Log,
) extends BaseStorage[User]:

    val entityManager: EntityManager = em
    val log = logger

    def findByLogin(login: String): Monad[Exception | UserNotFoundException, User] = 
        val result = query(
            "from User where login=:login",
            ("login" -> login),
        )
        result match
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj.length == 0 then ErrorMonad(UserNotFoundException()) else ResultMonad(r.obj(0))

    def findBySession[TId](session: UserSession): Monad[Exception | UserNotFoundException, User] =
        val result = query(
            "from User where session=:session",
            ("session" -> session),
        )
        result match
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj.length == 0 then ErrorMonad(UserNotFoundException()) else ResultMonad(r.obj(0))

    def findByEmail(email: String): Monad[Exception | UserNotFoundException, User] =
        val result = query(
            "from User where email=:email",
            ("email" -> email)
        )
        result match
            case r: ErrorMonad[?, ?] => ErrorMonad(r.err)
            case r: ResultMonad[?, ?] => if r.obj.length == 0 then ErrorMonad(UserNotFoundException()) else ResultMonad(r.obj(0))

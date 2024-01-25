package business.services.slickStorages.user

import business.entities.ObjectObservator
import business.entities.newEntity.User
import business.entities.newEntity.UserSession
import business.entities.slick.DatabaseUser
import business.entities.slick.DatabaseUserSession
import business.entities.slick.UserSessionsTable
import business.entities.slick.UsersTable
import business.services.slickStorages.BaseStorage
import components.basic.ErrorMonad
import components.basic.Monad
import components.basic.ResultMonad
import components.clients.curseforge.ApiPaths.categories
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import java.time.Instant
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeoutException
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import com.google.inject.Inject

class UserNotFound

trait UserStorage extends BaseStorage[User, UsersTable, Exception, Exception, Exception, Exception]:
    def findByToken(token: String): Monad[UserNotFound | Exception, User]


class SlickUserStorage @Inject() (db: Database, operationTimeout: Duration) extends UserStorage {
    override def create(modifier: User => Unit = null): User = {
        val creationDate = Date.from(Instant.now())
        val user = User(
            id = 0,
            creationDate = creationDate,
            login = "",
            email = "",
            passwdHash = "",
            session = null,
            isAdmin = false,
            avatarUrl = None,
            isTestPeriodAvailable = false
        )
        if (modifier != null)
            modifier(user)
        user
    }

    override def add(item: User): Monad[Exception, Boolean] = {
        val users = TableQuery[UsersTable]
        val sessions = TableQuery[UserSessionsTable]
        val currentDate = Date.from(Instant.now()).toGMTString()

        try {
            val user = DatabaseUser(0, currentDate, item.login, item.email, item.passwdHash, item.isAdmin, item.avatarUrl, item.isTestPeriodAvailable)
            val userId = Await.result(db.run(users returning users.map(_.id) += user), operationTimeout)
            val session = DatabaseUserSession(
                item.session.get.tryGetValue._2.id,
                item.session.get.tryGetValue._2.creationDate.toGMTString(),
                userId,
                item.session.get.tryGetValue._2.token.toString(),
                item.session.get.tryGetValue._2.data
            )
            val insertSessionAction = sessions += session
            Await.result(db.run(insertSessionAction), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

    override def update(item: User): Monad[Exception, Boolean] = {
        val users = TableQuery[UsersTable]
        val sessions = TableQuery[UserSessionsTable]
        try {
            val userUpdateAction = users.filter(_.id === item.id).update(DatabaseUser(
                item.id,
                item.creationDate.toGMTString(),
                item.login,
                item.email,
                item.passwdHash,
                item.isAdmin,
                item.avatarUrl,
                item.isTestPeriodAvailable
            ))
            val sessionUpdateAction = sessions.filter(_.id === item.session.get.tryGetValue._2.id).update(DatabaseUserSession(
                item.session.get.tryGetValue._2.id,
                item.session.get.tryGetValue._2.creationDate.toGMTString(),
                item.id,
                item.session.get.tryGetValue._2.token.toString(),
                item.session.get.tryGetValue._2.data
            ))
            Await.result(db.run(userUpdateAction.andThen(sessionUpdateAction)), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

    override def find(predicate: UsersTable => Rep[Boolean]): Monad[Exception, Seq[User]] = {
        val users = TableQuery[UsersTable]
        val sessions = TableQuery[UserSessionsTable]
        try {
            val innerJoin = for {
                (u, s) <- users filter(predicate) join sessions on (_.id === _.userId)
            } yield (u.id, u.creationDate, u.login, u.email, u.passwdHash, s.id, s.creationDate, s.token, s.data, u.isAdmin, u.avatarUrl, u.isTestPeriodAvailable)
            val dbRes = Await.result(db.run(innerJoin.result), operationTimeout)
            val res = dbRes.map(dbResult => User(
                dbResult._1,
                java.util.Date(dbResult._2),
                dbResult._3,
                dbResult._4,
                dbResult._5,
                ObjectObservator(UserSession(dbResult._6, java.util.Date(dbResult._7), UUID.fromString(dbResult._8), dbResult._9)),
                dbResult._10,
                dbResult._11,
                dbResult._12
            ))
            ResultMonad(res)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

    override def findByToken(token: String): Monad[UserNotFound | Exception, User] = {
        val users = TableQuery[UsersTable]
        val sessions = TableQuery[UserSessionsTable]
        try {
            val innerJoin = for {
                (u, s) <- users join sessions on (_.id === _.userId) filter(s => s._2.token === token)
            } yield (u.id, u.creationDate, u.login, u.email, u.passwdHash, s.id, s.creationDate, s.token, s.data, u.isAdmin, u.avatarUrl, u.isTestPeriodAvailable)
            val dbRes = Await.result(db.run(innerJoin.result), operationTimeout)
            val res = dbRes.map(dbResult => User(
                dbResult._1,
                java.util.Date(dbResult._2),
                dbResult._3,
                dbResult._4,
                dbResult._5,
                ObjectObservator(UserSession(dbResult._6, java.util.Date(dbResult._7), UUID.fromString(dbResult._8), dbResult._9)),
                dbResult._10,
                dbResult._11,
                dbResult._12
            ))
            if res.isEmpty then ErrorMonad(UserNotFound()) else ResultMonad(res.head)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

    override def remove(predicate: UsersTable => Rep[Boolean]): Monad[Exception, Boolean] = {
        val users = TableQuery[UsersTable]
        try {
            val removeUsersAction = Await.result(db.run(users.filter(predicate).delete), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

}

extension (storage: UserStorage){
    def findByLogin(login: String): Monad[UserNotFound | Exception, User] = 
        storage.find(u => u.login === login).flatMap(u => if u.length == 0 then ErrorMonad(UserNotFound()) else ResultMonad(u.head))
    def findByEmail(email: String): Monad[UserNotFound | Exception, User] =
        storage.find(u => u.email === email).flatMap(u => if u.length == 0 then ErrorMonad(UserNotFound()) else ResultMonad(u.head))
    def removeAll(): Monad[Exception, Boolean] = storage.remove(ut => ut.login =!= "")
}

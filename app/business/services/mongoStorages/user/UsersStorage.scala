package business.services.mongoStorages.user

import org.mongodb.scala.MongoClientSettings
import scala.concurrent.duration.Duration
import business.services.mongoStorages.BaseMongoStorage
import business.entities.newEntity.User
import components.basic.Monad
import components.basic.ErrorMonad
import scala.util.Using
import org.mongodb.scala.MongoClient
import scala.concurrent.Await
import components.basic.ResultMonad
import org.mongodb.scala.bson.collection.immutable.Document

import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.UpdateOptions
import org.bson.conversions.Bson
import business.entities.newEntity.UserSession
import java.util.Date
import java.util.UUID
import business.entities.ObjectObservator

class UserNotFound

class UsersStorage(settings: MongoClientSettings, databaseName: String, operationTimeout: Duration) extends BaseMongoStorage[User]{
    override def add(entity: User): Monad[Exception, Boolean] = {
        try {
            var result: Monad[Exception, Boolean] = null
            Using(MongoClient(settings)){ mongoClient =>
                val database = mongoClient.getDatabase(databaseName)
                result = entity.session.get
                    .flatMap(s => {
                        try {
                            Await.result(database.getCollection("users").insertOne(Document(
                                "creation_date" -> entity.creationDate.toGMTString(),
                                "session" -> Document(
                                    "creation_date" -> s.creationDate.toGMTString(),
                                    "token" -> s.token.toString(),
                                    "data" -> s.data
                                ),
                                "login" -> entity.login,
                                "email" -> entity.email,
                                "passwd_hash" -> entity.passwdHash,
                            )).head(), operationTimeout)
                            ResultMonad(true)
                        } catch {
                            case e: Exception => ErrorMonad(e)
                        }
                    })
            }
            result
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def update(entity: User): Monad[Exception, Boolean] = {
        try {
            Using(MongoClient(settings)){ mongoClient =>
                val database = mongoClient.getDatabase(databaseName)
                entity.session.get
                    .flatMap(s => {
                        try {
                            Await.result(database.getCollection("users").updateOne(
                                equal("login", entity.login),
                                combine(
                                    set("email", entity.email),
                                    set("passwd_hash", entity.passwdHash)
                                )
                            ).head(),
                                operationTimeout
                            )
                            ResultMonad(true)
                        } catch {
                            case e: Exception => ErrorMonad(e)
                        }
                    })
            }
            .get
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def addOrUpdate(entity: User): Monad[Exception, Boolean] = ???

    override def find(predicate: Bson): Monad[Exception, Seq[User]] = {
        try {
            Using(MongoClient(settings)){ mongoClient =>
                val database = mongoClient.getDatabase(databaseName)
                val res = for {
                    (record) <- database.getCollection("users").find(predicate)
                } yield User(
                    // id = record.get("_id").get.asNumber().longValue(),
                    id = 0,
                    creationDate = Date(record.get("creation_date").get.asString().getValue()),
                    login = record.get("login").get.asString().getValue(),
                    email = record.get("email").get.asString().getValue(),
                    passwdHash = record.get("passwd_hash").get.asString().getValue(),
                    ObjectObservator(UserSession(
                        // id = record.get("session").get.asDocument().get("_id").asNumber().longValue(),
                        id = 0,
                        creationDate = Date(record.get("session").get.asDocument().get("creation_date").asString().getValue()),
                        token = UUID.fromString(record.get("session").get.asDocument().get("token").asString().getValue()),
                        data = Some(record.get("session").get.asDocument().get("data").asString().toString())
                    )),
                    isAdmin = record.get("is_admin").get.asBoolean().getValue(),
                    avatarUrl = record.get("avatar_url"),
                    isTestPeriodAvailable = record.get("is_test_period_available").get.asBoolean().getValue()
                )
                ResultMonad(Await.result(res.collect().head(), operationTimeout))
            }
            .get
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

    override def remove(predicate: Bson = null): Monad[Exception, Boolean] = {
        try {
            Using(MongoClient(settings)){ mongoClient =>
                val database = mongoClient.getDatabase(databaseName)
                Await.result(
                    if predicate == null
                    then database.getCollection("users").drop().head()
                    else database.getCollection("users").deleteMany(predicate).head(),
                    operationTimeout
                )
            }
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
}

extension (storage: UsersStorage)
    def findByLogin(login: String): Monad[Exception | UserNotFound, User] =
        storage.find(equal("login", login)).flatMap(us => if us.length == 0 then ErrorMonad(UserNotFound()) else ResultMonad(us(0)))
    def findByEmail(email: String) = 
        storage.find(equal("email", email)).flatMap(us => if us.length == 0 then ErrorMonad(UserNotFound()) else ResultMonad(us(0)))
    def removeAll() = storage.remove()

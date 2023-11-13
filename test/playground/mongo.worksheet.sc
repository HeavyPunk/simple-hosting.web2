import java.util.concurrent.TimeUnit
import org.mongodb.scala.Observable
import org.mongodb.scala.bson.collection.immutable.Document
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.UUID
import business.entities.newEntity.UserSession
import business.entities.ObjectObservator
import java.util.Date
import business.entities.newEntity.User
import org.mongodb.scala.MongoClient
import scala.util.Using
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApiVersion
import com.mongodb.ServerApi
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._


val serverApi = ServerApi.builder().version(ServerApiVersion.V1).build()
val mongoSettings = MongoClientSettings
    .builder()
    .applyConnectionString(ConnectionString("mongodb://simplehosting:simplehosting@127.0.0.1:27017/admin"))
    .serverApi(serverApi)
    .build()

val databaseName = "simplehosting"
val operationTimeout = Duration(10, "sec")
val mongoClient = MongoClient(mongoSettings)
val database = mongoClient.getDatabase(databaseName)
val createCollectionAction = database.createCollection("test_1")
Await.result(createCollectionAction.head(), operationTimeout)

val collection = MongoClient(mongoSettings)
    .getDatabase(databaseName)
    .getCollection("test_1")

val addAction = collection.insertOne(Document("lalala" -> 1))
Await.result(addAction.head(), operationTimeout)

val res = for {
    findResult <- collection.find()
} yield findResult

val r = Await.result(res.head(), operationTimeout)

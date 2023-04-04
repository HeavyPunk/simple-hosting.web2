package business.entities

import jakarta.persistence.{ Entity, Table, Id, GenerationType, GeneratedValue, Column }
import org.hibernate.annotations.{ GenericGenerator }
import java.util.Date
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType

@Entity
@Table(name = "users")
case class User (){
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    var id: Int = 0
    @Temporal(TemporalType.TIMESTAMP)
    var creationDate: Date = null
    @Column(name = "login") var login: String = ""
    @Column(name = "email") var email: String = ""
    @Column(name = "passwd") var passwdHash: String = ""
}

@Entity
@Table(name = "game_servers")
case class GameServer (){
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    var id: Int = 0

    @Temporal(TemporalType.TIMESTAMP)
    var creationDate: Date = null

    @Column(name = "owner") var owner: Int = 0
    @Column(name = "host") var host: String = ""
    @Column(name = "name") var name: String = ""
    @Column(name = "ip") var ip: String = ""
    @Column(name = "ports") var ports: Array[Int] = null
}

@Entity
@Table(name = "game_server_port")
case class GameServerPort() {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    val id: Int = 0

    @Temporal(TemporalType.TIMESTAMP)
    var creationDate: Date = null

    @Column(name = "port") val port: String = ""
    @Column(name = "portKind") val portKind: String = ""
}

@Entity
@Table(name = "host")
case class Host() {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    val id: Int = 0
}

@Entity
@Table(name = "bucket")
case class FileBucket () {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    val id: Int = 0
    @Column(name = "storage") val storage: Int = 0
    @Column(name = "game_server") val server: Int = 0
    @Column(name = "files") val files: Array[Int] = null
}

case class FileBucketFile (val id: Int, val contentBase64: String)

@Entity
@Table(name = "storage")
case class UserFileStorage () {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    val id: Int = 0
    @Column(name = "owner") val owner: Int = 0
    @Column(name = "bucket") val buckets: Array[Int] = null
}
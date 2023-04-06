package business.entities

import jakarta.persistence.{ 
    Entity,
    Table,
    Id,
    GenerationType,
    GeneratedValue,
    Column,
    Temporal,
    TemporalType,
    PrePersist,
    PreUpdate,
    MappedSuperclass
}
import org.hibernate.annotations.{ GenericGenerator }
import java.util.Date
import java.time.Instant

@MappedSuperclass
class BaseEntity{
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    var id: Long = 0
    @Temporal(TemporalType.TIMESTAMP)
    var creationDate: Date = null
    @Temporal(TemporalType.TIMESTAMP)
    var updateDate: Date = null

    @PrePersist
    def onCreate() = creationDate = Date.from(Instant.now); updateDate = Date.from(Instant.now)
    @PreUpdate
    def onUpdate() = updateDate = Date.from(Instant.now)
}

@Entity
@Table(name = "users")
case class User () extends BaseEntity{
    @Column(name = "login") var login: String = ""
    @Column(name = "email") var email: String = ""
    @Column(name = "passwd") var passwdHash: String = ""
}

@Entity
@Table(name = "game_servers")
case class GameServer () extends BaseEntity{
    @Column(name = "owner") var owner: User = null
    @Column(name = "host") var host: String = ""
    @Column(name = "name") var name: String = ""
    @Column(name = "ip") var ip: String = ""
    @Column(name = "ports") var ports: Array[GameServerPort] = null
}

@Entity
@Table(name = "game_server_port")
case class GameServerPort() extends BaseEntity {
    @Column(name = "port") val port: String = ""
    @Column(name = "portKind") val portKind: String = ""
}

@Entity
@Table(name = "host")
case class Host() extends BaseEntity {
    @Column(name = "name") var name: String = ""
}

@Entity
@Table(name = "bucket")
case class FileBucket() extends BaseEntity {
    @Column(name = "storage") val storage: UserFileStorage = null
    @Column(name = "game_server") val server: GameServer = null
    @Column(name = "files") val files: Array[Long] = null
}

case class FileBucketFile (val contentBase64: String) extends BaseEntity

@Entity
@Table(name = "storage")
case class UserFileStorage() extends BaseEntity {
    @Column(name = "owner") val owner: User = null
    @Column(name = "bucket") val buckets: Array[FileBucket] = null
}
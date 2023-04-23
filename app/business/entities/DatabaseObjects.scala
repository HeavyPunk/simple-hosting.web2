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
import java.util.UUID
import jakarta.persistence.OneToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.JoinColumn
import jakarta.persistence.CascadeType

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
case class User () extends BaseEntity {
    @Column(name = "login") var login: String = ""
    @Column(name = "email") var email: String = ""
    @Column(name = "passwd") var passwdHash: String = ""
    @OneToOne(cascade = Array(CascadeType.ALL))
    @JoinColumn(name = "session") var session: UserSession = null
}

@Entity
@Table(name = "game_servers")
case class GameServer () extends BaseEntity {
    @ManyToOne()
    @JoinColumn(name = "owner")
    var owner: User = null

    @ManyToOne()
    @JoinColumn(name = "host") var host: Host = null

    @Column(name = "name") var name: String = ""
    @Column(name = "ip") var ip: String = ""
    @Column(name = "uuid") var uuid: String = ""
    @Column(name = "kind") var kind: String = ""
    @Column(name = "is_public") var isPublic: Boolean = false
    @Column(name = "is_active_vm") var isActiveVm: Boolean = false
    @Column(name = "is_active_server") var isActiveServer: Boolean = false

    @ManyToOne()
    @JoinColumn(name = "tariff") var tariff: Tariff = null

    @OneToMany(cascade = Array(CascadeType.ALL))
    @Column(name = "ports")
    var ports: Array[GameServerPort] = null
}

@Entity
@Table(name = "game_server_port")
case class GameServerPort() extends BaseEntity {
    @Column(name = "port") var port: Int = -1
    @Column(name = "portKind") var portKind: String = ""
}

@Entity
@Table(name = "host")
case class Host() extends BaseEntity {
    @Column(name = "name") var name: String = ""
    @Column(name = "ip") var ip: String = ""
}

@Entity
@Table(name = "bucket")
case class FileBucket() extends BaseEntity {
    @OneToOne()
    @JoinColumn(name = "storage")
    val storage: UserFileStorage = null

    @OneToOne
    @JoinColumn(name = "game_server")
    val server: GameServer = null

    @Column(name = "files") val files: Array[Long] = null
}

@Entity
@Table(name = "bucket_files")
case class FileBucketFile() extends BaseEntity {
    @Column(name = "contentBase64")
    val contentBase64: String = null
}

@Entity
@Table(name = "storage")
case class UserFileStorage() extends BaseEntity {
    @OneToOne()
    @JoinColumn(name = "owner") val owner: User = null
    @OneToMany()
    @JoinColumn(name = "bucket") val buckets: Array[FileBucket] = null
}

@Entity
@Table(name = "tariffs")
case class Tariff() extends BaseEntity {
    @Column(name = "name") val name: String = ""
    @Column(name = "description") val description: String = ""
    @Column(name = "s3_path") val path: String = null
} 

@Entity
@Table(name = "user_sessions")
case class UserSession() extends BaseEntity {
    @Column(name = "token") var token: String = null
    @Column(name = "data") var data: String = null
}

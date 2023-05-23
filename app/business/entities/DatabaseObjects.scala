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
import jakarta.persistence.FetchType

@MappedSuperclass
class BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
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

    @OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "session") var session: UserSession = null
    @Column(name = "is_admin") var isAdmin: Boolean = false
    @Column(name = "avatar_url") var avatarUrl: String = ""
    @Column(name = "is_test_period_available") var isTestPeriodAvailable: Boolean = false
}

@Entity
@Table(name = "game_servers")
case class GameServer () extends BaseEntity {
    @ManyToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "owner")
    var owner: User = null

    @ManyToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "host") var host: Host = null

    @Column(name = "name") var name: String = ""
    @Column(name = "slug") var slug: String = ""
    @Column(name = "ip") var ip: String = ""
    @Column(name = "uuid") var uuid: String = ""
    @Column(name = "kind") var kind: String = ""
    @Column(name = "version") var version: String = ""

    @ManyToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "location") var location: Location = null
    @Column(name = "is_public") var isPublic: Boolean = false
    @Column(name = "is_active_vm") var isActiveVm: Boolean = false
    @Column(name = "is_active_server") var isActiveServer: Boolean = false

    @ManyToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "tariff") var tariff: Tariff = null

    @OneToMany(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @Column(name = "ports")
    var ports: Array[GameServerPort] = null
}

@Entity
@Table(name = "locations")
case class Location() extends BaseEntity {
    @Column(name = "name") var name: String = ""
    @Column(name = "description") var description: String = ""
    @Column(name = "test_ip") var testIp: String = ""
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
    @OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "storage")
    val storage: UserFileStorage = null

    @OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
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
    @OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "owner") val owner: User = null
    @OneToMany(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket") val buckets: Array[FileBucket] = null
}

@Entity
@Table(name = "games")
case class Game() extends BaseEntity {
    @Column(name = "name") val name: String = ""
    @Column(name = "description") val description: String = ""
    @Column(name = "icon_uri") val iconUri: String = ""
    @OneToMany(mappedBy = "game", cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    val tariffs: Array[Tariff] = Array.empty
}

@Entity
@Table(name = "tariffs")
case class Tariff() extends BaseEntity {
    @Column(name = "name") val name: String = ""
    @ManyToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY) val game: Game = null
    @Column(name = "specification_id") val specificationId: Long = 0
    @Column(name = "description") val description: String = ""
    @Column(name = "s3_path") val path: String = null
} 

@Entity
@Table(name = "user_sessions")
case class UserSession() extends BaseEntity {
    @Column(name = "token") var token: String = null
    @Column(name = "data") var data: String = null
}

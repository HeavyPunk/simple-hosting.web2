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
    MappedSuperclass,
    OneToOne,
    OneToMany,
    ManyToOne,
    JoinColumn,
    CascadeType,
    FetchType,
    SequenceGenerator
}
import org.hibernate.annotations.{ GenericGenerator }
import java.util.{
    Date,
    UUID,
}
import java.time.Instant
import business.services.oauth2.OAuth2System

@MappedSuperclass
class BaseEntity{
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GenericGenerator(name="kaugen" , strategy="increment")
    @GeneratedValue(generator="kaugen")
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
    @JoinColumn(name = "session", nullable = true) var session: UserSession = null
    @Column(name = "is_admin") var isAdmin: Boolean = false
    @Column(name = "avatar_url", nullable = true) var avatarUrl: String = ""
    @Column(name = "is_test_period_available") var isTestPeriodAvailable: Boolean = false

    @OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "oauth_user") var oauthUser: OAuthUser = null

    override def equals(other: Any): Boolean = other.isInstanceOf[User] && other.asInstanceOf[User].login.equals(this.login)
}

@Entity
@Table(name = "oauth_users")
case class OAuthUser() extends BaseEntity {
    @Column(name = "oauth_system") var oauthSystem: OAuth2System = OAuth2System.None
    @Column(name = "oauth_key") var oauthKey: String = ""
    @Column(name = "oauth_code") var oauthCode: String = ""
    @Column(name = "oauth_token") var oauthToken: String = ""

    @OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
    @JoinColumn(name = "user") var user: User = null
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

    override def equals(other: Any): Boolean = other.isInstanceOf[Location] && other.asInstanceOf[Location].name.equals(this.name)
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
    @OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.EAGER)
    val specification: TariffSpecification = null
    @Column(name = "description") val description: String = ""
    @Column(name = "s3_path") val path: String = null
} 

@Entity
@Table(name = "tariff_specifications")
case class TariffSpecification() extends BaseEntity {
    @Column(name = "month_price") var monthPrice: Int = 0
    @Column(name = "is_price_per_player") var isPricePerPlayer: Boolean = false
    @Column(name = "is_memory_per_slot") var isMemoryPerSlot: Boolean = false
    @Column(name = "is_cpu_per_slot") val isCpuPerSlot: Boolean = false
    @Column(name = "min_slots") val minSlots: Int = 0
    @Column(name = "max_slots") val maxSlots: Int = 0

    @Column(name = "image_uri") var imageUri: String = null
    @Column(name = "available_disk_bytes") var availableDiskBytes: Long = 0
    @Column(name = "available_ram_bytes") var availableRamBytes: Long = 0
    @Column(name = "available_swap_bytes") var availableSwapBytes: Long = 0
    @Column(name = "available_cpu") var availableCpu: Long = 0


    @OneToMany(mappedBy = "specification", cascade = Array(CascadeType.ALL), fetch = FetchType.EAGER)
    var vmExposePorts: Array[TariffSpecificationPort] = Array.empty
    @Column(name = "cpu_frequency") var cpuFrequency: Long = 0
    @Column(name = "cpu_name") var cpuName: String = null
}

@Entity
@Table(name = "tariff_specifications_ports")
case class TariffSpecificationPort() extends BaseEntity {
    @ManyToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.EAGER) var specification: TariffSpecification = null
    @Column(name = "port") var port: String = null
    @Column(name = "kind") var kind: String = null
}

@Entity
@Table(name = "user_sessions")
case class UserSession() extends BaseEntity {
    @Column(name = "token") var token: String = null
    @Column(name = "data") var data: String = null

    override def equals(other: Any): Boolean = other.isInstanceOf[UserSession] && other.asInstanceOf[UserSession].token.equals(this.token)
}

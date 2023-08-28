package business.services.storages.servers

import business.services.storages.BaseStorage
import business.entities.GameServer
import jakarta.persistence.EntityManager
import java.{util => ju}
import business.entities.User
import components.services.log.Log
import components.basic.Monad
import components.basic.ErrorMonad
import components.basic.ResultMonad

class GameServerNotFoundException extends Exception

class GameServerStorage(
    val em: EntityManager,
    val logger: Log
) extends BaseStorage[GameServer]:

    override val entityManager: EntityManager = em
    override val log = logger
    
    def findByHash(id: String): Monad[Exception | GameServerNotFoundException, GameServer] = 
        val result = query(
            "from GameServer where uuid=:id",
            ("id" -> id)
        )
        result match 
            case r: ErrorMonad[Exception, List[GameServer]] => ErrorMonad(r.err)
            case r: ResultMonad[Exception, List[GameServer]] => if r.obj.length == 0 then ErrorMonad(GameServerNotFoundException()) else ResultMonad(r.obj(0))

    def findGameServerById[TKey](id: TKey): Monad[Exception | GameServerNotFoundException, GameServer] = 
        val result = findById(id)
        result match
            case r: ErrorMonad[Exception, GameServer] => r
            case r: ResultMonad[Exception, GameServer] => if r.obj != null then r else ErrorMonad(GameServerNotFoundException())

    def findPublicServers(kind: String): Monad[Exception, List[GameServer]] =
        query(
            "from GameServer where kind=:kind and isPublic=true",
            ("kind" -> kind),
        )

    def findServersByOwner(owner: User): Monad[Exception, List[GameServer]] = 
        query(
            "from GameServer where owner=:owner",
            ("owner" -> owner)
        )

package business.services.storages.games

import scala.jdk.CollectionConverters._
import business.services.storages.BaseStorage
import business.entities.Game
import jakarta.persistence.EntityManager
import components.services.log.Log
import components.basic.Monad
import components.basic.ResultMonad
import components.basic.ErrorMonad

class GameNotFoundException extends Exception

class GamesStorage(em: EntityManager, logger: Log) extends BaseStorage[Game]:
    override val log: Log = logger

    override val entityManager: EntityManager = em
    
    def getAll: Monad[Exception, List[Game]] = query("from Game")

    def findGameById[TKey](id: TKey): Monad[Exception | GameNotFoundException, Game] = 
        val result = findById(id)
        result match
            case r: ErrorMonad[Exception, Game] => r
            case r: ResultMonad[Exception, Game] => if r.obj != null then r else ErrorMonad(GameNotFoundException())
    
    def addGame(game: Game): Monad[Exception, Boolean] = add(game)

    def updateGame(game: Game): Monad[Exception, Boolean] = update(game)
    
    def removeGame(game: Game): Monad[Exception, Boolean] = remove(game)
        
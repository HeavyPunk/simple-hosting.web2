package business.services.slickStorages.games

import business.entities.DatabaseObservator
import business.entities.ObjectObservator
import business.entities.newEntity.Game
import business.entities.slick.DatabaseGame
import business.entities.slick.GamesTable
import business.services.slickStorages.BaseStorage
import business.services.slickStorages.tariff.TariffStorage
import com.google.inject.Inject
import components.basic.ErrorMonad
import components.basic.Monad
import components.basic.ResultMonad
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep

import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import business.services.slickStorages.tariff.SlickTariffStorage

class GameNotFound

trait GamesStorage extends BaseStorage[Game, GamesTable, Exception, Exception, Exception, Exception]

class SlickGameStorage @Inject() (
    db: Database,
    operationTimeout: Duration,
) extends GamesStorage {

    val tariffsStorage: TariffStorage = SlickTariffStorage(db, operationTimeout, this) //TODO: Прикол для избавления от цикличной зависимости
    override def create(modifier: Game => Unit = null): Game = ???
    override def add(item: Game): Monad[Exception, Boolean] = {
        try {
            val gamesTable = TableQuery[GamesTable]
            val databaseGame = DatabaseGame(
                id = 0,
                creationDate = item.creationDate.toGMTString(),
                name = item.name,
                description = item.description,
                iconUri = item.iconUri
            )
            Await.result(db.run(gamesTable += databaseGame), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def update(item: Game): Monad[Exception, Boolean] = {
        try {
            val gamesTable = TableQuery[GamesTable]
            val databaseGame = DatabaseGame(
                id = item.id,
                creationDate = item.creationDate.toGMTString(),
                name = item.name,
                description = item.description,
                iconUri = item.iconUri
            )
            Await.result(db.run(gamesTable.update(databaseGame)), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def find(predicate: GamesTable => Rep[Boolean]): Monad[Exception, Seq[Game]] = {
        try {
            val gamesTable = TableQuery[GamesTable]
            val games = for {
                game <- Await.result(db.run(gamesTable.filter(predicate).result), operationTimeout)
            } yield Game(
                id = game.id,
                creationDate = Date(game.creationDate),
                name = game.name,
                description = game.description,
                iconUri = game.iconUri,
                tariffs = DatabaseObservator(() => tariffsStorage.find(tariff => tariff.gameId === game.id))
            )
            ResultMonad(games)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def remove(predicate: GamesTable => Rep[Boolean]): Monad[Exception, Boolean] = ???
}

extension (storage: GamesStorage)
    def findById(id: Long): Monad[Exception | GameNotFound, Game] =
        storage.find(g => g.id === id).flatMap(gs => if gs.isEmpty then ErrorMonad(GameNotFound()) else ResultMonad(gs.head))
    def getAll(): Monad[Exception, Seq[Game]] = storage.find(_.id =!= 0L)

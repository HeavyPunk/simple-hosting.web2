package business.services.slickStorages.games

import slick.jdbc.PostgresProfile.api._
import business.services.slickStorages.BaseStorage
import business.entities.slick.GamesTable
import scala.concurrent.duration.Duration
import business.entities.newEntity.Game
import components.basic.Monad
import slick.lifted.Rep

class GameNotFound

trait GamesStorage extends BaseStorage[Game, GamesTable, GameNotFound]

class SlickGameStorage(db: Database, operationTimeout: Duration) extends GamesStorage {
    override def create(modifier: Game => Unit = null): Game = ???
    override def add(item: Game): Monad[Exception, Boolean] = ???
    override def update(item: Game): Monad[Exception, Boolean] = ???
    override def find(predicate: GamesTable => Rep[Boolean]): Monad[Exception | GameNotFound, Seq[Game]] = ???
    override def remove(predicate: GamesTable => Rep[Boolean]): Monad[Exception, Boolean] = ???
}

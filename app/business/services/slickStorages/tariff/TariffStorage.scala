package business.services.slickStorages.tariff

import slick.jdbc.PostgresProfile.api._
import business.services.slickStorages.BaseStorage
import business.entities.newEntity.Tariff
import business.entities.slick.TariffsTable
import scala.concurrent.duration.Duration
import components.basic.Monad
import slick.lifted.Rep
import components.basic.ErrorMonad
import components.basic.ResultMonad

class TariffNotFound

trait TariffStorage extends BaseStorage[Tariff, TariffsTable, TariffNotFound]

class SlickTariffStorage(
    db: Database,
    operationTimeout: Duration,
) extends TariffStorage {
    override def create(modifier: Tariff => Unit = null): Tariff = ???
    override def add(item: Tariff): Monad[Exception, Boolean] = ???
    override def update(item: Tariff): Monad[Exception, Boolean] = ???
    override def find(predicate: TariffsTable => Rep[Boolean]): Monad[Exception | TariffNotFound, Seq[Tariff]] = ???
    override def remove(predicate: TariffsTable => Rep[Boolean]): Monad[Exception, Boolean] = ???
}

extension (storage: TariffStorage){
    def findById(id: Long): Monad[Exception | TariffNotFound, Tariff] =
        storage.find(t => t.id === id).flatMap(t => if t.length == 0 then ErrorMonad(TariffNotFound()) else ResultMonad(t.head))
}


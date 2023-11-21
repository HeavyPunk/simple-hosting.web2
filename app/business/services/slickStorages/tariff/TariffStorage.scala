package business.services.slickStorages.tariff

import business.entities.DatabaseObservator
import business.entities.newEntity.Tariff
import business.entities.newEntity.TariffSpecification
import business.entities.newEntity.TariffSpecificationPort
import business.entities.slick.DatabaseTariff
import business.entities.slick.TariffSpecificationPortsTable
import business.entities.slick.TariffSpecificationsTable
import business.entities.slick.TariffsTable
import business.services.slickStorages.BaseStorage
import business.services.slickStorages.games.GameNotFound
import business.services.slickStorages.games.GamesStorage
import business.services.slickStorages.games.{findById => findGameById}
import com.google.inject.Inject
import components.basic.ErrorMonad
import components.basic.Monad
import components.basic.ResultMonad
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Rep

import java.util.Date
import scala.annotation.targetName
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TariffNotFound

trait TariffStorage extends BaseStorage[Tariff, TariffsTable, Exception, GameNotFound | Exception, Exception, Exception]

class SlickTariffStorage @Inject() (
    db: Database,
    operationTimeout: Duration,
    gamesStorage: GamesStorage,
) extends TariffStorage {
    override def create(modifier: Tariff => Unit = null): Tariff = ???
    override def add(item: Tariff): Monad[Exception, Boolean] = {
        try {
            val tariffTable = TableQuery[TariffsTable]
            val databaseTariff = DatabaseTariff(
                id = 0,
                creationDate = item.creationDate.toGMTString(),
                name = item.name,
                gameId = item.game.get.tryGetValue._2.id,
                description = item.description,
            )
            Await.result(db.run(tariffTable += databaseTariff), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def update(item: Tariff): Monad[Exception | GameNotFound, Boolean] = {
        item.game.get
        .flatMap(game => {
            try {
                val tariffTable = TableQuery[TariffsTable]
                val databaseTariff = DatabaseTariff(
                    id = item.id,
                    creationDate = item.creationDate.toGMTString(),
                    name = item.name,
                    gameId = game.id,
                    description = item.description,
                )
                Await.result(db.run(tariffTable.update(databaseTariff)), operationTimeout)
                if item.game.initialized then gamesStorage.update(game)
                else ResultMonad(true)
            } catch {
                case e: Exception => ErrorMonad(e)
            }
        })
    }
    override def find(predicate: TariffsTable => Rep[Boolean]): Monad[Exception, Seq[Tariff]] = {
        try {
            val tariffsTable = TableQuery[TariffsTable]
            val action = tariffsTable filter(predicate)
            val tariffs = Await.result(db.run(action.result), operationTimeout)
            val result = tariffs map { tariff => Tariff(
                id = tariff.id,
                creationDate = Date(tariff.creationDate),
                name = tariff.name,
                description = tariff.description,
                game = DatabaseObservator(() => gamesStorage.findGameById(tariff.gameId)),
                specification = DatabaseObservator(() => {
                    try {
                        val specificationsTable = TableQuery[TariffSpecificationsTable]
                        val specifications = for {
                            specification <- Await.result(db.run(specificationsTable.filter(_.tariffId === tariff.id).result), operationTimeout)
                        } yield TariffSpecification(
                            id = specification.id,
                            creationDate = Date(specification.creationDate),
                            imageUri = specification.imageUri,
                            monthPrice = specification.monthPrice,
                            isPricePerPlayer = specification.isPricePerPlayer,
                            isMemoryPerSlot = specification.isMemoryPerSlot,
                            isCpuPerSlot = specification.isCpuPerSlot,
                            minSlots = specification.minSlots,
                            maxSlots = specification.maxSlots,
                            availableDiskBytes = specification.availableDiskBytes,
                            availableRamBytes = specification.availableRamBytes,
                            availableSwapBytes = specification.availableSwapBytes,
                            availableCpu = specification.availableCpu,
                            vmExposePorts = DatabaseObservator(() => {
                                try {
                                    val specPorts = TableQuery[TariffSpecificationPortsTable]
                                    val ports = for {
                                        port <- Await.result(db.run(specPorts.filter(_.specificationId === specification.id).result), operationTimeout)
                                    } yield TariffSpecificationPort(
                                        id = port.id,
                                        creationDate = Date(port.creationDate),
                                        port = port.port,
                                        kind = port.kind
                                    )
                                    ResultMonad(ports)
                                } catch {
                                    case e: Exception => ErrorMonad(e)
                                }
                            }),
                            cpuFrequency = specification.cpuFrequency,
                            cpuName = specification.cpuName
                        )
                        ResultMonad(specifications.head)
                    } catch {
                        case e: Exception => ErrorMonad(e)
                    }
                }),
            )}
            ResultMonad(result)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
    override def remove(predicate: TariffsTable => Rep[Boolean]): Monad[Exception, Boolean] = {
        val tariffs = TableQuery[TariffsTable]
        try {
            val removeServersAction = Await.result(db.run(tariffs.filter(predicate).delete), operationTimeout)
            ResultMonad(true)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }
}

extension (storage: TariffStorage){
    def findById(id: Long): Monad[Exception | TariffNotFound, Tariff] =
        storage.find(t => t.id === id).flatMap(t => if t.length == 0 then ErrorMonad(TariffNotFound()) else ResultMonad(t.head))
}


package business.services.storages.tariffs

import business.services.storages.BaseStorage
import business.entities.Tariff
import jakarta.persistence.EntityManager
import components.services.log.Log
import components.basic.Monad
import components.basic.ErrorMonad
import components.basic.ResultMonad

class TariffNotFoundException extends Exception

class TariffStorage (
    val em: EntityManager,
    val logger: Log
) extends BaseStorage[Tariff]:
    override val entityManager: EntityManager = em
    override val log = logger

    def findTariffById[TKey](id: TKey): Monad[Exception | TariffNotFoundException, Tariff] = 
        val result = findById(id)
        result match
            case r: ErrorMonad[Exception, Tariff] => r
            case r: ResultMonad[Exception, Tariff] => if r.obj != null then r else ErrorMonad(TariffNotFoundException())

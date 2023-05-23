package business.services.storages.tariffs

import business.services.storages.BaseStorage
import business.entities.Tariff
import jakarta.persistence.EntityManager
import components.services.log.Log

class TariffStorage (
    val em: EntityManager,
    val logger: Log
) extends BaseStorage[Tariff]{
    override val entityManager: EntityManager = em
    override val log = logger
}

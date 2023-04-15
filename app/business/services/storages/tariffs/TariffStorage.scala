package business.services.storages.tariffs

import business.services.storages.BaseStorage
import business.entities.Tariff
import jakarta.persistence.EntityManager

class TariffStorage (
    val em: EntityManager
) extends BaseStorage[Tariff]{
    override val entityManager: EntityManager = em
}

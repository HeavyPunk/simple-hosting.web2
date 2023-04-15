package business.services.storages.hosts

import business.services.storages.BaseStorage
import business.entities.Host
import jakarta.persistence.EntityManager

class HostStorage(
    val em: EntityManager,
) extends BaseStorage[Host] {

  override val entityManager: EntityManager = em
}

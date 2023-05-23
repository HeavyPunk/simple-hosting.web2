package business.services.storages.hosts

import business.services.storages.BaseStorage
import business.entities.Host
import jakarta.persistence.EntityManager
import components.services.log.Log

class HostStorage(
    val em: EntityManager,
    val logger: Log
) extends BaseStorage[Host] {

  override val entityManager: EntityManager = em
  override val log = logger
}

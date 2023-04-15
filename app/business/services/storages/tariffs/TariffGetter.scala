package business.services.storages.tariffs

import business.services.storages.BaseStorage
import jakarta.persistence.EntityManager
import business.entities.ServerTariff
import business.entities.ServerTariffHardware

trait TariffGetter extends BaseStorage[ServerTariff] {
    def findTariffByName(name: String): Option[ServerTariff]
    def findTariffById(id: Long): Option[ServerTariff]
}

class StupidTariffProvider(
    em: EntityManager,
) extends TariffGetter {
    val entityManager: EntityManager = em

    val tariffs = Map(
        0L -> ServerTariff(0, "base-minecraft-server", "",
            ServerTariffHardware(
                "kirieshki/simple-hosting-minecraft-vanilla:preview-15.04.23.1",
                8L * 1024 * 1024 * 1024 * 2,
                8L * 1024 * 1024 * 1024 * 2,
                0,
                Array(
                    "8989:8989/tcp",
                    "25565/tcp"
                )
            )
        )
    )

    override def findTariffById(id: Long): Option[ServerTariff] = Some(tariffs(id))

    override def findTariffByName(name: String): Option[ServerTariff] = tariffs find(t => t._2.name.equals(name)) map (t => t._2)
}

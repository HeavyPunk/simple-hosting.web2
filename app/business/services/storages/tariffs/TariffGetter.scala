package business.services.storages.tariffs

import business.services.storages.BaseStorage
import jakarta.persistence.EntityManager
import business.entities.ServerTariff
import business.entities.ServerTariffHardware
import components.services.log.Log

trait TariffGetter extends BaseStorage[ServerTariff] {
    def findTariffByName(name: String): Option[ServerTariff]
    def findTariffById(id: Long): Option[ServerTariff]
}

class StupidTariffProvider(
    em: EntityManager,
    logger: Log
) extends TariffGetter {
    val entityManager: EntityManager = em
    val log = logger

    val tariffs = Map(
        0L -> ServerTariff(0, "vanilla-minecraft-1.19.3", "",
            ServerTariffHardware(
                "kirieshki/simple-hosting-minecraft-vanilla:preview-15.04.23.1",
                8L * 1024 * 1024 * 1024 * 2,
                8L * 1024 * 1024 * 1024 * 2,
                0,
                100, //%
                Array(
                    "8989/tcp",
                    "25565/tcp"
                ),
                3_000, //MHz
                "default"
            ),
            100, //руб
            false,
            false,
            false
        ),
        1L -> ServerTariff(0, "standard-vanilla-minecraft-server-1.19.3", "",
            ServerTariffHardware(
                "kirieshki/simple-hosting-minecraft-vanilla:preview-15.04.23.1",
                4L * 1024 * 1024 * 1024,
                8L * 1024 * 1024 * 1024,
                0,
                20, //%
                Array(
                    "8989/tcp",
                    "25565/tcp"
                ),
                3_000, //MHz
                "default"
            ),
            50, //руб
            false,
            false,
            false
        ),
        2L -> ServerTariff(0, "low-cs1.6", "",
            ServerTariffHardware(
                "kirieshki/cs1.6:preview-23.05.21.1",
                2L * 1024 * 1024 * 1024,
                8L * 1024 * 1024 * 1024,
                0,
                20, //%
                Array(
                    "8989/tcp",
                    "25565/tcp"
                ),
                3_000, //MHz
                "default"
            ),
            50, //руб
            false,
            false,
            false
        ),
        3L -> ServerTariff(0, "standard-cs1.6", "",
            ServerTariffHardware(
                "kirieshki/cs1.6:preview-23.05.21.1",
                4L * 1024 * 1024 * 1024,
                8L * 1024 * 1024 * 1024,
                0,
                50, //%
                Array(
                    "8989/tcp",
                    "25565/tcp"
                ),
                3_000, //MHz
                "default"
            ),
            100, //руб
            false,
            false,
            false
        ),
        4L -> ServerTariff(0, "low-garrys-mod", "",
            ServerTariffHardware(
                "kirieshki/garrys-mod:preview-23.05.21.1",
                2L * 1024 * 1024 * 1024,
                8L * 1024 * 1024 * 1024,
                0,
                20, //%
                Array(
                    "8989/tcp",
                    "25565/tcp"
                ),
                3_000, //MHz
                "default"
            ),
            50, //руб
            false,
            false,
            false
        ),
        5L -> ServerTariff(0, "low-garrys-mod", "",
            ServerTariffHardware(
                "kirieshki/garrys-mod:preview-23.05.21.1",
                4L * 1024 * 1024 * 1024,
                8L * 1024 * 1024 * 1024,
                0,
                50, //%
                Array(
                    "8989/tcp",
                    "25565/tcp"
                ),
                3_000, //MHz
                "default"
            ),
            100, //руб
            false,
            false,
            false
        ),
    )

    override def findTariffById(id: Long): Option[ServerTariff] = Some(tariffs(id))

    override def findTariffByName(name: String): Option[ServerTariff] = tariffs find(t => t._2.name.equals(name)) map (t => t._2)
}

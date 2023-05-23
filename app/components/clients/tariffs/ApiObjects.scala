package components.clients.tariffs

final case class GameTariff(
    val id: Long,
    val gameId: Long,
    val name: String,
    val description: String,
    val minSlots: Int,
    val maxSlots: Int,
    val monthPrice: Int,
    val isPricePerPlayer: Boolean,
    val allocatedCpu: Long,
    val allocatedDiskSpace: Long,
    val allocatedMemory: Long,
    val cpuFrequency: Long,
    val cpuName: String,
    val isMemoryPerSlot: Boolean,
    val isCpuPerSlot: Boolean,
    val locations: Array[Location]
)

final case class Location(
    val id: Long,
    val name: String,
    val description: String,
    val testIp: String,
)

final case class GameInfoResponse(
    val gameId: Long,
    val gameName: String,
    val gameImageUrl: String,
    val gameDescription: String,
    val tariffs: Array[GameTariff]
)

final case class GetAllGamesResponse(
    val games: Seq[GameInfoResponse]
)

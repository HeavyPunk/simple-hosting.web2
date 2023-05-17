package business.entities

case class ServerTariffHardware(
    val imageUri: String,
    val availableRamBytes: Long,
    val availableDiskBytes: Long,
    val availableSwapBytes: Long,
    val availableCpu: Long,
    val vmExposePorts: Array[String],
    val cpuFrequency: Long,
    val cpuName: String,
)

case class ServerTariff(
    val id: Long,
    val name: String,
    val description: String,
    val hadrware: ServerTariffHardware,
    val monthPrice: Int,
    val isPricePerPlayer: Boolean,
    val isMemoryPerSlot: Boolean,
    val isCpuPerSlot: Boolean,
)

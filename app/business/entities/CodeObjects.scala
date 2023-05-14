package business.entities

case class ServerTariffHardware(
    val imageUri: String,
    val availableRamBytes: Long,
    val availableDiskBytes: Long,
    val availableSwapBytes: Long,
    val vmExposePorts: Array[String]
)

case class ServerTariff(
    val id: Long,
    val name: String,
    val description: String,
    val hadrware: ServerTariffHardware,
)

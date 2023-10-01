package components.clients.compositor

class CreateContainerRequest(
    val vmImageUri: String,
    val vmName: String,
    val vmAvailableRamBytes: Long,
    val vmAvailableDiskBytes: Long,
    val vmAvailableSwapBytes: Long,
    val vmExposePorts: Array[String]
)

class CreateContainerResponse(
    val vmId: String
)

class StartContainerResponse(
    val vmId: String,
    val vmWhiteIp: String,
    val vmWhitePorts: Array[String]
)

class RemoveContainerResponse(
    val error: String,
    val success: Boolean
)

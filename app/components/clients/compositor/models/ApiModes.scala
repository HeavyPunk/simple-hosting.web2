package components.clients.compositor.models

import com.fasterxml.jackson.annotation.JsonProperty

class CreateServerRequest (
    @JsonProperty("image-uri") val vmImageUri: String,
    @JsonProperty("name") val vmName: String,
    @JsonProperty("ram") val vmAvailableRamBytes: Long,
    @JsonProperty("disk") val vmAvailableDiskBytes: Long,
    @JsonProperty("swap") val vmAvailableSwapBytes: Long,
    @JsonProperty("ports") val vmExposePorts: Array[String]
)

class CreateServerResponse (
    @JsonProperty("vm-id") val vmId: String,
)

class StartServerRequest (
    @JsonProperty("vm-id") val vmId: String
)

class StartServerResponse (
    @JsonProperty("vm-id") val vmId: String,
    @JsonProperty("ip") val vmWhiteIp: String,
    @JsonProperty("ports") val vmWhitePorts: Array[String],
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String,
)

class StopServerRequest (
    @JsonProperty("vm-id") val vmId: String
)
class StopServerResponse (
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)

class RemoveServerRequest (
    @JsonProperty("vm-id") val vmId: String
)

class RemoveServerResponse (
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)



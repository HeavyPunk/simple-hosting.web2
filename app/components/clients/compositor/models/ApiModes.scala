package components.clients.compositor.models

import com.fasterxml.jackson.annotation.JsonProperty

class CreateServerRequest (
    @JsonProperty("name") val vmName: String,
    @JsonProperty("version") val version: String,
    @JsonProperty("tariffId") val tariffId: String,
    @JsonProperty("rent-time") val rentTime: String,
    @JsonProperty("slots-count") val slotsCount: Int,
)

class CreateServerResponse (
    @JsonProperty("game-server-id") val gameServerId: String,
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String,
)


class StartServerRequest (
    @JsonProperty("game-server-id") val gameServerId: String,
)

class StartServerResponse (
    @JsonProperty("game-server-id") val gameServerId: String,
    @JsonProperty("ip") val vmWhiteIp: String,
    @JsonProperty("ports") val vmWhitePorts: Array[PortDescription],
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String,
)

class PortDescription (
    @JsonProperty("port-kind") val portKind: String,
    @JsonProperty("port") val ports: String,
)

class StopServerRequest (
    @JsonProperty("game-server-id") val gameServerId: String,
)
class StopServerResponse (
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)

class RemoveServerRequest (
    @JsonProperty("game-server-id") val gameServerId: String,
)

class RemoveServerResponse (
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)



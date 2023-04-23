package components.clients.compositor.models

import com.fasterxml.jackson.annotation.JsonProperty
import business.entities.GameServerPort

case class CreateServerRequest (
    @JsonProperty("name") val vmName: String,
    @JsonProperty("version") val version: String,
    @JsonProperty("tariffId") val tariffId: String,
    @JsonProperty("rent-time") val rentTime: String,
    @JsonProperty("slots-count") val slotsCount: Int,
)

case class CreateServerResponse (
    @JsonProperty("game-server-hash") val gameServerHash: String,
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String,
)


case class StartServerRequest (
    @JsonProperty("game-server-hash") val gameServerHash: String,
)

case class StartServerResponse (
    @JsonProperty("game-server-hash") val gameServerHash: String,
    @JsonProperty("ip") val vmWhiteIp: String,
    @JsonProperty("ports") val vmWhitePorts: Array[PortDescription],
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String,
)

case class PortDescription (
    @JsonProperty("port-kind") val portKind: String,
    @JsonProperty("port") val ports: String,
)

case class StopServerRequest (
    @JsonProperty("game-server-hash") val gameServerHash: String,
)
case class StopServerResponse (
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)

case class RemoveServerRequest (
    @JsonProperty("game-server-hash") val gameServerHash: String,
)

case class RemoveServerResponse (
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)

case class GetServersList (
    @JsonProperty("servers") val servers: Seq[ServerInfo]
)

case class ServerInfo(
    @JsonProperty("server-hash") val serverHash: String,
    @JsonProperty("server-name") val serverName: String,
    @JsonProperty("game-kind") val gameKind: String,
    @JsonProperty("server-ip") val serverIp: String,
    @JsonProperty("server-ports") val serverPorts: Array[GameServerPort],
)

case class GetServersListRequest (
    @JsonProperty("kind") val kind: String,
    @JsonProperty("is-public") val isPublic: Boolean
)

case class UpdateServerRequest (
    @JsonProperty("server-hash") val serverHash: String,
    @JsonProperty("is-public") val isPublic: Boolean
)



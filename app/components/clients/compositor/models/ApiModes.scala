package components.clients.compositor.models

import com.fasterxml.jackson.annotation.JsonProperty
import business.entities.GameServerPort

case class CreateServerRequest (
    @JsonProperty("name") val vmName: String,
    val version: String,
    val tariffId: String,
    val rentTime: String,
    val slotsCount: Int,
)

case class CreateServerResponse (
    val gameServerHash: String,
    val success: Boolean,
    val error: String,
)


case class StartServerRequest (
    val gameServerHash: String,
)

case class StartServerResponse (
    val gameServerHash: String,
    @JsonProperty("ip") val vmWhiteIp: String,
    @JsonProperty("ports") val vmWhitePorts: Array[PortDescription],
    val success: Boolean,
    val error: String,
)

case class PortDescription (
    val portKind: String,
    val ports: String,
)

case class StopServerRequest (
    val gameServerHash: String,
)
case class StopServerResponse (
    val success: Boolean,
    val error: String
)

case class RemoveServerRequest (
    val gameServerHash: String,
)

case class RemoveServerResponse (
    val success: Boolean,
    val error: String
)

case class GetServersList (
    val servers: Seq[ServerInfo]
)

case class ServerInfo(
    val serverHash: String,
    val serverName: String,
    val gameKind: String,
    val serverIp: String,
    val serverPorts: Array[GameServerPort],
)

case class GetServersListRequest (
    val kind: String,
    val isPublic: Boolean
)

case class UpdateServerRequest (
    val serverHash: String,
    val isPublic: Boolean
)



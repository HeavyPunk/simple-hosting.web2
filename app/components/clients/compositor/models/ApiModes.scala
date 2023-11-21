package components.clients.compositor.models

import com.fasterxml.jackson.annotation.JsonProperty
import business.entities.newEntity.GameServerPort

case class StopVmWithGameServerRequest (
    val gameServerHash: String,
    val force: Boolean,
)
case class StopVmWithGameServerResponse (
    val success: Boolean,
    val error: String
)

case class CreateVmWithGameServerRequest(
    @JsonProperty("name") val vmName: String,
    val gameId: Long,
    val locationId: Long,
    val tariffId: String,
    val period: Long,
    val isTestPeriod: Boolean,
    val promoCode: String,
    val slots: Int,
    val saveStdout: Boolean,
    val saveStderr: Boolean
)

case class CreateVmWithGameServerResponse(
    val gameServerHash: String,
    val success: Boolean,
    val error: String
)

case class CreateServerRequest (
    @JsonProperty("name") val vmName: String,
    val gameId: Long,
    val locationId: Long,
    val tariffId: String,
    val period: Long,
    val isTestPeriod: Boolean,
    val promoCode: String,
    val slots: Int,
)

case class CreateServerResponse (
    val gameServerHash: String,
    val success: Boolean,
    val error: String,
)


case class StartVmRequest (
    val gameServerHash: String,
)

case class StartServerResponse (
    val gameServerHash: String,
    @JsonProperty("serverIp") val vmWhiteIp: String,
    @JsonProperty("serverPorts") val vmWhitePorts: Array[PortDescription],
    val success: Boolean,
    val error: String,
)

case class PortDescription (
    val id: String,
    val portKind: String,
    val port: String,
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
    val gameServerHash: String,
    val gameServerName: String,
    val gameKind: String,
    val serverIp: String,
    val serverPorts: Array[GameServerPort],
    val isOnline: Boolean
)

case class GetUserServersRequest (
    val kind: String,
    val isPublic: Boolean
)

case class UpdateServerRequest (
    val gameServerHash: String,
    val isPublic: Boolean
)


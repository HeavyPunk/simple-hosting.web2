package components.services.business.servers

import java.util.UUID
import business.entities.newEntity.{
    User,
    Location,
    Game,
    Tariff
}
import components.clients.compositor.CreateContainerResponse

case class CreateVmRequest(
    val tariff: Tariff,
    val vmName: String,
    val vmSlug: UUID,
    val user: User,
    val game: Game,
    val location: Location
)
case class StartVmRequest(
    val user: User,
    val gameServerHash: String,
)
case class StopVmRequest(
    val user: User,
    val gameServerHash: String
)
case class UpdateVmRequest(
    val user: User,
    val gameServerHash: String,
    val isPublic: Boolean,
)
case class RemoveVmRequest(
    val user: User,
    val gameServerHash: String,
)
case class StartGameServerRequest(
    val user: User,
    val gameServerHash: String,
    val saveStdout: Boolean,
    val saveStderr: Boolean,
)
case class StopGameServerRequest(
    val user: User,
    val gameServerHash: String,
    val force: Boolean,
)
case class RestartVmRequest(
    val user: User,
    val gameServerHash: String,
)

case class CreateVmResponse(
    val container: CreateContainerResponse
)
case class StartVmResponse()
case class StopVmResponse(
    val success: Boolean,
    val error: String,
)
case class UpdateVmResponse()
case class RemoveVmResponse()
case class StartGameServerResponse(
    val success: Boolean,
    val error: String,
)
case class StopGameServerResponse(
    val success: Boolean,
    val error: String,
)
case class RestartVmResponse()
package components.services.business.servers

import components.clients.compositor.{
    ContainerNotCreated,
    ContainerNotRemoved
}
import business.services.storages.servers.{
    GameServerNotFoundException,
    GameServerStorage
}
import controllers.v2.servers.{
    ErrorWhenStartingServer,
    ErrorWhenStoppingServer
}
import com.google.inject.Inject
import components.clients.compositor.{
    CompositorClientWrapper,
    CreateContainerRequest
}
import business.entities.GameServer
import components.basic.{ 
    zipWith,
    mapToMonad,
    Monad,
    ResultMonad
}
import components.services.retrier.Retrier
import components.clients.controller.ControllerUtils
import scala.collection.immutable.ArraySeq
import components.clients.controller.ControllerClientFactory
import io.github.heavypunk.controller.client.Settings
import java.time.Duration
import components.services.log.Log
import business.entities.GameServerPort
import components.clients.compositor.models.StartServerResponse
import components.clients.compositor.models.PortDescription
import io.github.heavypunk.controller.client.contracts.server.StartServerRequest
import components.basic.ErrorMonad
import io.github.heavypunk.controller.client.contracts.server.StopServerRequest

class VmNotFound

trait ServersManagementService:
    def createVm(request: CreateVmRequest): Monad[ContainerNotCreated | Exception, CreateVmResponse]
    def startVm(request: StartVmRequest): Monad[VmNotFound| Exception, StartVmResponse]
    def stopVm(request: StopVmRequest): Monad[VmNotFound | Exception, StopVmResponse]
    def updateVm(request: UpdateVmRequest): Monad[VmNotFound | Exception, UpdateVmResponse]
    def removeVm(request: RemoveVmRequest): Monad[VmNotFound | ContainerNotRemoved | Exception, RemoveVmResponse]
    def restartVm(request: RestartVmRequest): Monad[VmNotFound | Exception, RestartVmResponse]
    def startGameServer(request: StartGameServerRequest): Monad[VmNotFound | ErrorWhenStartingServer | Exception, StartGameServerResponse]
    def stopGameServer(request: StopGameServerRequest): Monad[VmNotFound | ErrorWhenStoppingServer | Exception, StopGameServerResponse]

class CommonServersManagementService @Inject() (
    val compositorWrapper: CompositorClientWrapper,
    val gameServerStorage: GameServerStorage,
    val controllerClientFactory: ControllerClientFactory,
    val controllerClientSettings: Settings,
    val log: Log
) extends ServersManagementService {

    override def createVm(request: CreateVmRequest): Monad[ContainerNotCreated | Exception, CreateVmResponse] = {
        val container = compositorWrapper.createContainerMonad(CreateContainerRequest(
            request.tariff.specification.imageUri,
            request.vmSlug.toString,
            request.tariff.specification.availableRamBytes,
            request.tariff.specification.availableDiskBytes,
            request.tariff.specification.availableSwapBytes,
            request.tariff.specification.vmExposePorts.map(_.port)
        ))
        val result = container
            .flatMap(c => {
                val server = GameServer()
                server.name = request.vmName
                server.slug = request.vmSlug.toString
                server.tariff = request.tariff
                server.owner = request.user
                server.uuid = c.vmId
                server.kind = request.game.name
                server.location = request.location
                gameServerStorage.add(server)
            })
            .zipWith(container)
            .flatMap((_, c) => ResultMonad(CreateVmResponse(c)))
        result
    }

    override def updateVm(request: UpdateVmRequest): Monad[VmNotFound | Exception, UpdateVmResponse] = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid.equals(request.gameServerHash)} mapToMonad(VmNotFound()))
        val result = server.flatMap(s => {
            s.isPublic = request.isPublic
            gameServerStorage.update(s)
        })
        .flatMap(r => ResultMonad(UpdateVmResponse()))
        result
    }

    override def startVm(request: StartVmRequest): Monad[VmNotFound | Exception, StartVmResponse] = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid.equals(request.gameServerHash)} mapToMonad(VmNotFound()))
        val startContainerReq = server.flatMap(s => compositorWrapper.startContainerMonad(s.uuid))
        val ports = startContainerReq
            .zipWith(server)
            .flatMap((r, s) => {
                val searchRes = Retrier.work(
                    action = {
                        ControllerUtils.findControllerPort(
                            ArraySeq.unsafeWrapArray(r.vmWhitePorts),
                            controllerClientFactory,
                            Settings(controllerClientSettings.scheme, r.vmWhiteIp, controllerClientSettings.port)
                        )
                    },
                    isSuccess = p => p.isDefined,
                    delay = Duration.ofSeconds(2),
                    onAttempt = a => log.info(s"Attempting to connect to controller of server ${s.slug}, attempt $a")
                )
                val controllerPort = if (searchRes.isDefined) searchRes.get.get else -1
                val serializedControllerPort = GameServerPort()
                serializedControllerPort.port = controllerPort
                serializedControllerPort.portKind = "controller"
                val ports = Array(serializedControllerPort) ++ (r.vmWhitePorts map { p => {
                    val port = GameServerPort()
                    port.portKind = "none"
                    port.port = p.toInt
                    port
                }})
                ResultMonad(ports)
            })
        val serverUpdated = startContainerReq.zipWith(ports, server)
            .flatMap((container, ports, server) => {
                server.ip = container.vmWhiteIp
                server.ports = ports
                server.isActiveVm = true
                gameServerStorage.update(server)
            })
        val result = startContainerReq.zipWith(ports, serverUpdated)
            .flatMap((container, ports, _) => {
                ResultMonad(StartServerResponse(
                    container.vmId,
                    container.vmWhiteIp,
                    ports map { p => PortDescription(s"${p.portKind}-${p.port}", p.portKind, p.port.toString)},
                    true,
                    ""
                ))
            })
        result.flatMap(r => ResultMonad(StartVmResponse()))
    }

    override def removeVm(request: RemoveVmRequest): Monad[VmNotFound | ContainerNotRemoved | Exception, RemoveVmResponse] = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid == request.gameServerHash} mapToMonad(VmNotFound()))
        val removedResponse = server.flatMap(s => compositorWrapper.removeContainerMonad(s.uuid))
        val result = server.zipWith(removedResponse)
            .flatMap((server, _) => gameServerStorage.remove(server))
        result.flatMap(_ => ResultMonad(RemoveVmResponse()))
    }

    override def restartVm(request: RestartVmRequest): Monad[VmNotFound | Exception, RestartVmResponse] = {
        val stoppedVm = stopVm(StopVmRequest(request.user, request.gameServerHash))
        val startedVm = stoppedVm.flatMap(_ => startVm(StartVmRequest(request.user, request.gameServerHash)))
        startedVm.flatMap(_ => ResultMonad(RestartVmResponse()))
    }

    override def startGameServer(request: StartGameServerRequest): Monad[VmNotFound | ErrorWhenStartingServer | Exception, StartGameServerResponse] = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid == request.gameServerHash} mapToMonad(VmNotFound()))
        val controllerClient = server.flatMap(s =>
            ResultMonad(controllerClientFactory.getControllerClient(Settings(
                controllerClientSettings.scheme,
                s.ip,
                s.ports.find(_.portKind.equalsIgnoreCase("controller")).getOrElse(GameServerPort()).port
            )))
        )
        val result = controllerClient.flatMap(cc => {
            val response = cc.servers.startServer(StartServerRequest(
                request.saveStdout,
                request.saveStderr
            ), Duration.ofMinutes(2))
            if (response.success) ResultMonad(StartGameServerResponse(response.success, response.error))
            else ErrorMonad(ErrorWhenStartingServer(response))
        })
        result
    }

    override def stopGameServer(request: StopGameServerRequest): Monad[VmNotFound| ErrorWhenStoppingServer | Exception, StopGameServerResponse] = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid == request.gameServerHash} mapToMonad(VmNotFound()))
        val controllerClient = server.flatMap(s =>
            ResultMonad(controllerClientFactory.getControllerClient(Settings(
                controllerClientSettings.scheme,
                s.ip,
                s.ports.find(_.portKind.equalsIgnoreCase("controller")).getOrElse(GameServerPort()).port
            )))
        )
        val result = controllerClient.flatMap(cc => {
            val response = cc.servers.stopServer(StopServerRequest(request.force), Duration.ofMinutes(2))
            if (response.success) ResultMonad(StopGameServerResponse(response.success, response.error))
            else ErrorMonad(ErrorWhenStoppingServer(response))
        })
        result
    }

    override def stopVm(request: StopVmRequest): Monad[VmNotFound | Exception, StopVmResponse] = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid.equals(request.gameServerHash)} mapToMonad(VmNotFound()))
        val stopServerResponse = server.flatMap(s => compositorWrapper.stopContainerMonad(s.uuid))
        val result = stopServerResponse.zipWith(server)
            .flatMap((_, server) => {
                server.isActiveVm = false
                gameServerStorage.update(server)
            })
            .zipWith(stopServerResponse)
            .flatMap((_, r) => ResultMonad(StopVmResponse(r, "")))
        result
    }
}

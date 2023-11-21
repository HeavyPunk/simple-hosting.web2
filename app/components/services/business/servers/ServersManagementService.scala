package components.services.business.servers

import components.clients.compositor.{
    ContainerNotCreated,
    ContainerNotRemoved
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
import components.basic.{ 
    zipWith,
    mapToMonad,
    enrichWith,
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
import components.clients.compositor.models.StartServerResponse
import components.clients.compositor.models.PortDescription
import io.github.heavypunk.controller.client.contracts.server.StartServerRequest
import components.basic.ErrorMonad
import io.github.heavypunk.controller.client.contracts.server.StopServerRequest
import business.services.slickStorages.servers.{
    GameServersStorage,
    findServersByOwner,
    removeById,
}
import business.entities.newEntity.GameServer
import java.util.Date
import java.time.Instant
import business.services.slickStorages.locations.LocationNotFound
import business.services.slickStorages.tariff.TariffNotFound
import business.services.slickStorages.user.UserNotFound
import business.entities.ObjectObservator
import business.entities.DatabaseObservator
import business.entities.newEntity.GameServerPort

class VmNotFound

trait ServersManagementService:
    def createVm(request: CreateVmRequest): Monad[LocationNotFound | TariffNotFound | UserNotFound | ContainerNotCreated | Exception, CreateVmResponse]
    def startVm(request: StartVmRequest): Monad[LocationNotFound | TariffNotFound | UserNotFound | VmNotFound| Exception, StartVmResponse]
    def stopVm(request: StopVmRequest): Monad[LocationNotFound | TariffNotFound | UserNotFound | VmNotFound | Exception, StopVmResponse]
    def updateVm(request: UpdateVmRequest): Monad[LocationNotFound | TariffNotFound | UserNotFound | VmNotFound | Exception, UpdateVmResponse]
    def removeVm(request: RemoveVmRequest): Monad[LocationNotFound | TariffNotFound | UserNotFound | VmNotFound | ContainerNotRemoved | Exception, RemoveVmResponse]
    def restartVm(request: RestartVmRequest): Monad[LocationNotFound | TariffNotFound | UserNotFound | VmNotFound | Exception, RestartVmResponse]
    def startGameServer(request: StartGameServerRequest): Monad[VmNotFound | ErrorWhenStartingServer | Exception, StartGameServerResponse]
    def stopGameServer(request: StopGameServerRequest): Monad[VmNotFound | ErrorWhenStoppingServer | Exception, StopGameServerResponse]

class CommonServersManagementService @Inject() (
    val compositorWrapper: CompositorClientWrapper,
    val gameServerStorage: GameServersStorage,
    val controllerClientFactory: ControllerClientFactory,
    val controllerClientSettings: Settings,
    val log: Log
) extends ServersManagementService {

    override def createVm(request: CreateVmRequest) = {
        val req = request.tariff.specification.get.enrichWith(spec => spec.vmExposePorts.get)
            .flatMap((specification, specPorts) => ResultMonad(CreateContainerRequest(
                specification.imageUri,
                request.vmSlug.toString,
                specification.availableRamBytes,
                specification.availableDiskBytes,
                specification.availableSwapBytes,
                specPorts.map(_.port).toArray
            )))
        val container = req.flatMap(req => compositorWrapper.createContainerMonad(req))
        val result = container.zipWith(req)
            .flatMap((container, req) => {
                val server = GameServer(
                    id = 0,
                    creationDate = Date.from(Instant.now()),
                    owner = ObjectObservator(request.user),
                    name = request.vmName,
                    slug = request.vmSlug.toString(),
                    ip = "",
                    uuid = container.vmId,
                    kind = request.game.name,
                    version = "",
                    location = ObjectObservator(request.location),
                    isPublic = false,
                    isActiveVm = false,
                    isActiveServer = false,
                    tariff = ObjectObservator(request.tariff),
                    ports = DatabaseObservator(() => request.tariff.specification.get
                        .flatMap(s => s.vmExposePorts.get)
                        .flatMap(ps => ResultMonad(ps map { p => GameServerPort(
                            id = p.id,
                            creationDate = p.creationDate,
                            port = Integer.parseInt(p.port.split("/")(0)),
                            portKind = p.kind
                        )}))
                    ),
                )
                gameServerStorage.add(server)
            })
            .zipWith(container)
            .flatMap((_, c) => ResultMonad(CreateVmResponse(c)))
        result
    }

    override def updateVm(request: UpdateVmRequest) = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid.equals(request.gameServerHash)} mapToMonad(VmNotFound()))
        val result = server.flatMap(s => {
            s.isPublic = request.isPublic
            gameServerStorage.update(s)
        })
        .flatMap(r => ResultMonad(UpdateVmResponse()))
        result
    }

    override def startVm(request: StartVmRequest) = {
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
                val serializedControllerPort = GameServerPort(
                    id = 0,
                    creationDate = Date(),
                    port = controllerPort,
                    portKind = "controller"
                )
                val ports = Array(serializedControllerPort) ++ (r.vmWhitePorts map { p => {
                    val port = GameServerPort(
                        id = 0,
                        creationDate = Date(),
                        portKind = "none",
                        port = p.toInt
                    )
                    port
                }})
                ResultMonad(ports)
            })
        val serverUpdated = startContainerReq.zipWith(ports, server)
            .flatMap((container, ports, server) => {
                server.ip = container.vmWhiteIp
                server.ports = ObjectObservator(ports.toSeq)
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

    override def removeVm(request: RemoveVmRequest) = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid == request.gameServerHash} mapToMonad(VmNotFound()))
        val removedResponse = server.flatMap(s => compositorWrapper.removeContainerMonad(s.uuid))
        val result = server.zipWith(removedResponse)
            .flatMap((server, _) => gameServerStorage.removeById(server.id))
        result.flatMap(_ => ResultMonad(RemoveVmResponse()))
    }

    override def restartVm(request: RestartVmRequest) = {
        val stoppedVm = stopVm(StopVmRequest(request.user, request.gameServerHash))
        val startedVm = stoppedVm.flatMap(_ => startVm(StartVmRequest(request.user, request.gameServerHash)))
        startedVm.flatMap(_ => ResultMonad(RestartVmResponse()))
    }

    override def startGameServer(request: StartGameServerRequest) = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid == request.gameServerHash} mapToMonad(VmNotFound()))
        val controllerClient = server.enrichWith(s => s.ports.get)
        .flatMap((s, ports) =>
            ResultMonad(controllerClientFactory.getControllerClient(Settings(
                controllerClientSettings.scheme,
                s.ip,
                ports.find(_.portKind.equalsIgnoreCase("controller")) match
                    case None => 0
                    case Some(p) => p.port
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

    override def stopGameServer(request: StopGameServerRequest) = {
        val servers = gameServerStorage.findServersByOwner(request.user)
        val server = servers.flatMap(s => s find {i => i.uuid == request.gameServerHash} mapToMonad(VmNotFound()))
        val controllerClient = server.enrichWith(s => s.ports.get)
        .flatMap((s, ports) =>
            ResultMonad(controllerClientFactory.getControllerClient(Settings(
                controllerClientSettings.scheme,
                s.ip,
                ports.find(_.portKind.equalsIgnoreCase("controller")) match
                    case None => 0
                    case Some(p) => p.port
            )))
        )
        val result = controllerClient.flatMap(cc => {
            val response = cc.servers.stopServer(StopServerRequest(request.force), Duration.ofMinutes(2))
            if (response.success) ResultMonad(StopGameServerResponse(response.success, response.error))
            else ErrorMonad(ErrorWhenStoppingServer(response))
        })
        result
    }

    override def stopVm(request: StopVmRequest) = {
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

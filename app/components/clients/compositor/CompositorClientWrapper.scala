package components.clients.compositor

import com.google.inject.Inject
import io.github.heavypunk.compositor.client.CompositorClient
import components.clients.compositor.models.CreateServerRequest
import io.github.heavypunk.compositor
import java.time.Duration
import io.github.heavypunk.compositor.client.models.StartServerRequest
import io.github.heavypunk.compositor.client.models.StopServerRequest
import components.basic.Monad
import io.github.heavypunk.compositor.client.models.VmUnit
import components.basic.ErrorMonad
import components.basic.ResultMonad
import components.clients.compositor.models.RemoveServerRequest

class ContainerNotFound
class ContainerNotCreated
class ContainerNotRemoved(val error: String)

class CompositorClientWrapper @Inject() (
    val client: CompositorClient,
    val defaultTimeout: Duration = Duration.ofMinutes(2)
) {
    def stopContainerMonad(containerId: String): Monad[Exception, Boolean] = 
        try {
            ResultMonad(stopContainer(containerId))
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    def startContainerMonad(containerId: String): Monad[Exception, StartContainerResponse] =
        try {
            ResultMonad(startContainer(containerId))
        } catch {
            case e: Exception => ErrorMonad(e)
        }

    def findContainerBySlugMonad(vmSlug: String): Monad[Exception | ContainerNotFound, VmUnit] =
        try {
            val container = findContainerBySlug(vmSlug)
            container match {
                case None => ErrorMonad(ContainerNotFound())
                case c: Some[?] => ResultMonad(c.get)
            }
        } catch {
            case e: Exception => ErrorMonad(e)
        }

    def createContainerMonad(request: CreateContainerRequest): Monad[Exception | ContainerNotCreated, CreateContainerResponse] =
        try {
            val resp = createContainer(request)
            if (resp.vmId == null || resp.vmId == "") ErrorMonad(ContainerNotCreated()) else ResultMonad(resp)
        } catch {
            case e: Exception => ErrorMonad(e)
        }
        
    def removeContainerMonad(containerId: String): Monad[Exception | ContainerNotRemoved, Boolean] = {
        try{
            val removed = removeContainer(containerId)
            if (removed.success)
                ResultMonad(true)
            else ErrorMonad(ContainerNotRemoved(removed.error))
        } catch {
            case e: Exception => ErrorMonad(e)
        }
    }

    def stopContainer(containerId: String) = {
        val resp = client.stopServer(StopServerRequest(containerId), defaultTimeout)
        resp.success
    }

    def startContainer(containerId: String) = {
        val resp = client.startServer(StartServerRequest(containerId), defaultTimeout)
        StartContainerResponse(resp.vmId, resp.vmWhiteIp, resp.vmWhitePorts)
    }

    def findContainerBySlug(vmSlug: String) = {
        val resp = client.getServerList()
        resp.vmList find {vm => vm.id == vmSlug} 
    }

    def createContainer(request: CreateContainerRequest) = {
        val response = client.createServer(compositor.client.models.CreateServerRequest(
            request.vmImageUri,
            request.vmName,
            request.vmAvailableRamBytes, 
            request.vmAvailableDiskBytes,
            request.vmAvailableSwapBytes,
            request.vmExposePorts
        ), defaultTimeout)
        CreateContainerResponse(response.vmId)
    }

    def removeContainer(containerId: String) = {
        val response = client.removeServer(compositor.client.models.RemoveServerRequest(containerId), defaultTimeout)
        RemoveContainerResponse(response.error, response.success)
    }
}

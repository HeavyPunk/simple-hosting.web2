package components.clients.controller

import io.github.heavypunk.controller.client.Settings
import io.github.heavypunk.controller.client.ControllerClient
import io.github.heavypunk.controller.client.CommonControllerClient
import io.github.heavypunk.controller.client.server.CommonControllerServerClient
import io.github.heavypunk.controller.client.state.CommonControllerStateClient
import io.github.heavypunk.controller.client.files.CommonControllerFilesClient

class ControllerClientFactory {
    def getControllerClient(controllerClientSettings: Settings): ControllerClient = 
        new CommonControllerClient(
            new CommonControllerServerClient(controllerClientSettings),
            new CommonControllerStateClient(controllerClientSettings),
            new CommonControllerFilesClient(controllerClientSettings),
        )
}

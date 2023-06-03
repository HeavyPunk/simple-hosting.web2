package components.clients.controller

import io.github.heavypunk.controller.client.ControllerClient
import io.github.heavypunk.controller.client.Settings
import java.io.IOException

object ControllerUtils {
    def findControllerPort(input: Seq[String], controllerClientFactory: ControllerClientFactory, controllerBaseSettings: Settings): Option[Int] = {
        val controllerPort = input.find { port => 
            try {
                val portAsInt = port.toInt
                val client = controllerClientFactory.getControllerClient(new Settings(
                    controllerBaseSettings.scheme,
                    controllerBaseSettings.host,
                    portAsInt
                ))
                val res = client.state.ping()
                res.success
            } catch {
                case e: Exception => false
            }
        } 
        if (controllerPort.isDefined) Some(controllerPort.get.toInt) else None
    }

    def checkForServerRunning(controllerClientFactory: ControllerClientFactory, controllerBaseSettings: Settings): Boolean = {
        try {
            val client = controllerClientFactory.getControllerClient(new Settings(
                controllerBaseSettings.scheme,
                controllerBaseSettings.host,
                controllerBaseSettings.port
            ))
            client.state.ping().success
        } catch {
            case e: Exception => false
        }
    }
}

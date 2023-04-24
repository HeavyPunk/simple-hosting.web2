package components.clients.controller

import com.fasterxml.jackson.annotation.JsonProperty

class StartGameServerRequest (
    val gameServerId: String,
    val saveStdout: Boolean,
    val saveStderr: Boolean
)

class StopGameServerRequest (
    val gameServerId: String,
    val force: Boolean
)

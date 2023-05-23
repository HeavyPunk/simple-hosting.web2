package components.clients.controller

import com.fasterxml.jackson.annotation.JsonProperty

class StartGameServerRequest (
    val gameServerHash: String,
    val saveStdout: Boolean,
    val saveStderr: Boolean
)

class StopGameServerRequest (
    val gameServerHash: String,
    val force: Boolean
)

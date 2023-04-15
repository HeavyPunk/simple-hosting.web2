package components.clients.controller

import com.fasterxml.jackson.annotation.JsonProperty

class StartGameServerRequest (
    @JsonProperty("game-server-id") val gameServerId: String,
    @JsonProperty("save-stdout") val saveStdout: Boolean,
    @JsonProperty("save-stderr") val saveStderr: Boolean
)

class StopGameServerRequest (
    @JsonProperty("game-server-id") val gameServerId: String,
    @JsonProperty("force") val force: Boolean
)

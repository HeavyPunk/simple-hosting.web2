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

class GetServerInfoRequest (
    val postSystem: String,
    val gameServerHash: String,
)

class GetServerLogsOnPageRequest (
    val gameServerHash: String,
    val page: Int,
    val isLastLogs: Boolean
)

class SendServerMessageRequest (
    val gameServerHash: String,
    val postSystem: String,
    val message: String
)

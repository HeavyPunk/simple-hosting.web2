package components.clients.juggernaut

import com.fasterxml.jackson.annotation.JsonProperty

final case class Task(
    @JsonProperty("kind") val kind: String,
    @JsonProperty("context") val context: String
)

final case class TaskStatus(
    @JsonProperty("id") val id: String,
    @JsonProperty("found") val found: Boolean,
    @JsonProperty("status") val taskStatus: String,
)

final case class DownloadFileTask(
    @JsonProperty("controller-scheme") val controllerScheme: String,
    @JsonProperty("controller-ip") val controllerIp: String,
    @JsonProperty("controller-port") val controllerPort: Int,
    @JsonProperty("s3-bucket") val s3Bucket: String,
    @JsonProperty("s3-path") val s3Path: String,
    @JsonProperty("destination-path") val destinationPath: String
)

package components.clients.files

import com.fasterxml.jackson.annotation.JsonProperty

final case class DeleteFileRequest(
    @JsonProperty("gameServerHash") val gameServerHash: String,
    @JsonProperty("path") val pathToFile: String
)

final case class DeleteFileResponse(
    @JsonProperty("taskId") val taskId: String,
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)

final case class CreateFileRequest(
    @JsonProperty("gameServerHash") val gameServerHash: String,
    @JsonProperty("path") val pathToFile: String,
    @JsonProperty("content") val content: String
)

final case class CreateFileResponse(
    @JsonProperty("taskId") val taskId: String,
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)

final case class CreateDirectoryRequest(
    @JsonProperty("gameServerHash") val gameServerHash: String,
    @JsonProperty("path") val pathToDirectory: String,
)

final case class CreateDirectoryResponse(
    @JsonProperty("taskId") val taskId: String,
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)

final case class ListDirectoryRequest(
    @JsonProperty("gameServerHash") val gameServerHash: String,
    @JsonProperty("path") val pathToDirectory: String
)

final case class ListDirectoryResponse(
    @JsonProperty("files") val files: Seq[FileNode],
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)

final case class FileNode(
    @JsonProperty("path") val path: String,
    @JsonProperty("type") val nodeType: String,
    @JsonProperty("size") val size: Long,
    @JsonProperty("name") val fileName: String,
    @JsonProperty("extension") val extension: String
)

final case class GetFileContentRequest(
    @JsonProperty("gameServerHash") val gameServerHash: String,
    @JsonProperty("path") val path: String
)

final case class GetFileContentResponse(
    @JsonProperty("content") val content: String,
    @JsonProperty("success") val success: Boolean,
    @JsonProperty("error") val error: String
)

package components.basic

final case class MessageResponse(
    val message: String,
    val success: Boolean
)

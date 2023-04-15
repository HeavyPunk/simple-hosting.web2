package components.services.business

import com.fasterxml.jackson.annotation.JsonProperty

case class RegisterUserRequest (
    val login: String,
    val email: String,
    val password: String
)

case class LoginUserRequest (
    val login: String,
    val password: String
)

case class LoginUserResponse (
    @JsonProperty("auth-token") val authToken: String,
)

case class LogoutUserRequest (
    @JsonProperty("auth-token") val authToken: String
)




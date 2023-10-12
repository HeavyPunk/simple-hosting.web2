package components.services.business

import com.fasterxml.jackson.annotation.JsonProperty
import business.entities.User
import business.services.oauth2.OAuth2System

case class AuthorizeOAuth2 (
    val system: OAuth2System
)

case class RegisterUserRequest (
    val login: String,
    val email: String,
    val password: String
)

case class LoginUserRequest (
    val login: String,
    val password: String
)

case class UserModel (
    val id: Long,
    val email: String,
    val userName: String,
    val isAdmin: Boolean,
    val avatarUrl: String,
)

case class LoginUserResponse (
    val authToken: String,
    val user: UserModel 
)

case class LogoutUserRequest (
    val authToken: String
)

case class GetCurrentUserResponse (
    val email: String,
    val login: String,
    val avatarUrl: String
)


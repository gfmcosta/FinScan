package pt.ipt.dama2026.finscan.data.api.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a request to register a new user.
 */
data class RegisterRequest(
    @SerializedName("username")
    val username: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("role")
    val role: String = "user"
)

/**
 * Represents a request to change the user's password.
 */
data class ChangePasswordRequest(
    @SerializedName("current_password")
    val currentPassword: String,
    @SerializedName("new_password")
    val newPassword: String
)

/**
 * Represents a request to send a password reset email.
 */
data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String
)

/**
 * Represents a request to reset the user's password.
 */
data class ResetPasswordRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("code")
    val code: String,
    @SerializedName("new_password")
    val newPassword: String
)

/**
 * Represents a request to refresh the access token.
 */
data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

/**
 * Represents a response containing a token, name and email of a user.
 */
data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("token_type")
    val tokenType: String = "bearer",
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null
)

/**
 * Represents a response containing a user's information.
 */
data class UserResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("avatar")
    val avatar: String? = null
)

/**
 * Represents a request to update the user's profile.
 */
data class UpdateProfileRequest(
    @SerializedName("username")
    val username: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("avatar_base64")
    val avatarBase64: String? = null,
)
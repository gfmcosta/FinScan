package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.*
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.PATCH

/**
 * Retrofit interface for the Auth API.
 */
interface AuthApiService {

    /**
     * Logs in a user.
     * @param username The user's username.
     * @param password The user's password.
     * @return A [Response] containing a [TokenResponse] on success.
     */
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    /**
     * Registers a new user.
     * @param request The [RegisterRequest] containing user registration details.
     * @return A [Response] containing a [UserResponse] on success.
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    /**
     * Changes the user's password.
     * @param request The [ChangePasswordRequest] containing current and new passwords.
     * @return A [Response] containing a [Unit] on success.
     */
    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    /**
     * Sends a password reset email.
     * @param request The [ForgotPasswordRequest] containing the user's email.
     * @return A [Response] containing a [Unit] on success.
     */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Unit>

    /**
     * Resets the user's password.
     * @param request The [ResetPasswordRequest] containing reset details.
     * @return A [Response] containing a [Unit] on success.
     */
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>

    /**
     * Refreshes the access token.
     * @param request The [RefreshTokenRequest] containing the refresh token.
     * @return A [Response] containing a [TokenResponse] on success.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    /**
     * Updates the user's profile.
     * @param request The [UpdateProfileRequest] containing updated profile details.
     * @return A [Response] containing a [UserResponse] on success.
     */
    @PATCH("users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserResponse>

    /**
     * Retrieves the user's profile.
     * @return A [Response] containing a [UserResponse] on success.
     */
    @GET("users/me")
    suspend fun getMe(): Response<UserResponse>
}

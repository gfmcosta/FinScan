package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<TokenResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>
}

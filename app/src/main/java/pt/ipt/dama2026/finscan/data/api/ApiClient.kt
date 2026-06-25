package pt.ipt.dama2026.finscan.data.api

import android.content.Context
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import pt.ipt.dama2026.finscan.data.api.models.RefreshTokenRequest
import pt.ipt.dama2026.finscan.data.api.services.AuthApiService
import pt.ipt.dama2026.finscan.data.datastore.AuthManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    //    private const val BASE_URL = "https://finscan-production.up.railway.app/api/v1/"
    private const val BASE_URL = "http://10.0.2.2:8000/api/v1/"

    private var authManager: AuthManager? = null
    private var retrofit: Retrofit? = null

    fun initialize(context: Context) {
        authManager = AuthManager.getInstance(context)
    }

    fun getRetrofit(): Retrofit {
        if (retrofit == null) {
            retrofit = buildRetrofit()
        }
        return retrofit!!
    }

    private fun buildRetrofit(): Retrofit {
        val httpClientBuilder = OkHttpClient.Builder()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        httpClientBuilder.addInterceptor(loggingInterceptor)
        httpClientBuilder.addInterceptor(authInterceptor())
        httpClientBuilder.authenticator(tokenAuthenticator())

        httpClientBuilder.connectTimeout(30, TimeUnit.SECONDS)
        httpClientBuilder.readTimeout(30, TimeUnit.SECONDS)
        httpClientBuilder.writeTimeout(30, TimeUnit.SECONDS)

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun authInterceptor(): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path.contains("auth/login") || path.contains("auth/register") || path.contains("auth/refresh")) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val token = runBlocking { authManager?.getTokenSync() }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        if (originalRequest.header("Content-Type") == null) {
            requestBuilder.header("Content-Type", "application/json")
        }
        chain.proceed(requestBuilder.build())
    }

    /**
     * Called automatically by OkHttp on every 401 response.
     * Tries to get a new access token using the stored refresh token.
     * If the refresh succeeds, the original request is retried transparently.
     * If the refresh fails (expired/invalid), auth is cleared → UI navigates to login.
     */
    private fun tokenAuthenticator(): Authenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
            // Don't retry if the failing request was already a refresh attempt
            val path = response.request.url.encodedPath
            if (path.contains("auth/refresh") || path.contains("auth/login")) return null

            // Don't retry more than once (priorResponse means we already tried)
            if (response.priorResponse != null) return null

            val refreshToken = runBlocking { authManager?.getRefreshTokenSync() }
                ?: run {
                    runBlocking { authManager?.clearAuth() }
                    return null
                }

            // Use a plain client (no authenticator) to call the refresh endpoint
            val plainClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val refreshRetrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(plainClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val newTokens = runBlocking {
                try {
                    refreshRetrofit.create(AuthApiService::class.java)
                        .refreshToken(RefreshTokenRequest(refreshToken))
                } catch (e: Exception) {
                    null
                }
            }

            val body = newTokens?.takeIf { it.isSuccessful }?.body()
            if (body == null) {
                // Refresh failed — force re-login
                runBlocking { authManager?.clearAuth() }
                return null
            }

            // Save new tokens and retry original request
            runBlocking { authManager?.updateTokens(body.accessToken, body.refreshToken) }

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${body.accessToken}")
                .build()
        }
    }

    fun resetRetrofit() {
        retrofit = null
    }
}

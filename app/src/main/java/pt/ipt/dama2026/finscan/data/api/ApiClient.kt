package pt.ipt.dama2026.finscan.data.api

import android.content.Context
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import pt.ipt.dama2026.finscan.data.datastore.AuthManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Change this to your API backend URL
    // For local development with emulator: http://10.0.2.2:8000
    // For local development with real device: http://YOUR_IP:8000
    // For production: https://your-api-domain.com
    private const val BASE_URL = "https://finscan-production.up.railway.app/api/v1/"

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

        // Add logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        httpClientBuilder.addInterceptor(loggingInterceptor)

        // Add auth interceptor to add Bearer token to requests
        httpClientBuilder.addInterceptor(authInterceptor())

        // Configure timeouts
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
        var authenticatedRequest = originalRequest

        // Try to get the token (non-blocking)
        authManager?.let { manager ->
            try {
                // If you need to get token synchronously, you can use runBlocking
                // but it's better to pass token through a different approach
                // For now, we'll skip adding token for non-auth endpoints
                
                // Add the token if it exists
                authenticatedRequest = originalRequest.newBuilder().apply {
                    // Note: For proper token handling, consider using suspend functions
                    // or passing the token directly to the service methods
                    addHeader("Content-Type", "application/json")
                }.build()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        chain.proceed(authenticatedRequest)
    }

    fun resetRetrofit() {
        retrofit = null
    }
}

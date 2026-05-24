package pt.ipt.dama2026.finscan.data.api

import android.content.Context
import kotlinx.coroutines.runBlocking
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
        val path = originalRequest.url.encodedPath
        
        // Skip adding token if it's login or register
        if (path.contains("auth/login") || path.contains("auth/register")) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val token = runBlocking {
            authManager?.getTokenSync()
        }

        val requestBuilder = originalRequest.newBuilder()
        
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        // Ensure Content-Type is set for JSON requests if not already set
        if (originalRequest.header("Content-Type") == null) {
            requestBuilder.header("Content-Type", "application/json")
        }

        chain.proceed(requestBuilder.build())
    }

    fun resetRetrofit() {
        retrofit = null
    }
}

package pt.ipt.dama2026.finscan.data.api.services

import android.content.Context
import pt.ipt.dama2026.finscan.data.api.ApiClient
import pt.ipt.dama2026.finscan.data.api.models.*
import pt.ipt.dama2026.finscan.data.datastore.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AuthService(private val context: Context) {
    private val authApiService: AuthApiService = ApiClient.getRetrofit().create(AuthApiService::class.java)
    private val authManager = AuthManager.getInstance(context)

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
        object Loading : Result<Nothing>()
    }

    /**
     * Fazer login com username e password
     * Retorna o token de acesso
     */
    suspend fun login(username: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = authApiService.login(username, password)

            when {
                response.isSuccessful && response.body() != null -> {
                    val tokenResponse = response.body()!!
                    // Guardar token e nome
                    authManager.saveToken(tokenResponse.accessToken, username, tokenResponse.name, tokenResponse.refreshToken)
                    Result.Success(tokenResponse.accessToken)
                }
                response.code() == 401 -> {
                    Result.Error("Invalid credentials")
                }
                response.code() == 404 -> {
                    Result.Error("User not found")
                }
                response.code() == 400 -> {
                    Result.Error("Invalid request")
                }
                response.code() >= 500 -> {
                    Result.Error("Internal system error. Please contact the administrator.")
                }
            else -> {
                    val errorBody = response.errorBody()?.string()
                    Result.Error(errorBody ?: "Login failed: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Registar novo utilizador
     * Retorna os dados do utilizador
     */
    suspend fun register(
        username: String,
        name: String,
        email: String,
        password: String
    ): Result<UserResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val request = RegisterRequest(
                username = username,
                name = name,
                email = email,
                password = password,
                role = "user"
            )
            val response = authApiService.register(request)

            when {
                response.isSuccessful && response.body() != null -> {
                    Result.Success(response.body()!!)
                }
                response.code() == 400 -> {
                    val errorBody = response.errorBody()?.string()
                    val message = if (errorBody?.contains("already registered") == true) {
                        "Username or email already registered"
                    } else if (errorBody?.contains("email") == true) {
                        "Email already in use"
                    } else {
                        "Invalid registration data"
                    }
                    Result.Error(message)
                }
                response.code() == 409 -> {
                    Result.Error("Username or email already exists")
                }
                response.code() >= 500 -> {
                    Result.Error("Internal system error. Please contact the administrator.")
                }
            else -> {
                    val errorBody = response.errorBody()?.string()
                    Result.Error(errorBody ?: "Registration failed: ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Fazer logout
     * Remove o token armazenado
     */
    suspend fun logout() {
        withContext(Dispatchers.IO) {
            authManager.clearAuth()
        }
    }

    /**
     * Verificar se está autenticado
     */
    suspend fun isAuthenticated(): Boolean {
        return try {
            authManager.isLoggedIn.first()
        } catch (e: Exception) {
            false
        }
    }
}

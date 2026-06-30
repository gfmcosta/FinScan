package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.ChatRequest
import pt.ipt.dama2026.finscan.data.api.models.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the Chat API.
 */
interface ChatApiService {

    /**
     * Asks a question to the chatbot.
     * @param request The [ChatRequest] containing the question.
     * @return A [Response] containing a [ChatResponse] on success.
     */
    @POST("chatbot/ask")
    suspend fun ask(@Body request: ChatRequest): Response<ChatResponse>
}

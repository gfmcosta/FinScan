package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.ChatRequest
import pt.ipt.dama2026.finscan.data.api.models.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApiService {

    @POST("chatbot/ask")
    suspend fun ask(@Body request: ChatRequest): Response<ChatResponse>
}

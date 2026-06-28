package pt.ipt.dama2026.finscan.data.api.models

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val question: String
)

data class ChatResponse(
    val answer: String
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

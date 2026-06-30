package pt.ipt.dama2026.finscan.data.api.models

/**
 * Represents a request to create a new chat question.
 */
data class ChatRequest(
    val question: String
)

/**
 * Represents a response containing the answer to a chat question.
 */
data class ChatResponse(
    val answer: String
)

/**
 * Represents a chat message.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

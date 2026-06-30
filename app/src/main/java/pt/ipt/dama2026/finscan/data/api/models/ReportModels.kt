package pt.ipt.dama2026.finscan.data.api.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a request to generate a report.
 */
data class GenerateReportRequest(
    @SerializedName("date_from") val dateFrom: String? = null,  // "YYYY-MM-DD" or null
    @SerializedName("date_to")   val dateTo: String? = null,
    @SerializedName("locale")    val locale: String = "en"
)

/**
 * Represents a response containing information about a report.
 */
data class ReportResponse(
    @SerializedName("id")         val id: Int,
    @SerializedName("status")     val status: String,        // "generating" | "completed" | "failed"
    @SerializedName("filename")   val filename: String? = null,
    @SerializedName("date_from")  val dateFrom: String? = null,
    @SerializedName("date_to")    val dateTo: String? = null,
    @SerializedName("created_at") val createdAt: String
)

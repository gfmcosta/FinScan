package pt.ipt.dama2026.finscan.data.api.services

import okhttp3.ResponseBody
import pt.ipt.dama2026.finscan.data.api.models.GenerateReportRequest
import pt.ipt.dama2026.finscan.data.api.models.ReportResponse
import retrofit2.Response
import retrofit2.http.*

interface ReportApiService {

    @POST("reports/generate")
    suspend fun generateReport(@Body request: GenerateReportRequest): Response<ReportResponse>

    @GET("reports")
    suspend fun getReports(
        @Query("skip")  skip: Int  = 0,
        @Query("limit") limit: Int = 11,
        @Query("month") month: Int? = null,
        @Query("year")  year: Int?  = null,
    ): Response<List<ReportResponse>>

    @DELETE("reports/{id}")
    suspend fun deleteReport(@Path("id") id: Int): Response<Unit>

    @GET("reports/{id}/download")
    @Streaming
    suspend fun downloadReport(@Path("id") id: Int): Response<ResponseBody>
}

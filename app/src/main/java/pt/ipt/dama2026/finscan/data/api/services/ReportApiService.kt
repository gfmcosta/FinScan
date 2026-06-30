package pt.ipt.dama2026.finscan.data.api.services

import okhttp3.ResponseBody
import pt.ipt.dama2026.finscan.data.api.models.GenerateReportRequest
import pt.ipt.dama2026.finscan.data.api.models.ReportResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the Report API.
 */
interface ReportApiService {

    /**
     * Generates a report.
     * @param request The [GenerateReportRequest] containing report details.
     * @return A [Response] containing a [ReportResponse] on success.
     */
    @POST("reports/generate")
    suspend fun generateReport(@Body request: GenerateReportRequest): Response<ReportResponse>

    /**
     * Lists reports.
     * @param skip The number of reports to skip.
     * @param limit The maximum number of reports to return.
     * @param month The month of the reports (optional).
     * @param year The year of the reports (optional).
     */
    @GET("reports")
    suspend fun getReports(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 11,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
    ): Response<List<ReportResponse>>

    /**
     * Deletes a report.
     * @param id The ID of the report to delete.
     * @return A [Response] containing a [Unit] on success.
     */
    @DELETE("reports/{id}")
    suspend fun deleteReport(@Path("id") id: Int): Response<Unit>

    /**
     * Downloads a report.
     * @param id The ID of the report to download.
     * @return A [Response] containing a [ResponseBody] on success.
     */
    @GET("reports/{id}/download")
    @Streaming
    suspend fun downloadReport(@Path("id") id: Int): Response<ResponseBody>
}

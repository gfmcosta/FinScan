package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.ExpenseStats
import pt.ipt.dama2026.finscan.data.api.models.ReceiptCreateRequest
import pt.ipt.dama2026.finscan.data.api.models.ReceiptResponse
import pt.ipt.dama2026.finscan.data.api.models.ReceiptUpdateRequest
import retrofit2.Response
import retrofit2.http.*

interface ReceiptApiService {

    @GET("receipts")
    suspend fun listReceipts(
        @Query("search") search: String? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20,
    ): Response<List<ReceiptResponse>>

    @POST("receipts")
    suspend fun createReceipt(@Body request: ReceiptCreateRequest): Response<ReceiptResponse>

    @GET("receipts/{id}")
    suspend fun getReceipt(
        @Path("id") id: Int
    ): Response<ReceiptResponse>

    @PUT("receipts/{id}")
    suspend fun updateReceipt(
        @Path("id") id: Int,
        @Body request: ReceiptUpdateRequest
    ): Response<ReceiptResponse>

    @GET("receipts/stats")
    suspend fun getStats(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<ExpenseStats>

    @DELETE("receipts/{id}")
    suspend fun deleteReceipt(
        @Path("id") id: Int
    ): Response<Unit>
}

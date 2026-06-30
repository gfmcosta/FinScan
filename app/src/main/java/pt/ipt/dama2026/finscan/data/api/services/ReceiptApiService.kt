package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.ExpenseStats
import pt.ipt.dama2026.finscan.data.api.models.ReceiptCreateRequest
import pt.ipt.dama2026.finscan.data.api.models.ReceiptResponse
import pt.ipt.dama2026.finscan.data.api.models.ReceiptUpdateRequest
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the Receipt API.
 */
interface ReceiptApiService {

    /**
     * Lists receipts.
     * @param search The search query.
     * @param skip The number of receipts to skip.
     * @param limit The maximum number of receipts to return.
     * @return A [Response] containing a list of [ReceiptResponse] on success.
     */
    @GET("receipts")
    suspend fun listReceipts(
        @Query("search") search: String? = null,
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20,
    ): Response<List<ReceiptResponse>>

    /**
     * Creates a new receipt.
     * @param request The [ReceiptCreateRequest] containing receipt details.
     * @return A [Response] containing a [ReceiptResponse] on success.
     */
    @POST("receipts")
    suspend fun createReceipt(@Body request: ReceiptCreateRequest): Response<ReceiptResponse>

    /**
     * Updates an existing receipt.
     * @param id The ID of the receipt to update.
     * @param request The [ReceiptUpdateRequest] containing updated receipt details.
     * @return A [Response] containing a [ReceiptResponse] on success.
     */
    @PUT("receipts/{id}")
    suspend fun updateReceipt(
        @Path("id") id: Int,
        @Body request: ReceiptUpdateRequest
    ): Response<ReceiptResponse>

    /**
     * Retrieves statistics about expenses.
     * @param startDate The start date for the statistics (optional).
     * @param endDate The end date for the statistics (optional).
     * @return A [Response] containing [ExpenseStats] on success.
     */
    @GET("receipts/stats")
    suspend fun getStats(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<ExpenseStats>

    /**
     * Deletes a receipt.
     * @param id The ID of the receipt to delete.
     * @return A [Response] containing a [Unit] on success.s
     */
    @DELETE("receipts/{id}")
    suspend fun deleteReceipt(
        @Path("id") id: Int
    ): Response<Unit>
}

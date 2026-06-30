package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.ReceiptOCRRequest
import pt.ipt.dama2026.finscan.data.api.models.ReceiptResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the Scan API.
 */
interface ScanApiService {

    /**
     * Scans a receipt.
     * @param payload The [ReceiptOCRRequest] containing receipt details.
     * @return A [Response] containing a [ReceiptResponse] on success.
     */
    @POST("receipts/scan")
    suspend fun scanReceipt(
        @Body payload: ReceiptOCRRequest
    ): Response<ReceiptResponse>
}

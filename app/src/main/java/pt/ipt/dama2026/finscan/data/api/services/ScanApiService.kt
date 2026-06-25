package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.ReceiptOCRRequest
import pt.ipt.dama2026.finscan.data.api.models.ReceiptResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ScanApiService {

    @POST("receipts/scan")
    suspend fun scanReceipt(
        @Body payload: ReceiptOCRRequest
    ): Response<ReceiptResponse>
}

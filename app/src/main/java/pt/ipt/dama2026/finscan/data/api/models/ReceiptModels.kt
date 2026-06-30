package pt.ipt.dama2026.finscan.data.api.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a request to create a new receipt.
 */
data class ReceiptCreateRequest(
    @SerializedName("store")
    val store: String,
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("total")
    val total: Double,
    @SerializedName("purchase_date")
    val purchaseDate: String,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null
)

/**
 * Represents a request to OCR a receipt image.
 */
data class ReceiptOCRRequest(
    @SerializedName("image_base64")
    val imageBase64: String,
    @SerializedName("mime_type")
    val mimeType: String = "image/jpeg",
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null
)

/**
 * Represents a request to update a receipt.
 */
data class ReceiptUpdateRequest(
    @SerializedName("store")
    val store: String? = null,
    @SerializedName("category_id")
    val categoryId: Int? = null,
    @SerializedName("total")
    val total: Double? = null,
    @SerializedName("purchase_date")
    val purchaseDate: String? = null,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null
)

/**
 * Represents a response containing information about a receipt.
 */
data class ReceiptResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("store")
    val store: String,
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("category_name")
    val categoryName: String,
    @SerializedName("category_icon")
    val categoryIcon: String = "Category",
    @SerializedName("total")
    val total: Double,
    @SerializedName("purchase_date")
    val purchaseDate: String,
    @SerializedName("latitude")
    val latitude: Double?,
    @SerializedName("longitude")
    val longitude: Double?,
    @SerializedName("owner_id")
    val ownerId: Int
)

/**
 * Represents a response containing information about a group.
 */
data class GroupTotal(
    @SerializedName("key")
    val key: String,
    @SerializedName("total")
    val total: Double
)

/**
 * Represents a response containing information about expenses.
 */
data class ExpenseStats(
    @SerializedName("by_category")
    val byCategory: List<GroupTotal>,
    @SerializedName("by_store")
    val byStore: List<GroupTotal>
)
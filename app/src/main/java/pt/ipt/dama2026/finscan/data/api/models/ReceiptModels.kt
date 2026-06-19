package pt.ipt.dama2026.finscan.data.api.models

import com.google.gson.annotations.SerializedName

data class ReceiptResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("store")
    val store: String,
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("category_name")
    val categoryName: String,
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

data class GroupTotal(
    @SerializedName("key")
    val key: String,
    @SerializedName("total")
    val total: Double
)

data class ExpenseStats(
    @SerializedName("by_category")
    val byCategory: List<GroupTotal>,
    @SerializedName("by_store")
    val byStore: List<GroupTotal>
)

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

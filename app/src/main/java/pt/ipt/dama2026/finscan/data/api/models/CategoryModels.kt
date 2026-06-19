package pt.ipt.dama2026.finscan.data.api.models

import com.google.gson.annotations.SerializedName

data class CategoryResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("owner_id")
    val ownerId: Int?,
    @SerializedName("is_default")
    val isDefault: Boolean
)

data class CategoryCreateRequest(
    @SerializedName("name")
    val name: String
)

data class CategoryUpdateRequest(
    @SerializedName("name")
    val name: String
)

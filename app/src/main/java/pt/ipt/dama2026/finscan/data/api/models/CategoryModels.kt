package pt.ipt.dama2026.finscan.data.api.models

import com.google.gson.annotations.SerializedName

/**
 * Represents a request to create a new category.
 */
data class CategoryCreateRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("icon")
    val icon: String = "Category"
)

/**
 * Represents a request to update a category.
 */
data class CategoryUpdateRequest(
    @SerializedName("name")
    val name: String,
    @SerializedName("icon")
    val icon: String? = null
)

/**
 * Represents a response containing information about a category.
 */
data class CategoryResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("icon")
    val icon: String = "Category",
    @SerializedName("owner_id")
    val ownerId: Int?,
    @SerializedName("is_default")
    val isDefault: Boolean
)
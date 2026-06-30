package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.CategoryCreateRequest
import pt.ipt.dama2026.finscan.data.api.models.CategoryResponse
import pt.ipt.dama2026.finscan.data.api.models.CategoryUpdateRequest
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Query

/**
 * Retrofit interface for the Category API.
 */
interface CategoryApiService {

    /**
     * Lists categories.
     * @param skip The number of categories to skip.
     * @param limit The maximum number of categories to return.
     * @return A [Response] containing a list of [CategoryResponse] on success.
     */
    @GET("categories")
    suspend fun listCategories(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 10,
    ): Response<List<CategoryResponse>>

    /**
     * Creates a new category.
     * @param request The [CategoryCreateRequest] containing category details.
     * @return A [Response] containing a [CategoryResponse] on success.
     */
    @POST("categories")
    suspend fun createCategory(@Body request: CategoryCreateRequest): Response<CategoryResponse>

    /**
     * Updates an existing category.
     * @param id The ID of the category to update.
     * @param request The [CategoryUpdateRequest] containing updated category details.
     * @return A [Response] containing a [CategoryResponse] on success.
     */
    @PUT("categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Int,
        @Body request: CategoryUpdateRequest
    ): Response<CategoryResponse>

    /**
     * Deletes a category.
     * @param id The ID of the category to delete.
     * @return A [Response] containing a [Unit] on success.
     */
    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): Response<Unit>
}

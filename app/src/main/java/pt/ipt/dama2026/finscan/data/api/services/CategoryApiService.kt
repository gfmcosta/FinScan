package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.CategoryCreateRequest
import pt.ipt.dama2026.finscan.data.api.models.CategoryResponse
import pt.ipt.dama2026.finscan.data.api.models.CategoryUpdateRequest
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.Query

interface CategoryApiService {

    @GET("categories")
    suspend fun listCategories(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 10,
    ): Response<List<CategoryResponse>>

    @POST("categories")
    suspend fun createCategory(@Body request: CategoryCreateRequest): Response<CategoryResponse>

    @PUT("categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Int,
        @Body request: CategoryUpdateRequest
    ): Response<CategoryResponse>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): Response<Unit>
}

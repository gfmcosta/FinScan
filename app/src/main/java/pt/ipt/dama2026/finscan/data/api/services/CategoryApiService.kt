package pt.ipt.dama2026.finscan.data.api.services

import pt.ipt.dama2026.finscan.data.api.models.CategoryCreateRequest
import pt.ipt.dama2026.finscan.data.api.models.CategoryResponse
import pt.ipt.dama2026.finscan.data.api.models.CategoryUpdateRequest
import retrofit2.Response
import retrofit2.http.*

interface CategoryApiService {

    @GET("categories")
    suspend fun listCategories(): Response<List<CategoryResponse>>

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

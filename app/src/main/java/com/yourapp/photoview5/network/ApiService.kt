package com.yourapp.photoview5.network
import com.yourapp.photoview5.model.ApiResponse
import retrofit2.Response
import retrofit2.http.GET
interface ApiService {
    @GET("photos")
    suspend fun getAllPhotos(): Response<ApiResponse>
    companion object {
        const val BASE_URL = "https://photo-gallery-api-9biv.onrender.com/api/"
    }
}
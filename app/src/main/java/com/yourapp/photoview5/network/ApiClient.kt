package com.yourapp.photoview5.network
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
object ApiClient {
    private const val TIMEOUT = 30L
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .build()
    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiService.BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
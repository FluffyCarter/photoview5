package com.yourapp.photoview5.model
data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
    val data: List<Photo>? = null
)
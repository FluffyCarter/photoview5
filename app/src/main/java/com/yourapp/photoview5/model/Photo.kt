package com.yourapp.photoview5.model
import java.io.Serializable
data class Photo(
    val id: Int,
    val filename: String,
    val description: String? = "",
    val file_size: Long? = 0,
    val width: Int? = 0,
    val height: Int? = 0,
    val mime_type: String? = "",
    val tags: List<String>? = emptyList(),
    val created_at: String? = ""
) : Serializable {
    fun getImageUrl(): String {
        return "https://photo-gallery-api-9biv.onrender.com/api/photos/$id/image"
    }
    fun getFormattedSize(): String {
        val kb = file_size?.div(1024) ?: 0
        val mb = kb / 1024.0
        return if (mb >= 1) {
            "%.2f MB".format(mb)
        } else {
            "$kb KB"
        }
    }
    fun getResolution(): String {
        return "${width ?: 0} × ${height ?: 0}"
    }
}
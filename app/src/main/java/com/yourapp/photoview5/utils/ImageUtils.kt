@file:Suppress("DEPRECATION")
package com.yourapp.photoview5.utils
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
object ImageUtils {
    suspend fun downloadFavoriteImage(context: Context, photo: com.yourapp.photoview5.model.Photo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = photo.getImageUrl()
                val filename = photo.filename
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        saveBitmapToFavoritesAlbum(context, bitmap, filename, photo.id)
                        return@withContext true
                    }
                }
                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    private fun saveBitmapToFavoritesAlbum(context: Context, bitmap: Bitmap, filename: String, photoId: Int): String? {
        return try {
            val imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val favoritesDir = File(imagesDir, "API Favorites")
            if (!favoritesDir.exists()) {
                favoritesDir.mkdirs()
            }
            val sanitizedFilename = sanitizeFilename(filename)
            val file = File(favoritesDir, "${photoId}_$sanitizedFilename")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            notifyMediaScanner(context, file.absolutePath)
            savePhotoInfo(favoritesDir, photoId, sanitizedFilename)
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    private fun sanitizeFilename(filename: String): String {
        return filename.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    }
    private fun notifyMediaScanner(context: Context, filePath: String) {
        try {
            val mediaScanIntent = android.content.Intent(
                android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                android.net.Uri.fromFile(File(filePath))
            )
            context.sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun savePhotoInfo(directory: File, photoId: Int, filename: String) {
        try {
            val infoFile = File(directory, "${photoId}_info.txt")
            if (!infoFile.exists()) {
                infoFile.createNewFile()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun getFavoriteImageFile(photoId: Int): File? {
        val imagesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val favoritesDir = File(imagesDir, "API Favorites")
        if (!favoritesDir.exists()) {
            return null
        }
        val files = favoritesDir.listFiles()
        files?.forEach { file ->
            if (file.name.startsWith("${photoId}_")) {
                return file
            }
        }
        return null
    }
    fun isFavoriteDownloaded(photoId: Int): Boolean {
        return getFavoriteImageFile(photoId)?.exists() ?: false
    }
    fun deleteFavoriteImage(photoId: Int): Boolean {
        return try {
            val file = getFavoriteImageFile(photoId)
            file?.delete() ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    fun getAllFavoriteFiles(): List<File> {
        val imagesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val favoritesDir = File(imagesDir, "API Favorites")
        return if (favoritesDir.exists() && favoritesDir.isDirectory) {
            favoritesDir.listFiles()?.filter {
                it.isFile && (it.name.endsWith(".jpg", ignoreCase = true) ||
                        it.name.endsWith(".jpeg", ignoreCase = true) ||
                        it.name.endsWith(".png", ignoreCase = true))
            } ?: emptyList()
        } else {
            emptyList()
        }
    }
    fun clearFavoritesAlbum(): Boolean {
        return try {
            val imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val favoritesDir = File(imagesDir, "API Favorites")
            if (favoritesDir.exists() && favoritesDir.isDirectory) {
                favoritesDir.listFiles()?.forEach { it.delete() }
                return true
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun downloadImage(context: Context, url: String, filename: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        saveBitmapToGallery(context, bitmap, filename)
                        return@withContext true
                    }
                }
                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String): String? {
        return try {
            val imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val appDir = File(imagesDir, "PhotoViewer")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            val file = File(appDir, filename)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            context.sendBroadcast(
                android.content.Intent(
                    android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    android.net.Uri.fromFile(file)
                )
            )
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun getImageFile(filename: String): File? {
        val imagesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val appDir = File(imagesDir, "PhotoViewer")
        val file = File(appDir, filename)
        return if (file.exists()) file else null
    }
    fun isImageDownloaded(filename: String): Boolean {
        val file = getImageFile(filename)
        return file?.exists() ?: false
    }
    fun getAllDownloadedFiles(): List<File> {
        val imagesDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        val appDir = File(imagesDir, "PhotoViewer")
        return if (appDir.exists() && appDir.isDirectory) {
            appDir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    fun getDownloadedFilenames(): List<String> {
        return getAllDownloadedFiles().map { it.name }
    }
    fun deleteAllDownloadedFiles(): Boolean {
        return try {
            val files = getAllDownloadedFiles()
            files.forEach { it.delete() }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
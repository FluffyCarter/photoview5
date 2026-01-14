package com.yourapp.photoview5.utils
import android.content.Context
import android.content.SharedPreferences
class PreferenceHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("photo_viewer", Context.MODE_PRIVATE)
    companion object {
        private const val KEY_THEME = "app_theme"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_LAST_SYNC = "last_sync_ids"
        private const val KEY_AUTO_DOWNLOAD_FAVORITES = "auto_download_favorites"
    }
    fun saveTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }
    fun getTheme(): String = prefs.getString(KEY_THEME, "light") ?: "light"
    fun saveLanguage(language: String) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }
    fun getLanguage(): String = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    fun addFavorite(photoId: Int) {
        val favorites = getFavorites().toMutableSet()
        favorites.add(photoId.toString())
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }
    fun removeFavorite(photoId: Int) {
        val favorites = getFavorites().toMutableSet()
        favorites.remove(photoId.toString())
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
    }
    fun getFavorites(): Set<String> = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    fun isFavorite(photoId: Int): Boolean {
        return getFavorites().contains(photoId.toString())
    }
    fun setAutoDownloadFavorites(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DOWNLOAD_FAVORITES, enabled).apply()
    }
    fun isAutoDownloadFavorites(): Boolean {
        return prefs.getBoolean(KEY_AUTO_DOWNLOAD_FAVORITES, true) 
    }
    fun saveDownloadedIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_LAST_SYNC, ids).apply()
    }
    fun getDownloadedIds(): Set<String> = prefs.getStringSet(KEY_LAST_SYNC, emptySet()) ?: emptySet()
    fun syncDownloadedIdsWithFilesystem() {
        val downloadedFiles = com.yourapp.photoview5.utils.ImageUtils.getDownloadedFilenames()
        if (downloadedFiles.isEmpty()) {
            saveDownloadedIds(emptySet())
        }
    }
}
package com.yourapp.photoview5
import android.app.Application
import com.yourapp.photoview5.utils.LanguageHelper
import com.yourapp.photoview5.utils.PreferenceHelper
class PhotoViewerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = PreferenceHelper(this)
        LanguageHelper.setLocale(this, prefs.getLanguage())
    }
}
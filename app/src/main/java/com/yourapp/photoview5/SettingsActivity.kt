package com.yourapp.photoview5
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yourapp.photoview5.utils.LanguageHelper
import com.yourapp.photoview5.utils.PreferenceHelper
import kotlinx.coroutines.launch
class SettingsActivity : AppCompatActivity() {
    private lateinit var themeGroup: RadioGroup
    private lateinit var lightThemeRadio: RadioButton
    private lateinit var darkThemeRadio: RadioButton
    private lateinit var languageGroup: RadioGroup
    private lateinit var englishRadio: RadioButton
    private lateinit var russianRadio: RadioButton
    private lateinit var autoDownloadSwitch: Switch
    private lateinit var saveButton: Button
    private lateinit var clearFavoritesButton: Button
    private lateinit var prefs: PreferenceHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceHelper(this)
        val theme = prefs.getTheme()
        if (theme == "dark") {
            setTheme(R.style.Theme_PhotoViewer_Dark)
        } else {
            setTheme(R.style.Theme_PhotoViewer_Light)
        }
        setContentView(R.layout.activity_settings)
        initViews()
        loadCurrentSettings()
        saveButton.setOnClickListener {
            saveSettings()
        }
        clearFavoritesButton.setOnClickListener {
            showClearFavoritesConfirmation()
        }
    }
    private fun initViews() {
        themeGroup = findViewById(R.id.themeGroup)
        lightThemeRadio = findViewById(R.id.lightThemeRadio)
        darkThemeRadio = findViewById(R.id.darkThemeRadio)
        languageGroup = findViewById(R.id.languageGroup)
        englishRadio = findViewById(R.id.englishRadio)
        russianRadio = findViewById(R.id.russianRadio)
        autoDownloadSwitch = findViewById(R.id.autoDownloadSwitch)
        saveButton = findViewById(R.id.saveButton)
        clearFavoritesButton = findViewById(R.id.clearFavoritesButton)
    }
    private fun loadCurrentSettings() {
        val currentTheme = prefs.getTheme()
        val currentLanguage = prefs.getLanguage()
        val autoDownload = prefs.isAutoDownloadFavorites()
        if (currentTheme == "dark") {
            darkThemeRadio.isChecked = true
        } else {
            lightThemeRadio.isChecked = true
        }
        if (currentLanguage == "ru") {
            russianRadio.isChecked = true
        } else {
            englishRadio.isChecked = true
        }
        autoDownloadSwitch.isChecked = autoDownload
    }
    private fun saveSettings() {
        val selectedTheme = when (themeGroup.checkedRadioButtonId) {
            R.id.lightThemeRadio -> "light"
            R.id.darkThemeRadio -> "dark"
            else -> "light"
        }
        val selectedLanguage = when (languageGroup.checkedRadioButtonId) {
            R.id.englishRadio -> "en"
            R.id.russianRadio -> "ru"
            else -> "en"
        }
        val autoDownload = autoDownloadSwitch.isChecked
        prefs.saveTheme(selectedTheme)
        prefs.saveLanguage(selectedLanguage)
        prefs.setAutoDownloadFavorites(autoDownload)
        LanguageHelper.setLocale(this, selectedLanguage)
        Toast.makeText(
            this,
            getString(R.string.settings_saved),
            Toast.LENGTH_SHORT
        ).show()
        val intent = android.content.Intent(this, MainActivity::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }
    private fun showClearFavoritesConfirmation() {
        val favoritesCount = com.yourapp.photoview5.utils.ImageUtils.getAllFavoriteFiles().size
        if (favoritesCount == 0) {
            Toast.makeText(this, "Альбом избранного пуст", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Очистка альбома избранного")
            .setMessage("Вы уверены, что хотите удалить все $favoritesCount фото из альбома 'API Favorites'?\n\nЭто действие удалит только скачанные файлы, но не удалит фото из списка избранного в приложении.")
            .setPositiveButton("Удалить") { dialog, _ ->
                dialog.dismiss()
                clearFavoritesAlbum()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun clearFavoritesAlbum() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val success = com.yourapp.photoview5.utils.ImageUtils.clearFavoritesAlbum()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (success) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Альбом избранного очищен",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@SettingsActivity,
                        "Ошибка при очистке альбома",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
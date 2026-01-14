package com.yourapp.photoview5
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.yourapp.photoview5.adapter.PhotoAdapter
import com.yourapp.photoview5.model.Photo
import com.yourapp.photoview5.network.ApiClient
import com.yourapp.photoview5.utils.ImageUtils
import com.yourapp.photoview5.utils.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var downloadButton: Button
    private lateinit var favoritesButton: Button
    private lateinit var settingsButton: Button
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var prefs: PreferenceHelper
    private var photos = mutableListOf<Photo>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceHelper(this)
        val theme = prefs.getTheme()
        if (theme == "dark") {
            setTheme(R.style.Theme_PhotoViewer_Dark)
        } else {
            setTheme(R.style.Theme_PhotoViewer_Light)
        }
        setContentView(R.layout.activity_main)
        initViews()
        setupRecyclerView()
        loadPhotos()
        swipeRefresh.setOnRefreshListener {
            loadPhotos()
        }
        downloadButton.setOnClickListener {
            downloadAllPhotos()
        }
        favoritesButton.setOnClickListener {
            val intent = android.content.Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }
        settingsButton.setOnClickListener {
            val intent = android.content.Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    private fun initViews() {
        recyclerView = findViewById(R.id.photosRecyclerView)
        swipeRefresh = findViewById(R.id.swipeRefreshLayout)
        progressBar = findViewById(R.id.progressBar)
        downloadButton = findViewById(R.id.downloadButton)
        favoritesButton = findViewById(R.id.favoritesButton)
        settingsButton = findViewById(R.id.settingsButton)
    }
    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        photoAdapter = PhotoAdapter(
            photos,
            onItemClick = { photo ->
                val intent = android.content.Intent(this, PhotoDetailActivity::class.java)
                intent.putExtra("photo", photo)
                intent.putExtra("all_photos", ArrayList(photos))
                intent.putExtra("from_favorites", false)
                startActivity(intent)
            },
            onFavoriteClick = { photo ->
                toggleFavorite(photo)
            }
        )
        photoAdapter.setPreferenceHelper(prefs)
        recyclerView.adapter = photoAdapter
    }
    private fun toggleFavorite(photo: Photo) {
        if (prefs.isFavorite(photo.id)) {
            prefs.removeFavorite(photo.id)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                ImageUtils.deleteFavoriteImage(photo.id)
            }
            showFavoriteNotification(false, photo.filename)
        } else {
            prefs.addFavorite(photo.id)
            if (prefs.isAutoDownloadFavorites()) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val success = ImageUtils.downloadFavoriteImage(
                        this@MainActivity,
                        photo
                    )
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (success) {
                            showFavoriteNotification(true, photo.filename, true)
                        } else {
                            showFavoriteNotification(true, photo.filename, false, true)
                        }
                    }
                }
            } else {
                showFavoriteNotification(true, photo.filename, false)
            }
        }
        photoAdapter.notifyDataSetChanged()
    }
    private fun showFavoriteNotification(isAdded: Boolean, filename: String,
                                         downloaded: Boolean = false,
                                         downloadError: Boolean = false) {
        val message = if (isAdded) {
            if (downloaded) {
                "✓ $filename\nФайл сохранен в галерее"
            } else if (downloadError) {
                "⚠ $filename\nОшибка скачивания"
            } else {
                "♥ $filename\nДобавлено в избранное"
            }
        } else {
            "🗑 $filename\nУдалено из избранного"
        }
        val title = if (isAdded) "Добавлено в избранное" else "Удалено из избранного"
        val snackbar = com.google.android.material.snackbar.Snackbar.make(
            findViewById(android.R.id.content),
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        )
        snackbar.setAction("OK") {
            snackbar.dismiss()
        }
        val backgroundColor = if (isAdded) {
            if (downloadError) {
                android.R.color.holo_orange_dark
            } else {
                android.R.color.holo_green_dark
            }
        } else {
            android.R.color.holo_red_dark
        }
        snackbar.view.setBackgroundColor(
            resources.getColor(backgroundColor, theme)
        )
        snackbar.show()
    }
    private fun loadPhotos() {
        progressBar.visibility = android.view.View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getAllPhotos()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    swipeRefresh.isRefreshing = false
                    if (response.isSuccessful) {
                        photos.clear()
                        response.body()?.data?.let { photoList ->
                            photos.addAll(photoList)
                            photoAdapter.updatePhotos(photos)
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_loading_photos),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.network_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun downloadAllPhotos() {
        val downloadedFilenames = ImageUtils.getDownloadedFilenames().toSet()
        val newPhotos = photos.filter { photo ->
            !downloadedFilenames.contains(photo.filename)
        }
        if (newPhotos.isEmpty()) {
            showDownloadOptionsDialog()
            return
        }
        startDownloadProcess(newPhotos)
    }
    private fun showDownloadOptionsDialog() {
        val downloadedCount = ImageUtils.getAllDownloadedFiles().size
        val builder = android.app.AlertDialog.Builder(this)
            .setTitle("Фотографии уже скачаны")
            .setMessage("Найдено $downloadedCount скачанных фотографий.\nЧто вы хотите сделать?")
            .setPositiveButton("Проверить повторно") { dialog, _ ->
                dialog.dismiss()
                val currentDownloaded = ImageUtils.getDownloadedFilenames().toSet()
                val reallyNewPhotos = photos.filter { !currentDownloaded.contains(it.filename) }
                if (reallyNewPhotos.isEmpty()) {
                    Toast.makeText(this, "Все фотографии уже скачаны", Toast.LENGTH_SHORT).show()
                } else {
                    startDownloadProcess(reallyNewPhotos)
                }
            }
            .setNegativeButton("Удалить все") { dialog, _ ->
                dialog.dismiss()
                showDeleteConfirmationDialog()
            }
            .setNeutralButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
        builder.show()
    }
    private fun showDeleteConfirmationDialog() {
        val downloadedCount = ImageUtils.getAllDownloadedFiles().size
        android.app.AlertDialog.Builder(this)
            .setTitle("Подтверждение удаления")
            .setMessage("Вы уверены, что хотите удалить все $downloadedCount скачанных фотографий?")
            .setPositiveButton("Удалить") { dialog, _ ->
                dialog.dismiss()
                deleteAllDownloadedPhotos()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun deleteAllDownloadedPhotos() {
        CoroutineScope(Dispatchers.IO).launch {
            val success = ImageUtils.deleteAllDownloadedFiles()
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(
                        this@MainActivity,
                        "Все скачанные фотографии удалены",
                        Toast.LENGTH_SHORT
                    ).show()
                    prefs.saveDownloadedIds(emptySet())
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка при удалении файлов",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun startDownloadProcess(newPhotos: List<Photo>) {
        val progressDialog = android.app.ProgressDialog(this).apply {
            setTitle("Скачивание фотографий")
            setMessage("Подготовка...")
            setCancelable(false)
            setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
            max = newPhotos.size
            show()
        }
        CoroutineScope(Dispatchers.IO).launch {
            var successCount = 0
            val downloadedIds = prefs.getDownloadedIds().toMutableSet()
            newPhotos.forEachIndexed { index, photo ->
                withContext(Dispatchers.Main) {
                    progressDialog.setMessage("Скачивание: ${photo.filename}")
                    progressDialog.progress = index
                }
                val success = ImageUtils.downloadImage(
                    this@MainActivity,
                    photo.getImageUrl(),
                    photo.filename
                )
                if (success) {
                    successCount++
                    downloadedIds.add(photo.id.toString())
                }
            }
            prefs.saveDownloadedIds(downloadedIds)
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                if (successCount > 0) {
                    Toast.makeText(
                        this@MainActivity,
                        "Успешно скачано: $successCount из ${newPhotos.size} фотографий",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Не удалось скачать фотографии",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        loadPhotos()
    }
}
package com.yourapp.photoview5
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import com.bumptech.glide.Glide
import com.yourapp.photoview5.model.Photo
import com.yourapp.photoview5.utils.ImageUtils
import com.yourapp.photoview5.utils.PreferenceHelper
import com.yourapp.photoview5.view.ZoomImageView
import kotlinx.coroutines.launch
import java.util.ArrayList
@Suppress("DEPRECATION")
class PhotoDetailActivity : AppCompatActivity() {
    private lateinit var zoomImageView: ZoomImageView
    private lateinit var infoLayout: LinearLayout
    private lateinit var filenameTextView: TextView
    private lateinit var sizeTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var tagsTextView: TextView
    private lateinit var favoriteButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var prefs: PreferenceHelper
    private lateinit var currentPhoto: Photo
    private lateinit var allPhotos: List<Photo>
    private var currentIndex = 0
    private var isFromFavorites = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceHelper(this)
        val theme = prefs.getTheme()
        if (theme == "dark") {
            setTheme(R.style.Theme_PhotoViewer_Dark_FullScreen)
        } else {
            setTheme(R.style.Theme_PhotoViewer_Light_FullScreen)
        }
        setContentView(R.layout.activity_photo_detail)
        currentPhoto = intent.getSerializableExtra("photo") as Photo
        isFromFavorites = intent.getBooleanExtra("from_favorites", false)
        if (isFromFavorites) {
            val allPhotosFromIntent = intent.getSerializableExtra("all_photos") as? ArrayList<Photo> ?: emptyList()
            val favoriteIds = prefs.getFavorites()
            allPhotos = allPhotosFromIntent.filter { favoriteIds.contains(it.id.toString()) }
        } else {
            allPhotos = intent.getSerializableExtra("all_photos") as? ArrayList<Photo> ?: emptyList()
        }
        currentIndex = allPhotos.indexOfFirst { it.id == currentPhoto.id }
        if (currentIndex == -1) currentIndex = 0
        initViews()
        setupGestureDetector()
        loadPhotoDetails()
        updateFavoriteButton()
        favoriteButton.setOnClickListener {
            toggleFavorite()
        }
        closeButton.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        zoomImageView.setOnClickListener {
            toggleInfoVisibility()
        }
        setupInitialZoom()
    }
    private fun initViews() {
        zoomImageView = findViewById(R.id.zoomImageView)
        infoLayout = findViewById(R.id.infoLayout)
        filenameTextView = findViewById(R.id.filenameTextView)
        sizeTextView = findViewById(R.id.sizeTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        tagsTextView = findViewById(R.id.tagsTextView)
        favoriteButton = findViewById(R.id.favoriteButton)
        closeButton = findViewById(R.id.closeButton)
    }
    private fun setupGestureDetector() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val swipeThreshold = 100
                val velocityThreshold = 1000
                try {
                    val diffY = e2.y - (e1?.y ?: 0f)
                    val diffX = e2.x - (e1?.x ?: 0f)
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > swipeThreshold && Math.abs(velocityX) > velocityThreshold) {
                            if (diffX > 0) {
                                showPreviousPhoto()
                            } else {
                                showNextPhoto()
                            }
                            return true
                        }
                    } else {
                        if (Math.abs(diffY) > swipeThreshold && Math.abs(velocityY) > velocityThreshold) {
                            if (diffY > 0) {
                                finish()
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            }
                            return true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
        })
        zoomImageView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }
    private fun setupInitialZoom() {
        zoomImageView.post {
            zoomImageView.resetZoom()
        }
    }
    private fun loadPhotoDetails() {
        Glide.with(this)
            .load(currentPhoto.getImageUrl())
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error)
            .into(zoomImageView)
        filenameTextView.text = currentPhoto.filename
        sizeTextView.text = getString(R.string.size) + " " + currentPhoto.getFormattedSize()
        descriptionTextView.text = getString(R.string.description) + " " + (currentPhoto.description ?: getString(R.string.no_description))
        val tags = currentPhoto.tags?.joinToString(", ") ?: getString(R.string.no_tags)
        tagsTextView.text = getString(R.string.tags) + " " + tags
        zoomImageView.resetZoom()
    }
    private fun toggleFavorite() {
        if (prefs.isFavorite(currentPhoto.id)) {
            prefs.removeFavorite(currentPhoto.id)
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                ImageUtils.deleteFavoriteImage(currentPhoto.id)
            }
            Toast.makeText(this, "Удалено из избранного", Toast.LENGTH_SHORT).show()
            if (isFromFavorites) {
                val favoriteIds = prefs.getFavorites()
                allPhotos = allPhotos.filter { favoriteIds.contains(it.id.toString()) }
                if (allPhotos.isEmpty()) {
                    finish()
                    return
                }
                if (currentIndex >= allPhotos.size) {
                    currentIndex = allPhotos.size - 1
                }
                if (currentIndex >= 0 && currentIndex < allPhotos.size) {
                    currentPhoto = allPhotos[currentIndex]
                    loadPhotoDetails()
                }
            }
        } else {
            prefs.addFavorite(currentPhoto.id)
            if (prefs.isAutoDownloadFavorites()) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    val success = ImageUtils.downloadFavoriteImage(
                        this@PhotoDetailActivity,
                        currentPhoto
                    )
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (success) {
                            Toast.makeText(
                                this@PhotoDetailActivity,
                                "Фото добавлено в избранное и скачано",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@PhotoDetailActivity,
                                "Фото добавлено в избранное (ошибка скачивания)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Фото добавлено в избранное", Toast.LENGTH_SHORT).show()
            }
        }
        updateFavoriteButton()
    }
    private fun updateFavoriteButton() {
        val drawable = if (prefs.isFavorite(currentPhoto.id)) {
            R.drawable.ic_favorite_filled
        } else {
            R.drawable.ic_favorite_border
        }
        favoriteButton.setImageResource(drawable)
    }
    private fun toggleInfoVisibility() {
        if (infoLayout.visibility == View.VISIBLE) {
            infoLayout.visibility = View.GONE
        } else {
            infoLayout.visibility = View.VISIBLE
        }
    }
    private fun showNextPhoto() {
        if (currentIndex < allPhotos.size - 1) {
            currentIndex++
            currentPhoto = allPhotos[currentIndex]
            loadPhotoDetails()
            updateFavoriteButton()
            zoomImageView.resetZoom()
        } else {
            Toast.makeText(this, "Это последняя фотография", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showPreviousPhoto() {
        if (currentIndex > 0) {
            currentIndex--
            currentPhoto = allPhotos[currentIndex]
            loadPhotoDetails()
            updateFavoriteButton()
            zoomImageView.resetZoom()
        } else {
            Toast.makeText(this, "Это первая фотография", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
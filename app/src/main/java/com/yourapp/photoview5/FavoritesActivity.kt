    package com.yourapp.photoview5
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yourapp.photoview5.adapter.FavoritesAdapter
import com.yourapp.photoview5.model.Photo
import com.yourapp.photoview5.network.ApiClient
import com.yourapp.photoview5.utils.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
@Suppress("DEPRECATION")
class FavoritesActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var backButton: ImageButton
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var prefs: PreferenceHelper
    private var allPhotos = mutableListOf<Photo>()
    private var favoritePhotos = mutableListOf<Photo>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceHelper(this)
        val theme = prefs.getTheme()
        if (theme == "dark") {
            setTheme(R.style.Theme_PhotoViewer_Dark)
        } else {
            setTheme(R.style.Theme_PhotoViewer_Light)
        }
        setContentView(R.layout.activity_favorites)
        initViews()
        setupRecyclerView()
        loadAllPhotos()
        backButton.setOnClickListener {
            onBackPressed()
        }
    }
    private fun initViews() {
        recyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyTextView = findViewById(R.id.emptyTextView)
        backButton = findViewById(R.id.backButton)
    }
    private fun setupRecyclerView() {
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        favoritesAdapter = FavoritesAdapter(
            favoritePhotos,
            onItemClick = { photo ->
                val intent = android.content.Intent(this, PhotoDetailActivity::class.java)
                intent.putExtra("photo", photo)
                intent.putExtra("all_photos", ArrayList(allPhotos))
                intent.putExtra("from_favorites", true) 
                startActivity(intent)
            },
            onFavoriteClick = { photo ->
                prefs.removeFavorite(photo.id)
                updateFavorites()
            }
        )
        recyclerView.adapter = favoritesAdapter
    }
    private fun loadAllPhotos() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getAllPhotos()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.data?.let { photoList ->
                            allPhotos.clear()
                            allPhotos.addAll(photoList)
                            updateFavorites()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    emptyTextView.visibility = android.view.View.VISIBLE
                    recyclerView.visibility = android.view.View.GONE
                }
            }
        }
    }
    private fun updateFavorites() {
        val favoriteIds = prefs.getFavorites()
        favoritePhotos.clear()
        favoritePhotos.addAll(allPhotos.filter { favoriteIds.contains(it.id.toString()) })
        favoritesAdapter.updateFavorites(favoritePhotos)
        if (favoritePhotos.isEmpty()) {
            emptyTextView.visibility = android.view.View.VISIBLE
            recyclerView.visibility = android.view.View.GONE
        } else {
            emptyTextView.visibility = android.view.View.GONE
            recyclerView.visibility = android.view.View.VISIBLE
        }
    }
    override fun onResume() {
        super.onResume()
        updateFavorites()
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
package com.yourapp.photoview5.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.yourapp.photoview5.R
import com.yourapp.photoview5.model.Photo
import com.yourapp.photoview5.utils.PreferenceHelper
class PhotoAdapter(
    private var photos: List<Photo> = emptyList(),
    private val onItemClick: (Photo) -> Unit,
    private val onFavoriteClick: (Photo) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {
    private lateinit var prefs: PreferenceHelper
    fun setPreferenceHelper(preferenceHelper: PreferenceHelper) {
        this.prefs = preferenceHelper
    }
    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        val titleTextView: TextView = itemView.findViewById(R.id.photoTitleTextView)
        val sizeTextView: TextView = itemView.findViewById(R.id.photoSizeTextView)
        val favoriteButton: ImageButton = itemView.findViewById(R.id.favoriteButton)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]
        holder.titleTextView.text = photo.filename
        holder.sizeTextView.text = photo.getFormattedSize()
        Glide.with(holder.itemView.context)
            .load(photo.getImageUrl())
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error)
            .into(holder.imageView)
        val isFavorite = if (::prefs.isInitialized) prefs.isFavorite(photo.id) else false
        updateFavoriteButton(holder.favoriteButton, isFavorite)
        holder.itemView.setOnClickListener {
            onItemClick(photo)
        }
        holder.favoriteButton.setOnClickListener {
            onFavoriteClick(photo)
            val newFavoriteState = !isFavorite
            updateFavoriteButton(holder.favoriteButton, newFavoriteState)
        }
    }
    override fun getItemCount(): Int = photos.size
    fun updatePhotos(newPhotos: List<Photo>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
    private fun updateFavoriteButton(button: ImageButton, isFavorite: Boolean) {
        val drawable = if (isFavorite) {
            R.drawable.ic_favorite_filled
        } else {
            R.drawable.ic_favorite_border
        }
        button.setImageResource(drawable)
        val context = button.context
        val isDarkTheme = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val color = if (isDarkTheme) {
            if (isFavorite) android.R.color.holo_red_light else android.R.color.darker_gray
        } else {
            if (isFavorite) android.R.color.holo_red_dark else android.R.color.darker_gray
        }
        button.setColorFilter(
            context.resources.getColor(color, context.theme),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }
}
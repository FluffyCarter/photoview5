package com.yourapp.photoview5.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.yourapp.photoview5.R
import com.yourapp.photoview5.model.Photo
class FavoritesAdapter(
    private var favorites: List<Photo> = emptyList(),
    private val onItemClick: (Photo) -> Unit,
    private val onFavoriteClick: (Photo) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {
    class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.favoriteImageView)
        val favoriteButton: ImageView = itemView.findViewById(R.id.favoriteButton)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view)
    }
    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        val photo = favorites[position]
        Glide.with(holder.itemView.context)
            .load(photo.getImageUrl())
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error)
            .into(holder.imageView)
        holder.favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
        holder.imageView.setOnClickListener {
            onItemClick(photo)
        }
        holder.favoriteButton.setOnClickListener {
            onFavoriteClick(photo)
        }
    }
    override fun getItemCount(): Int = favorites.size
    fun updateFavorites(newFavorites: List<Photo>) {
        favorites = newFavorites
        notifyDataSetChanged()
    }
}
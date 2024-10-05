package com.example.auction_gu_lee.Chat

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R

class ImagePreviewAdapter(
    private val imageUris: List<Uri>,
    private val onDeleteClick: (Uri) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_preview)
        val deleteButton: ImageButton = view.findViewById(R.id.button_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_image_preview, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = imageUris[position]
        Glide.with(holder.imageView.context)
            .load(uri)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.imageView)

        holder.deleteButton.setOnClickListener {
            onDeleteClick(uri)
        }
    }

    override fun getItemCount(): Int {
        return imageUris.size
    }
}
package com.example.auction_gu_lee.Board

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Auction

class AuctionSelectionAdapter(
    private val auctionList: List<Auction>,
    private val onItemClick: (Auction) -> Unit
) : RecyclerView.Adapter<AuctionSelectionAdapter.AuctionViewHolder>() {

    inner class AuctionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemImageView: ImageView = itemView.findViewById(R.id.imageView_photo)
        val itemNameTextView: TextView = itemView.findViewById(R.id.textView_item)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(auctionList[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuctionViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auction_selection, parent, false)
        return AuctionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AuctionViewHolder, position: Int) {
        val auction = auctionList[position]
        holder.itemNameTextView.text = auction.item

        // 이미지 로딩
        Glide.with(holder.itemView.context)
            .load(auction.photoUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.error_image)
            .into(holder.itemImageView)
    }

    override fun getItemCount(): Int = auctionList.size
}

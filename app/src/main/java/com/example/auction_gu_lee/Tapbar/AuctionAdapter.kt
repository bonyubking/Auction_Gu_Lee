package com.example.auction_gu_lee.Tapbar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Auction

class AuctionAdapter(private val auctionList: List<Auction>) :
    RecyclerView.Adapter<AuctionAdapter.AuctionViewHolder>() {

    class AuctionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemTextView: TextView = view.findViewById(R.id.textView_item)
        val detailTextView: TextView = view.findViewById(R.id.textView_detail)
        val priceTextView: TextView = view.findViewById(R.id.textView_price)
        val photoImageView: ImageView = view.findViewById(R.id.imageView_photo)
        val quantityTextView: TextView = view.findViewById(R.id.textView_quantity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuctionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auction, parent, false)
        return AuctionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AuctionViewHolder, position: Int) {
        val auction = auctionList[position]
        holder.itemTextView.text = auction.item
        holder.detailTextView.text = auction.detail
        holder.priceTextView.text = auction.startingPrice
        holder.quantityTextView.text = auction.quantity

        // Glide로 사진 URL을 ImageView에 로드
        Glide.with(holder.photoImageView.context)
            .load(auction.photoUrl)
            .placeholder(R.drawable.placehoder_image) // 이미지 로딩 중일 때 표시할 기본 이미지
            .error(R.drawable.error_image) // 오류 시 표시할 이미지
            .into(holder.photoImageView)
    }

    override fun getItemCount(): Int = auctionList.size
}

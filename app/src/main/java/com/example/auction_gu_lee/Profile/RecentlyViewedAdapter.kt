package com.example.auction_gu_lee.Profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ItemRecentlyViewedBinding
import com.example.auction_gu_lee.models.Auction

class RecentlyViewedAdapter(
    private val auctionList: MutableList<Auction>,
    private val itemClickListener: (String) -> Unit
) : RecyclerView.Adapter<RecentlyViewedAdapter.RecentlyViewedViewHolder>() {

    class RecentlyViewedViewHolder(val binding: ItemRecentlyViewedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentlyViewedViewHolder {
        val binding = ItemRecentlyViewedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecentlyViewedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentlyViewedViewHolder, position: Int) {
        val auctionItem = auctionList[position]

        // 데이터 설정
        holder.binding.textViewItem.text = auctionItem.item ?: "항목 없음"
        holder.binding.textViewStartingPrice.text = "${auctionItem.startingPrice ?: 0}₩"
        holder.binding.textViewHighestPrice.text = "${auctionItem.highestPrice ?: 0}₩"

        // 남은 시간 설정
        val endTime = auctionItem.endTime
        if (endTime != null) {
            val remainingTime = endTime - System.currentTimeMillis()
            if (remainingTime > 0) {
                val hours = (remainingTime / (1000 * 60 * 60)) % 24
                val minutes = (remainingTime / (1000 * 60)) % 60
                val seconds = (remainingTime / 1000) % 60
                holder.binding.textViewRemainingTime.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                holder.binding.textViewRemainingTime.text = "경매 종료"
            }
        }

        // 이미지 로드
        Glide.with(holder.binding.imageViewPhoto.context)
            .load(auctionItem.photoUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.binding.imageViewPhoto)

        holder.itemView.setOnClickListener {
            auctionItem.id?.let { auctionId ->
                itemClickListener(auctionId)
            }
        }
    }

    override fun getItemCount(): Int = auctionList.size
}
package com.example.auction_gu_lee.Profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ItemWishlistBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class WishlistAdapter(
    private val auctionList: MutableList<Auction>,
    private val itemClickListener: (String) -> Unit
) : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WishlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        val auctionItem = auctionList[position]

        // 데이터 설정
        holder.binding.textViewItem.text = auctionItem.item ?: "항목 없음"
        holder.binding.textViewStartingPrice.text = "${auctionItem.startingPrice ?: 0}₩"
        holder.binding.textViewHighestPrice.text = "${auctionItem.highestPrice ?: 0}₩"
        holder.binding.textViewRemainingTime.text = auctionItem.remainingTime ?: "시간 정보 없음"

        // Glide를 사용하여 이미지 로드
        Glide.with(holder.binding.imageViewPhoto.context)
            .load(auctionItem.photoUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.binding.imageViewPhoto)

        // 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            auctionItem.id?.let { auctionId ->
                itemClickListener(auctionId)
            }
        }



    }

    override fun getItemCount(): Int = auctionList.size

    class WishlistViewHolder(val binding: ItemWishlistBinding) : RecyclerView.ViewHolder(binding.root)
}

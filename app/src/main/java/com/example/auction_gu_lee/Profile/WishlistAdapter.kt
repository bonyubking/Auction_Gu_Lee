package com.example.auction_gu_lee.Profile

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ItemWishlistBinding
import com.example.auction_gu_lee.models.Auction

class WishlistAdapter(
    private val auctionList: MutableList<Auction>,
    private val itemClickListener: (String) -> Unit
) : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

    class WishlistViewHolder(val binding: ItemWishlistBinding) : RecyclerView.ViewHolder(binding.root) {
        var countDownTimer: CountDownTimer? = null  // CountDownTimer 추가
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WishlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        val auctionItem = auctionList[position]

        // 데이터 설정
        holder.binding.textViewItem.text = auctionItem.item ?: "항목 없음"
        holder.binding.textViewStartingPrice.text = "${auctionItem.startingPrice ?: 0}₩"

        // 최고 가격이 시작 가격보다 높을 경우 빨간색, 그렇지 않으면 검은색으로 표시
        val highestPrice = auctionItem.highestPrice ?: auctionItem.startingPrice ?: 0L
        holder.binding.textViewHighestPrice.text = "$highestPrice ₩"

        if (highestPrice > (auctionItem.startingPrice ?: 0L)) {
            holder.binding.textViewHighestPrice.setTextColor(android.graphics.Color.RED)
        } else {
            holder.binding.textViewHighestPrice.setTextColor(android.graphics.Color.BLACK)
        }

        // CountDownTimer가 이미 있으면 취소
        holder.countDownTimer?.cancel()

        // 경매 종료 시간을 Long으로 처리 (endTime이 null일 수 있으므로 처리)
        val endTime = auctionItem.endTime
        if (endTime != null) {
            val currentTime = System.currentTimeMillis()
            val remainingTime = endTime - currentTime

            // 남은 시간이 0보다 크면 카운트다운 시작
            if (remainingTime > 0) {
                holder.countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val days = millisUntilFinished / (1000 * 60 * 60 * 24)
                        val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                        val minutes = (millisUntilFinished / (1000 * 60)) % 60
                        val seconds = (millisUntilFinished / 1000) % 60

                        // 남은 시간을 실시간으로 업데이트
                        holder.binding.textViewRemainingTime.text = String.format(
                            "%02d:%02d:%02d:%02d",
                            days, hours, minutes, seconds
                        )

                        // 남은 시간이 24시간 이내인 경우 빨간색으로 표시
                        if (millisUntilFinished <= 24 * 60 * 60 * 1000) {
                            holder.binding.textViewRemainingTime.setTextColor(android.graphics.Color.RED)
                        } else {
                            holder.binding.textViewRemainingTime.setTextColor(android.graphics.Color.BLACK)
                        }
                    }

                    override fun onFinish() {
                        holder.binding.textViewRemainingTime.text = "경매 종료"
                    }
                }.start()
            } else {
                holder.binding.textViewRemainingTime.text = "경매 종료"
            }
        }

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
}

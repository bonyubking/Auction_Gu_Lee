package com.example.auction_gu_lee.Profile

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ItemBiddingHistoryBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.auth.FirebaseAuth

class BiddingHistoryAdapter(
    private val auctionList: MutableList<Auction>,
    private val itemClickListener: (String) -> Unit
) : RecyclerView.Adapter<BiddingHistoryAdapter.BiddingHistoryViewHolder>() {

    class BiddingHistoryViewHolder(val binding: ItemBiddingHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        var countDownTimer: CountDownTimer? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BiddingHistoryViewHolder {
        val binding = ItemBiddingHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BiddingHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BiddingHistoryViewHolder, position: Int) {
        val auctionItem = auctionList[position]

        // 데이터 설정
        holder.binding.textViewItem.text = auctionItem.item ?: "항목 없음"
        holder.binding.textViewStartingPrice.text = "${auctionItem.startingPrice ?: 0}₩"
        holder.binding.textViewHighestPrice.text = "${auctionItem.highestPrice ?: 0}₩"

        // 현재 사용자가 입찰한 금액 가져오기
        val userBidAmount = auctionItem.participants[FirebaseAuth.getInstance().currentUser?.uid]

        // 사용자의 최종 입찰 금액과 최고가를 비교하여 상태 업데이트
        if (userBidAmount != null) {
            if (userBidAmount < (auctionItem.highestPrice ?: 0L)) {
                holder.binding.textViewBidStatus.text = "최고가 갱신"
                holder.binding.textViewBidStatus.setTextColor(android.graphics.Color.RED)
            } else {
                holder.binding.textViewBidStatus.text = "최고가 유지"
                holder.binding.textViewBidStatus.setTextColor(android.graphics.Color.GREEN)
            }
        } else {
            holder.binding.textViewBidStatus.text = "입찰 없음"
            holder.binding.textViewBidStatus.setTextColor(android.graphics.Color.GRAY)
        }

        // 남은 시간 설정
        holder.countDownTimer?.cancel()
        val endTime = auctionItem.endTime
        if (endTime != null) {
            val currentTime = System.currentTimeMillis()
            val remainingTime = endTime - currentTime

            if (remainingTime > 0) {
                holder.countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val days = millisUntilFinished / (1000 * 60 * 60 * 24)
                        val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                        val minutes = (millisUntilFinished / (1000 * 60)) % 60
                        val seconds = (millisUntilFinished / 1000) % 60

                        holder.binding.textViewRemainingTime.text = String.format(
                            "%02d:%02d:%02d:%02d",
                            days, hours, minutes, seconds
                        )

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

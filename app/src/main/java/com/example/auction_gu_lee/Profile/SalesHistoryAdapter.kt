package com.example.auction_gu_lee.Profile

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ItemSalesHistoryBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.database.*

class SalesHistoryAdapter(
    private val auctionList: MutableList<Auction>,
    private val itemClickListener: (String) -> Unit
) : RecyclerView.Adapter<SalesHistoryAdapter.SalesHistoryViewHolder>() {

    private val database = FirebaseDatabase.getInstance()
    private val auctionRef = database.getReference("auctions")

    class SalesHistoryViewHolder(val binding: ItemSalesHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        var countDownTimer: CountDownTimer? = null
        var currentListener: ValueEventListener? = null
        var auctionId: String? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesHistoryViewHolder {
        val binding = ItemSalesHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SalesHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SalesHistoryViewHolder, position: Int) {
        val auctionItem = auctionList[position]

        // 기존 리스너가 있으면 제거하고 뷰 홀더의 auctionId가 동일할 때만 삭제
        holder.currentListener?.let {
            holder.auctionId?.let { prevAuctionId ->
                auctionRef.child(prevAuctionId).child("highestPrice").removeEventListener(it)
            }
        }

        holder.auctionId = auctionItem.id

        // 데이터 설정
        holder.binding.textViewItem.text = auctionItem.item ?: "항목 없음"
        holder.binding.textViewStartingPrice.text = "${auctionItem.startingPrice ?: 0}₩"

        // 새로운 리스너 설정
        auctionItem.id?.let { auctionId ->
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val highestPrice = snapshot.getValue(Long::class.java) ?: auctionItem.startingPrice ?: 0L
                    holder.binding.textViewHighestPrice.text = "$highestPrice ₩"

                    if (highestPrice > (auctionItem.startingPrice ?: 0L)) {
                        holder.binding.textViewHighestPrice.setTextColor(android.graphics.Color.RED)
                    } else {
                        holder.binding.textViewHighestPrice.setTextColor(android.graphics.Color.BLACK)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // 에러 처리 (옵션)
                }
            }

            auctionRef.child(auctionId).child("highestPrice").addValueEventListener(listener)
            holder.currentListener = listener
        }

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
                        holder.binding.textViewSalesStatus.text = "판매 완료" // 판매 상태 업데이트
                    }
                }.start()
            } else {
                holder.binding.textViewRemainingTime.text = "경매 종료"
                holder.binding.textViewSalesStatus.text = "판매 완료" // 판매 상태 업데이트
            }
        }

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

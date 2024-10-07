package com.example.auction_gu_lee.Profile

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ItemWishlistBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.*

class WishlistAdapter(
    private val auctionList: MutableList<Auction>,
    private val itemClickListener: (String) -> Unit
) : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

    private val database = FirebaseDatabase.getInstance()
    private val auctionRef = database.getReference("auctions")

    class WishlistViewHolder(val binding: ItemWishlistBinding) : RecyclerView.ViewHolder(binding.root) {
        var countDownTimer: CountDownTimer? = null
        var currentListener: ValueEventListener? = null  // 현재 리스너를 추적
        var auctionId: String? = null // 현재 아이템의 ID 추적
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WishlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        val auctionItem = auctionList[position]

        // 기존 리스너가 있으면 제거하고 뷰 홀더의 auctionId가 동일할 때만 삭제
        holder.currentListener?.let {
            holder.auctionId?.let { prevAuctionId ->
                auctionRef.child(prevAuctionId).child("highestPrice").removeEventListener(it)
            }
        }

        // 새로운 아이템의 ID 설정
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

            // 리스너 등록 및 현재 리스너로 저장
            auctionRef.child(auctionId).child("highestPrice").addValueEventListener(listener)
            holder.currentListener = listener
        }

        // 카운트다운 타이머가 이미 있으면 취소
        holder.countDownTimer?.cancel()

        // 경매 종료 시간을 처리
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

package com.example.auction_gu_lee.Profile.MyAuction

import android.graphics.Rect
import android.icu.text.NumberFormat
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.databinding.DataBindingUtil.setContentView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ItemBiddingHistoryBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import java.util.Locale

class BiddingHistoryAdapter(
    private val auctionList: MutableList<Auction>,
    private var isRebidWarningEnabled: Boolean,
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


        // 버튼 클릭 영역 확장
        expandButtonTouchArea(holder.binding.btnRebid)

        // 재입찰 버튼 클릭 리스너 설정
        holder.binding.btnRebid.setOnClickListener {
            if (isRebidWarningEnabled) {
                showRebidWarningDialog(holder, auctionItem)
            } else {
                placeBid(auctionItem, holder)
            }
        }

        // 숫자 형식 포맷을 위한 NumberFormat 객체 생성 (콤마 추가)
        val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

        // 데이터 설정 (콤마 추가)
        holder.binding.textViewItem.text = auctionItem.item ?: "항목 없음"
        holder.binding.textViewStartingPrice.text = "${numberFormat.format(auctionItem.startingPrice ?: 0)}₩"
        holder.binding.textViewHighestPrice.text = "${numberFormat.format(auctionItem.highestPrice ?: 0)}₩"

        // 사용자 입찰 상태 업데이트
        updateUserBidStatus(holder, auctionItem)

        // 남은 시간 표시
        updateRemainingTime(holder, auctionItem)

        // 이미지 로드
        Glide.with(holder.binding.imageViewPhoto.context)
            .load(auctionItem.photoUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.binding.imageViewPhoto)

        // 아이템 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            auctionItem.id?.let { auctionId -> itemClickListener(auctionId) }
        }
    }

    override fun getItemCount(): Int = auctionList.size

    private fun expandButtonTouchArea(button: Button) {
        button.post {
            val parent = button.parent as View
            parent.post {
                val rect = Rect()
                button.getHitRect(rect)

                // 터치 영역 확장
                val additionalPadding = 15
                rect.top -= additionalPadding
                rect.bottom += additionalPadding
                rect.left -= additionalPadding
                rect.right += additionalPadding

                parent.touchDelegate = TouchDelegate(rect, button)
            }
        }
    }

    private fun updateUserBidStatus(holder: BiddingHistoryViewHolder, auctionItem: Auction) {
        val userBidAmount = auctionItem.participants[FirebaseAuth.getInstance().currentUser?.uid]

        if (userBidAmount != null) {
            if (userBidAmount < (auctionItem.highestPrice ?: 0L)) {
                holder.binding.textViewBidStatus.text = "최고가 갱신됨"
                holder.binding.textViewBidStatus.setTextColor(android.graphics.Color.RED)
            } else {
                holder.binding.textViewBidStatus.text = "내가 최고 가격"
                holder.binding.textViewBidStatus.setTextColor(android.graphics.Color.parseColor("#4da98d"))  // 여기서 색상을 #4da98d로 설정
            }
        } else {
            holder.binding.textViewBidStatus.text = "입찰 없음"
            holder.binding.textViewBidStatus.setTextColor(android.graphics.Color.GRAY)
        }
    }

    private fun updateRemainingTime(holder: BiddingHistoryViewHolder, auctionItem: Auction) {
        holder.countDownTimer?.cancel()

        val endTime = auctionItem.endTime
        if (endTime != null) {
            val remainingTime = endTime - System.currentTimeMillis()

            if (remainingTime > 0) {
                holder.countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val days = millisUntilFinished / (1000 * 60 * 60 * 24)
                        val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                        val minutes = (millisUntilFinished / (1000 * 60)) % 60
                        val seconds = (millisUntilFinished / 1000) % 60

                        holder.binding.textViewRemainingTime.text = when {
                            millisUntilFinished > 24 * 60 * 60 * 1000 -> {
                                String.format("%d일 남음", days)
                            }
                            millisUntilFinished in 60 * 60 * 1000..24 * 60 * 60 * 1000 -> {
                                String.format("%d시간 남음", hours)
                            }
                            else -> {
                                String.format("%02d:%02d", minutes, seconds)
                            }
                        }

                        holder.binding.textViewRemainingTime.setTextColor(
                            if (millisUntilFinished <= 24 * 60 * 60 * 1000) android.graphics.Color.RED
                            else android.graphics.Color.BLACK
                        )
                    }

                    override fun onFinish() {
                        holder.binding.textViewRemainingTime.text = "경매 종료"
                        holder.binding.textViewRemainingTime.setTextColor(android.graphics.Color.BLUE)
                    }
                }.start()
            } else {
                holder.binding.textViewRemainingTime.text = "경매 종료"
                holder.binding.textViewRemainingTime.setTextColor(android.graphics.Color.BLUE)
            }
        }
    }

    private fun showRebidWarningDialog(holder: BiddingHistoryViewHolder, auctionItem: Auction) {
        val context = holder.itemView.context
        android.app.AlertDialog.Builder(context)
            .setTitle("재입찰 확인")
            .setMessage("정말 ${auctionItem.item}에 재입찰하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                placeBid(auctionItem, holder)
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    private fun placeBid(auctionItem: Auction, holder: BiddingHistoryViewHolder) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        val increment = calculateBidIncrement(auctionItem.startingPrice ?: 0L)
        val newBidAmount = (auctionItem.highestPrice ?: auctionItem.startingPrice ?: 0L) + increment

        val auctionRef = FirebaseDatabase.getInstance().reference.child("auctions").child(auctionItem.id ?: return)

        auctionRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val auction = currentData.getValue(Auction::class.java) ?: return Transaction.success(currentData)
                auction.highestPrice = newBidAmount
                auction.highestBidderUid = userId
                auction.participants[userId] = newBidAmount
                currentData.value = auction
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (committed && error == null) {
                    // NumberFormat 객체를 이용하여 쉼표 추가
                    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
                    holder.binding.textViewHighestPrice.text = "${numberFormat.format(newBidAmount)}₩"
                    Toast.makeText(holder.itemView.context, "입찰 성공!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(holder.itemView.context, "입찰 실패: ${error?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun calculateBidIncrement(startingPrice: Long): Long {
        return when (startingPrice) {
            in 100L..499L -> 1
            in 500L..999L -> 5
            in 1000L..4999L -> 10
            in 5000L..9999L -> 50
            in 10000L..49999L -> 100
            in 50000L..99999L -> 500
            in 100000L..499999L -> 1000
            in 500000L..999999L -> 5000
            in 1000000L..4999999L -> 10000
            in 5000000L..9999999L -> 50000
            in 10000000L..49999999L -> 100000
            in 50000000L..99999999L -> 500000
            in 100000000L..499999999L -> 1000000
            in 500000000L..999999999L -> 5000000
            else -> startingPrice / 100
        }
    }

    fun setRebidWarningEnabled(isEnabled: Boolean) {
        this.isRebidWarningEnabled = isEnabled
    }
}

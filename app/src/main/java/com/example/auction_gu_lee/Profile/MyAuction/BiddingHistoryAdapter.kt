package com.example.auction_gu_lee.Profile.MyAuction

import android.graphics.Rect
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

        // btnRebid의 클릭 영역 확장
        holder.binding.btnRebid.post {
            val parent = holder.binding.btnRebid.parent as View // 버튼의 부모 레이아웃 가져오기
            parent.post {
                val rect = Rect()
                holder.binding.btnRebid.getHitRect(rect) // 버튼의 터치 영역 가져오기

                // 터치 영역을 20dp 확장 (위, 아래, 왼쪽, 오른쪽)
                val additionalPadding = 15
                rect.top -= additionalPadding
                rect.bottom += additionalPadding
                rect.left -= additionalPadding
                rect.right += additionalPadding

                // 부모 레이아웃에 TouchDelegate 설정
                parent.touchDelegate = TouchDelegate(rect, holder.binding.btnRebid)
            }
        }

        holder.binding.btnRebid.setOnClickListener {
            if (isRebidWarningEnabled) {
                // 경고 팝업 표시
                showRebidWarningDialog(holder, auctionItem)
            } else {
                // 즉시 재입찰
                placeBid(auctionItem, holder)
            }
        }

        // 데이터 설정
        holder.binding.textViewItem.text = auctionItem.item ?: "항목 없음"
        holder.binding.textViewStartingPrice.text = "${auctionItem.startingPrice ?: 0}₩"
        holder.binding.textViewHighestPrice.text = "${auctionItem.highestPrice ?: 0}₩"

        // 현재 사용자가 입찰한 금액 가져오기
        val userBidAmount = auctionItem.participants[FirebaseAuth.getInstance().currentUser?.uid]

        // 사용자의 최종 입찰 금액과 최고가를 비교하여 상태 업데이트
        if (userBidAmount != null) {
            if (userBidAmount < (auctionItem.highestPrice ?: 0L)) {
                holder.binding.textViewBidStatus.text = "최고가 갱신됨"
                holder.binding.textViewBidStatus.setTextColor(android.graphics.Color.RED)
            } else {
                holder.binding.textViewBidStatus.text = "내가 최고 가격"
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

    private fun showRebidWarningDialog(holder: BiddingHistoryViewHolder, auctionItem: Auction) {
        val context = holder.itemView.context
        android.app.AlertDialog.Builder(context)
            .setTitle("재입찰 확인")
            .setMessage("정말로 ${auctionItem.item}에 재입찰하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                placeBid(auctionItem, holder)
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    fun setRebidWarningEnabled(isEnabled: Boolean) {
        this.isRebidWarningEnabled = isEnabled
    }

    private fun placeBid(auctionItem: Auction, holder: BiddingHistoryViewHolder) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        // 입찰 증가폭 계산
        val increment = when (auctionItem.startingPrice) {
            in 100L..499L -> 1
            in 500L..999L -> 5
            in 1000L..4999L -> 10
            in 5000L..9999L -> 50
            in 10000L..49999L -> 100
            in 50000L..99999L -> 500
            in 100000L..499999L -> 1000
            in 500000L..999999L -> 5000
            in 1000000L..4999999L-> 10000
            in 5000000L..9999999L -> 50000
            in 10000000L..49999999L -> 100000
            in 50000000L..99999999L -> 500000
            in 100000000L..499999999L -> 1000000
            in 500000000L..999999999L -> 5000000
            in 1000000000..4999999999 -> 10000000
            in 5000000000..9999999999 -> 50000000
            in 10000000000..49999999999 -> 100000000
            in 50000000000..99999999999 -> 500000000
            in 100000000000..499999999999 -> 1000000000
            in 500000000000..999999999999 -> 5000000000
            in 1000000000000..4999999999999 -> 10000000000
            in 5000000000000..9999999999999 -> 50000000000
            in 10000000000000..100000000000000 -> 100000000000
            else -> auctionItem.startingPrice?.div(100) ?: 1000 // 시작가의 1% 또는 기본값 1000원
        }

        val newBidAmount = (auctionItem.highestPrice ?: auctionItem.startingPrice ?: 0) + increment

        val auctionRef = FirebaseDatabase.getInstance().reference.child("auctions").child(auctionItem.id ?: return)

        // Firebase의 트랜잭션을 사용하여 입찰 업데이트
        auctionRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val auction = currentData.getValue(Auction::class.java)
                    ?: return Transaction.success(currentData)

                // 입찰자 업데이트
                auction.highestPrice = newBidAmount
                auction.highestBidderUid = userId

                // 참가자 목록 업데이트
                auction.participants[userId] = newBidAmount

                currentData.value = auction
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error == null && committed) {
                    holder.binding.textViewHighestPrice.text = "${newBidAmount}₩"
                    Toast.makeText(holder.itemView.context, "입찰 성공!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(holder.itemView.context, "입찰 실패: ${error?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }



    override fun getItemCount(): Int = auctionList.size
}

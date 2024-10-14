package com.example.auction_gu_lee.Board

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Auction
import com.example.auction_gu_lee.models.Comment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class CommentAdapter(
    private val commentList: List<Comment>,
    private val currentUserUid: String,  // 현재 로그인한 사용자 UID
    private val onDeleteClickListener: (Comment) -> Unit,  // 삭제 클릭 리스너 추가
    private val onAuctionClickListener: (Auction) -> Unit  // 경매 클릭 리스너 추가
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageViewPhoto: ImageView = view.findViewById(R.id.imageView_photo)  // ImageView 추가
        val textViewAuctionItem: TextView = view.findViewById(R.id.textView_item)
        val textViewStartingPrice: TextView = view.findViewById(R.id.textView_startingPrice)
        val textViewHighestPrice: TextView = view.findViewById(R.id.textView_highestPrice)
        val textViewRemainingTime: TextView = view.findViewById(R.id.textView_remainingTime)
        val buttonDeleteComment: ImageButton = view.findViewById(R.id.button_delete_comment)
        var countDownTimer: CountDownTimer? = null  // CountDownTimer 추가
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]

        // auctionId로 해당 경매 정보를 로드하고 표시
        val auctionId = comment.auctionId
        if (!auctionId.isNullOrEmpty()) {
            loadAuctionData(auctionId, holder)
        } else {
            holder.textViewAuctionItem.text = "경매 ID가 올바르지 않습니다."
            holder.imageViewPhoto.setImageResource(R.drawable.error_image)
            holder.textViewStartingPrice.text = ""
            holder.textViewHighestPrice.text = ""
            holder.textViewRemainingTime.text = ""
        }

        // 댓글 작성자가 현재 사용자와 동일할 때만 삭제 버튼 표시
        if (comment.userId == currentUserUid) {
            holder.buttonDeleteComment.visibility = View.VISIBLE
            holder.buttonDeleteComment.setOnClickListener {
                onDeleteClickListener(comment)
            }
        } else {
            holder.buttonDeleteComment.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = commentList.size

    private fun loadAuctionData(auctionId: String, holder: CommentViewHolder) {
        val auctionRef = FirebaseDatabase.getInstance().reference.child("auctions").child(auctionId)

        auctionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val auction = snapshot.getValue(Auction::class.java)
                if (auction != null) {
                    auction.id = auctionId  // auction.id 설정

                    // 경매 항목 및 이미지 표시
                    holder.textViewAuctionItem.text = auction.item

                    val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
                    holder.textViewStartingPrice.text = "${numberFormat.format(auction.startingPrice ?: 0)}₩"
                    holder.textViewHighestPrice.text = "${numberFormat.format(auction.highestPrice ?: 0)}₩"

                    // 이미지 로드
                    Glide.with(holder.imageViewPhoto.context)
                        .load(auction.photoUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.error_image)
                        .into(holder.imageViewPhoto)

                    // 실시간 남은 시간 갱신
                    startCountDownTimer(holder, auction.endTime ?: 0)

                    // 경매 클릭 리스너 설정
                    holder.itemView.setOnClickListener {
                        onAuctionClickListener(auction)
                    }
                } else {
                    holder.textViewAuctionItem.text = "경매 정보를 불러올 수 없습니다."
                    holder.imageViewPhoto.setImageResource(R.drawable.error_image)
                    holder.textViewStartingPrice.text = ""
                    holder.textViewHighestPrice.text = ""
                    holder.textViewRemainingTime.text = ""
                }
            }

            override fun onCancelled(error: DatabaseError) {
                holder.textViewAuctionItem.text = "경매 정보를 불러올 수 없습니다."
                holder.imageViewPhoto.setImageResource(R.drawable.error_image)
                holder.textViewStartingPrice.text = ""
                holder.textViewHighestPrice.text = ""
                holder.textViewRemainingTime.text = ""
            }
        })
    }

    // CountDownTimer를 사용하여 실시간으로 남은 시간을 표시
    private fun startCountDownTimer(holder: CommentViewHolder, endTime: Long) {
        holder.countDownTimer?.cancel()  // 이전 타이머가 있다면 취소
        val remainingTime = endTime - System.currentTimeMillis()

        if (remainingTime > 0) {
            holder.countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                    val minutes = (millisUntilFinished / (1000 * 60)) % 60
                    val seconds = (millisUntilFinished / 1000) % 60
                    holder.textViewRemainingTime.text = String.format("%02d:%02d:%02d 남음", hours, minutes, seconds)
                }

                override fun onFinish() {
                    holder.textViewRemainingTime.text = "경매 종료"
                }
            }.start()
        } else {
            holder.textViewRemainingTime.text = "경매 종료"
        }
    }
}

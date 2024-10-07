package com.example.auction_gu_lee.Tapbar

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Auction

class AuctionAdapter(
    private var auctionList: MutableList<Auction>,
    private val itemClickListener: (Auction) -> Unit  // 아이템 클릭 리스너 추가
) : RecyclerView.Adapter<AuctionAdapter.AuctionViewHolder>() {

    class AuctionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImageView: ImageView = view.findViewById(R.id.imageView_photo)
        val itemTextView: TextView = view.findViewById(R.id.textView_item)
        val priceTextView: TextView = view.findViewById(R.id.textView_startingPrice)
        val highestPriceTextView: TextView = view.findViewById(R.id.textView_highestPrice)
        val remainingTimeTextView: TextView = view.findViewById(R.id.textView_remainingTime)
        var countDownTimer: CountDownTimer? = null  // CountDownTimer를 추가
    }

    fun updateList(newList: List<Auction>) {
        auctionList.clear()  // 기존 리스트 초기화
        auctionList.addAll(newList)  // 새로운 리스트 추가
        notifyDataSetChanged()  // 어댑터에 데이터가 변경되었음을 알림
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuctionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_auction, parent, false)
        return AuctionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AuctionViewHolder, position: Int) {
        val auction = auctionList[position]
        holder.itemTextView.text = auction.item
        holder.priceTextView.text = "${auction.startingPrice}₩"

        // 최고 가격 표시 및 색상 변경
        val highestPrice = auction.highestPrice ?: auction.startingPrice ?: 0L
        holder.highestPriceTextView.text = "$highestPrice ₩"

        // 최고 가격이 시작 가격보다 높을 경우 빨간색으로 표시
        if (highestPrice > (auction.startingPrice ?: 0L)) {
            holder.highestPriceTextView.setTextColor(android.graphics.Color.RED)
        } else {
            holder.highestPriceTextView.setTextColor(android.graphics.Color.BLACK)
        }

        // CountDownTimer가 이미 있으면 취소
        holder.countDownTimer?.cancel()

        // 경매 종료 시간을 Long으로 처리 (endTime이 null일 수 있으므로 처리)
        val endTime = auction.endTime
        if (endTime != null) {
            val currentTime = System.currentTimeMillis()
            val remainingTime = endTime - currentTime

            // 만약 남은 시간이 0보다 크면 카운트다운 시작
            if (remainingTime > 0) {
                holder.countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val days = millisUntilFinished / (1000 * 60 * 60 * 24)
                        val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                        val minutes = (millisUntilFinished / (1000 * 60)) % 60
                        val seconds = (millisUntilFinished / 1000) % 60

                        // 남은 시간을 실시간으로 업데이트
                        holder.remainingTimeTextView.text = String.format(
                            "%02d:%02d:%02d:%02d",
                            days, hours, minutes, seconds
                        )

                        // 남은 시간이 24시간 이내인 경우 빨간색으로 표시
                        if (millisUntilFinished <= 24 * 60 * 60 * 1000) {
                            holder.remainingTimeTextView.setTextColor(android.graphics.Color.RED)
                        } else {
                            holder.remainingTimeTextView.setTextColor(android.graphics.Color.BLACK)
                        }
                    }

                    override fun onFinish() {
                        holder.remainingTimeTextView.text = "경매 종료"
                    }
                }.start()
            } else {
                holder.remainingTimeTextView.text = "경매 종료"
            }
        }

        // Glide로 사진 URL을 ImageView에 로드
        Glide.with(holder.photoImageView.context)
            .load(auction.photoUrl)
            .placeholder(R.drawable.placehoder_image) // 이미지 로딩 중일 때 표시할 기본 이미지
            .error(R.drawable.error_image) // 오류 시 표시할 이미지
            .into(holder.photoImageView)

        // 아이템 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            itemClickListener(auction)
        }
    }



    override fun getItemCount(): Int = auctionList.size  // getItemCount 함수가 제대로 위치하도록 수정
}
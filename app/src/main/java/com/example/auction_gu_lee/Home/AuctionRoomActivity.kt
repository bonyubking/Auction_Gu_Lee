package com.example.auction_gu_lee.Home

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ActivityAuctionRoomBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit

class AuctionRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuctionRoomBinding
    private var startingPrice: Int = 0
    private var highestPrice: Int = 0
    private var remainingTimeMillis: Long = 0
    private var participantUids: MutableSet<String> = mutableSetOf()
    private var countDownTimer: CountDownTimer? = null
    private lateinit var creatorUid: String
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuctionRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize data from Intent
        creatorUid = intent.getStringExtra("creator_uid")?.trim() ?: ""
        uid = FirebaseAuth.getInstance().currentUser?.uid?.trim() ?: ""
        val itemName = intent.getStringExtra("item_name") ?: "항목 없음"
        val itemDetail = intent.getStringExtra("item_detail") ?: "세부사항 없음"
        val startingPriceString = intent.getStringExtra("starting_price") ?: "0"
        val photoUrl = intent.getStringExtra("photo_url") ?: ""

        startingPrice = startingPriceString.toInt()
        highestPrice = startingPrice
        remainingTimeMillis = intent.getLongExtra("remaining_time", 0)

        // Firebase Realtime Database에서 사용자 이름 가져오기
        if (creatorUid.isNotEmpty()) {
            val database = FirebaseDatabase.getInstance().getReference("users")
            database.child(creatorUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val creatorUsername = dataSnapshot.child("username").getValue(String::class.java) ?: "사용자 이름 없음"
                        binding.username.text = creatorUsername
                    } else {
                        binding.username.text = "사용자 이름 없음"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.username.text = "사용자 이름 없음"
                    Log.e("FirebaseError", "데이터 읽기 실패: ${error.message}")
                }
            })
        } else {
            binding.username.text = "사용자 이름 없음"
        }

        // Log to check values
        Log.d("AuctionRoomActivity", "creatorUid: $creatorUid, uid: $uid")

        // Set initial values
        binding.itemName.text = itemName
        binding.itemDetail.text = itemDetail
        binding.startingPrice.text = "시작 가격: $startingPrice ₩"
        binding.highestPrice.text = "최고 가격: $highestPrice ₩"

        // Load auction item photo using Glide
        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(binding.itemPhoto)
        } else {
            binding.itemPhoto.setImageResource(R.drawable.error_image)
        }

        startCountDownTimer()
        updateHighestPriceColor()

        // Bid Button visibility & functionality
        binding.fabBid.visibility = View.VISIBLE
        if (creatorUid == uid) {
            // 경매 생성자에게 입찰 버튼을 비활성화
            binding.fabBid.isEnabled = false
        } else {
            // 경매 생성자가 아닌 경우 입찰 버튼을 활성화
            binding.fabBid.isEnabled = true
            binding.fabBid.setOnClickListener {
                placeBid()
            }
        }

        // Chat Button Click Listener
        binding.fabChat.setOnClickListener {
            openChat()
        }
    }

    private fun startCountDownTimer() {
        countDownTimer?.cancel() // 기존 타이머가 있으면 취소

        if (remainingTimeMillis > 0) {
            countDownTimer = object : CountDownTimer(remainingTimeMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingTimeMillis = millisUntilFinished

                    val days = TimeUnit.MILLISECONDS.toDays(remainingTimeMillis)
                    val hours = (TimeUnit.MILLISECONDS.toHours(remainingTimeMillis) % 24)
                    val minutes = (TimeUnit.MILLISECONDS.toMinutes(remainingTimeMillis) % 60)
                    val seconds = (TimeUnit.MILLISECONDS.toSeconds(remainingTimeMillis) % 60)

                    binding.remainingTime.text = String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds)

                    if (remainingTimeMillis <= TimeUnit.HOURS.toMillis(24)) {
                        binding.remainingTime.setTextColor(Color.RED)
                    } else {
                        binding.remainingTime.setTextColor(Color.BLACK)
                    }
                }

                override fun onFinish() {
                    binding.remainingTime.text = "경매 종료"
                    binding.remainingTime.setTextColor(Color.RED)
                }
            }.start()
        } else {
            binding.remainingTime.text = "경매 종료"
            binding.remainingTime.setTextColor(Color.RED)
        }
    }

    private fun updateHighestPriceColor() {
        if (highestPrice > startingPrice) {
            binding.highestPrice.setTextColor(Color.RED)
        } else {
            binding.highestPrice.setTextColor(Color.BLACK)
        }
    }

    private fun placeBid() {
        // Logic to place a bid
        val newBid = highestPrice + 1000  // Increase by 1000 ₩ for each bid
        highestPrice = newBid
        binding.highestPrice.text = "최고 가격: $highestPrice ₩"
        updateHighestPriceColor()

        // Add user to participant list
        participantUids.add(uid)
        binding.participantsCount.text = "참가자 수: ${participantUids.size} 명"
    }

    private fun openChat() {
        // Logic to open chat with auction creator
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel() // 타이머가 계속 실행되지 않도록 해제
    }
}

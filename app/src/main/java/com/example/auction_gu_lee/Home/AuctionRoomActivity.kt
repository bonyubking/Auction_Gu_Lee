package com.example.auction_gu_lee.Home

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.Chat.ChatActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ActivityAuctionRoomBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

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
    private lateinit var auctionId: String
    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuctionRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize data from Intent
        auctionId = intent.getStringExtra("auction_id") ?: ""
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Firebase Realtime Database에서 경매 데이터 가져오기
        if (auctionId.isNotEmpty()) {
            databaseReference.child("auctions").child(auctionId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val itemName = snapshot.child("item").getValue(String::class.java) ?: "항목 없음"
                            val itemDetail = snapshot.child("detail").getValue(String::class.java) ?: "세부사항 없음"
                            startingPrice = snapshot.child("startingPrice").getValue(Long::class.java)?.toInt() ?: 0
                            highestPrice = snapshot.child("highestPrice").getValue(Long::class.java)?.toInt() ?: startingPrice
                            remainingTimeMillis = snapshot.child("endTime").getValue(Long::class.java)
                                ?.minus(System.currentTimeMillis()) ?: 0
                            creatorUid = snapshot.child("creatorUid").getValue(String::class.java) ?: ""

                            // Set initial values in the UI
                            binding.itemName.text = itemName
                            binding.itemDetail.text = itemDetail
                            binding.startingPrice.text = "시작 가격: $startingPrice ₩"
                            binding.highestPrice.text = "최고 가격: $highestPrice ₩"
                            updateHighestPriceColor()

                            // Load auction item photo using Glide
                            val photoUrl = snapshot.child("photoUrl").getValue(String::class.java) ?: ""
                            if (photoUrl.isNotEmpty()) {
                                Glide.with(this@AuctionRoomActivity)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.placeholder_image)
                                    .error(R.drawable.error_image)
                                    .into(binding.itemPhoto)
                            } else {
                                binding.itemPhoto.setImageResource(R.drawable.error_image)
                            }

                            // 참가자 수 초기화
                            if (snapshot.hasChild("participants")) {
                                participantUids = snapshot.child("participants").children.mapNotNull { it.key }.toMutableSet()
                            }
                            binding.participantsCount.text = "참가자 수: ${participantUids.size} 명"

                            // Update UI for Bid Button visibility
                            binding.fabBid.visibility = View.VISIBLE
                            if (creatorUid == uid) {
                                binding.fabBid.isEnabled = false // 경매 생성자는 입찰 불가
                            } else {
                                binding.fabBid.isEnabled = true
                                binding.fabBid.setOnClickListener {
                                    placeBid()
                                }
                            }

                            // Firebase에서 사용자 이름 가져오기
                            databaseReference.child("users").child(creatorUid)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(userSnapshot: DataSnapshot) {
                                        if (userSnapshot.exists()) {
                                            val creatorUsername = userSnapshot.child("username").getValue(String::class.java) ?: "사용자 이름 없음"
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

                            // Start countdown timer
                            startCountDownTimer()

                        } else {
                            Toast.makeText(this@AuctionRoomActivity, "경매 데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@AuctionRoomActivity, "데이터베이스 오류: ${error.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                })
        } else {
            Toast.makeText(this, "경매 ID가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
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
        val newBid = highestPrice + 1000  // 입찰가를 1000 ₩ 증가
        highestPrice = newBid
        binding.highestPrice.text = "최고 가격: $highestPrice ₩"
        updateHighestPriceColor()

        // Firebase에 최고 가격 및 입찰자 정보 업데이트
        val auctionRef = databaseReference.child("auctions").child(auctionId)
        auctionRef.child("highestPrice").setValue(highestPrice)
        auctionRef.child("highestBidderUid").setValue(uid) // 입찰자의 UID 저장

        // 참가자 추가
        participantUids.add(uid)
        auctionRef.child("participants").setValue(participantUids.associateWith { true })
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.participantsCount.text = "참가자 수: ${participantUids.size} 명"
                } else {
                    Toast.makeText(this, "참가자 수 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun openChat() {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("auction_id", auctionId)
        intent.putExtra("seller_uid", creatorUid) // 판매자의 UID도 추가로 전달
        intent.putExtra("bidder_uid", uid) // 구매자의 UID도 추가로 전달
        startActivity(intent)
    }
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel() // 타이머가 계속 실행되지 않도록 해제
    }
}

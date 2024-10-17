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
import com.example.auction_gu_lee.models.Auction
import com.example.auction_gu_lee.models.ChatItem
import com.example.auction_gu_lee.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.UUID

import java.util.concurrent.TimeUnit

class AuctionRoomActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuctionRoomBinding
    private var startingPrice: Long = 0
    private var highestPrice: Long = 0
    private var remainingTimeMillis: Long = 0
    private var participantUids: MutableSet<String> = mutableSetOf()
    private var countDownTimer: CountDownTimer? = null
    private lateinit var creatorUid: String
    private lateinit var uid: String
    private lateinit var auctionId: String
    private val databaseReference = FirebaseDatabase.getInstance().reference
    private var favoritesCount: Int = 0
    private var biddersCount: Int = 0
    private var quantity: String = "" // 갯수 변수 수정

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuctionRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInitialVisibility()

        // Initialize data from Intent
        auctionId = intent.getStringExtra("auction_id") ?: ""
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // 관심 목록 상태 초기화
        updateWishlistButtonState()

        // 관심 목록 버튼 클릭 리스너 설정
        binding.btnAddToWishlist.setOnClickListener {
            toggleWishlist()
        }

        // Firebase Realtime Database에서 경매 데이터 가져오기
        if (auctionId.isNotEmpty()) {
            databaseReference.child("auctions").child(auctionId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val itemName = snapshot.child("item").getValue(String::class.java) ?: "항목 없음"
                            val itemDetail = snapshot.child("detail").getValue(String::class.java) ?: "세부사항 없음"
                            startingPrice = snapshot.child("startingPrice").getValue(Long::class.java) ?: 0L
                            highestPrice = snapshot.child("highestPrice").getValue(Long::class.java) ?: startingPrice
                            remainingTimeMillis = snapshot.child("endTime").getValue(Long::class.java)?.minus(System.currentTimeMillis()) ?: 0
                            creatorUid = snapshot.child("creatorUid").getValue(String::class.java) ?: ""

                            // favoritesCount 초기화
                            favoritesCount = snapshot.child("favoritesCount").getValue(Int::class.java) ?: 0

                            // biddersCount 초기화
                            biddersCount = snapshot.child("biddersCount").getValue(Int::class.java) ?: 0

                            quantity = snapshot.child("quantity").getValue(String::class.java) ?: "0개"

                            val bidUnit = calculateBidUnit(startingPrice)


                            // Set initial values in the UI
                            binding.itemName.text = itemName
                            binding.itemQuantity.text = "$quantity" // 갯수 표시
                            binding.itemDetail.text = itemDetail
                            binding.startingPrice.text = "시작가 $startingPrice ₩"
                            binding.highestPrice.text = "최고가 $highestPrice ₩"
                            binding.favoritesCount.text = "찜 $favoritesCount"
                            binding.participantsCount.text = "참가자 $biddersCount 명"
                            binding.bidUnit.text = "입찰 단위 $bidUnit" // 새로운 TextView 설정
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
                            binding.participantsCount.text = "참가자 수: $biddersCount 명"

                            // Update UI for Bid and Chat Button visibility
                            binding.fabBid.visibility = View.VISIBLE
                            binding.fabChat.visibility = View.VISIBLE

                            if (creatorUid == uid) {
                                // 판매자일 경우 입찰과 채팅 불가
                                disableButtons()
                            } else if (remainingTimeMillis <= 0) {
                                // 경매 종료 시 모든 버튼 비활성화
                                disableButtons()
                            } else {
                                binding.fabBid.isEnabled = true
                                binding.fabChat.isEnabled = true
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
                            Toast.makeText(
                                this@AuctionRoomActivity,
                                "경매 데이터를 불러올 수 없습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@AuctionRoomActivity,
                            "데이터베이스 오류: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                })
        } else {
            Toast.makeText(this, "경매 ID가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Chat Button Click Listener
        binding.fabChat.setOnClickListener {
            if (remainingTimeMillis > 0 && creatorUid != uid) {
                openChat()
            } else {
                Toast.makeText(this, "채팅을 이용할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 버튼 비활성화 메서드 추가
    private fun disableButtons() {
        binding.fabBid.isEnabled = false
        binding.fabChat.isEnabled = false
        binding.fabBid.setBackgroundColor(Color.GRAY)
        binding.fabChat.setBackgroundColor(Color.GRAY)
    }

    // 타이머 종료 시 버튼 비활성화
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

                    binding.remainingTime.text =
                        String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds)

                    if (remainingTimeMillis <= TimeUnit.HOURS.toMillis(24)) {
                        binding.remainingTime.setTextColor(Color.RED)
                    } else {
                        binding.remainingTime.setTextColor(Color.BLACK)
                    }
                }

                override fun onFinish() {
                    binding.remainingTime.text = "경매 종료"
                    binding.remainingTime.setTextColor(Color.RED)

                    // 경매 종료 시 모든 버튼 비활성화
                    disableButtons()

                    // 판매자일 경우에만 상위 입찰자와의 채팅 버튼 활성화
                    if (creatorUid == uid) {
                        fetchTopBidders()
                    }
                }
            }.start()
        } else {
            binding.remainingTime.text = "경매 종료"
            binding.remainingTime.setTextColor(Color.RED)

            // 경매 종료 시 모든 버튼 비활성화
            disableButtons()

            // 판매자일 경우에만 상위 입찰자와의 채팅 버튼 활성화
            if (creatorUid == uid) {
                fetchTopBidders()
            }
        }
    }


    private fun updateHighestPriceColor() {
        if (highestPrice > startingPrice) {
            binding.highestPrice.setTextColor(Color.RED)
        } else {
            binding.highestPrice.setTextColor(Color.BLACK)
        }
    }


    private fun setupInitialVisibility() {
        // 메시지 버튼과 입찰자 텍스트 뷰를 GONE으로 설정
        val messageButtons = listOf(binding.btnMessage1, binding.btnMessage2, binding.btnMessage3)
        val bidderTexts = listOf(binding.highestBidder1, binding.highestBidder2, binding.highestBidder3)
        val priceTexts = listOf(binding.priceBidder1, binding.priceBidder2, binding.priceBidder3)

        for (button in messageButtons) {
            button.visibility = View.GONE
        }

        for (textView in bidderTexts + priceTexts) {
            textView.visibility = View.GONE
        }
    }

    private fun placeBid() {
        // 입찰 증가폭 결정 로직
        val increment = when (startingPrice) {
            in 100..499 -> 1
            in 500..999 -> 5
            in 1000..4999 -> 10
            in 5000..9999 -> 50
            in 10000..49999 -> 100
            in 50000..99999 -> 500
            in 100000..499999 -> 1000
            in 500000..999999 -> 5000
            in 1000000..4999999 -> 10000
            in 5000000..9999999 -> 50000
            in 10000000..49999999 -> 100000
            in 50000000..99999999 -> 500000
            in 100000000..499999999 -> 1000000
            in 500000000..999999999 -> 5000000
            in 1000000000..4999999999 -> 10000000
            in 5000000000..9999999999 -> 50000000
            in 10000000000..49999999999 -> 100000000
            in 50000000000..99999999999 -> 500000000
            in 100000000000..499999999999 -> 1000000000
            in 500000000000..999999999999 -> 5000000000
            in 1000000000000..4999999999999 -> 10000000000
            in 5000000000000..9999999999999 -> 50000000000
            in 10000000000000..100000000000000 -> 100000000000
            else -> startingPrice / 100  // 100억 초과 시 시작 가격의 1%로 증가
        }

        val newBid = highestPrice + increment  // 입찰가를 증가폭만큼 증가


        highestPrice = newBid
        binding.highestPrice.text = "최고 가격: $highestPrice ₩"
        updateHighestPriceColor()

        // Firebase에 최고 가격 및 입찰자 정보 업데이트
        val auctionRef = databaseReference.child("auctions").child(auctionId)

        val previousBidderUidHolder = arrayOf<String?>(null)

        auctionRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val auction = currentData.getValue(Auction::class.java)
                    ?: return Transaction.success(currentData)

                previousBidderUidHolder[0] = auction.highestBidderUid

                // 최고 가격 및 입찰자 UID 업데이트
                auction.highestPrice = highestPrice
                auction.highestBidderUid = uid

                // 이미 참가자인지 확인
                val isAlreadyParticipant = auction.participants.containsKey(uid)

                if (!isAlreadyParticipant) {
                    // 새로운 참가자라면 참가자 목록에 입찰 금액 추가 및 biddersCount 증가
                    auction.participants[uid] = newBid
                    auction.biddersCount = (auction.biddersCount ?: 0) + 1
                } else {
                    // 기존 참가자라면 입찰 금액 업데이트
                    auction.participants[uid] = newBid
                }

                // 업데이트된 경매 정보를 현재 데이터에 설정
                currentData.value = auction
                return Transaction.success(currentData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error == null && committed) {
                    val updatedAuction = currentData?.getValue(Auction::class.java)
                    if (updatedAuction != null) {
                        highestPrice = updatedAuction.highestPrice ?: 0L
                        binding.highestPrice.text = "최고가 $highestPrice ₩"
                        updateHighestPriceColor()
                        biddersCount = updatedAuction.biddersCount ?: 0
                        binding.participantsCount.text = "참가자 수: $biddersCount 명"
                        Toast.makeText(this@AuctionRoomActivity, "입찰 성공!", Toast.LENGTH_SHORT).show()
                        createBidNotification(newBid)

                        val previousBidderUid = previousBidderUidHolder[0]
                        if (previousBidderUid != null && previousBidderUid != uid) {
                            sendOutbidNotification(previousBidderUid, updatedAuction)
                        }
                    }
                } else {
                    Toast.makeText(this@AuctionRoomActivity, "입찰 실패: ${error?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun sendOutbidNotification(previousBidderUid: String, auction: Auction) {
        val notificationId = UUID.randomUUID().toString()
        val notification = Notification(
            id = notificationId,
            message = "경매 '${auction.item}'의 최고 입찰가가 갱신되었습니다.",
            timestamp = null,
            type = "bid",
            relatedAuctionId = auction.id,
            read = false
        )

        val notificationMap = notification.toMap().toMutableMap()
        notificationMap["timestamp"] = ServerValue.TIMESTAMP

        databaseReference.child("users").child(previousBidderUid).child("notifications")
            .child(notificationId)
            .setValue(notification)
            .addOnSuccessListener {
                Log.d("AuctionRoomActivity", "Outbid notification sent to previous bidder successfully")
            }
            .addOnFailureListener { e ->
                Log.e("AuctionRoomActivity", "Failed to send outbid notification to previous bidder: ${e.message}")
            }
    }

    private fun createBidNotification(newBidAmount: Long) {
        val notificationId = UUID.randomUUID().toString()
        val notification = Notification(
            id = notificationId,
            message = "${binding.itemName.text}에 새로운 입찰가 ${newBidAmount}원이 등록되었습니다.",
            timestamp = null,
            type = "bid",
            relatedAuctionId = auctionId,
            read = false
        )

        val notificationMap = notification.toMap().toMutableMap()
        notificationMap["timestamp"] = ServerValue.TIMESTAMP



        databaseReference.child("users").child(creatorUid).child("notifications")
            .child(notificationId)
            .setValue(notification)
            .addOnSuccessListener {
                Log.d("AuctionRoomActivity", "Notification created successfully")
            }
            .addOnFailureListener { e ->
                Log.e("AuctionRoomActivity", "Failed to create notification: ${e.message}")
            }
    }


    private fun calculateBidIncrement(startingPrice: Long): Long {
        return when (startingPrice) {
            in 100..499 -> 1
            in 500..999 -> 5
            in 1000..4999 -> 10
            in 5000..9999 -> 50
            in 10000..49999 -> 100
            in 50000..99999 -> 500
            in 100000..499999 -> 1000
            in 500000..999999 -> 5000
            in 1000000..4999999 -> 10000
            in 5000000..9999999 -> 50000
            in 10000000..49999999 -> 100000
            in 50000000..99999999 -> 500000
            in 100000000..499999999 -> 1000000
            in 500000000..999999999 -> 5000000
            in 1000000000..4999999999 -> 10000000
            in 5000000000..9999999999 -> 50000000
            in 10000000000..49999999999 -> 100000000
            in 50000000000..99999999999 -> 500000000
            in 100000000000..499999999999 -> 1000000000
            in 500000000000..999999999999 -> 5000000000
            in 1000000000000..4999999999999 -> 10000000000
            in 5000000000000..9999999999999 -> 50000000000
            in 10000000000000..100000000000000 -> 100000000000
            else -> startingPrice / 100  // 100억 초과 시 시작 가격의 1%로 증가
        }
    }

    private fun calculateBidUnit(startingPrice: Long): String {
        val increment = calculateBidIncrement(startingPrice)
        return "${increment} ₩"
    }


    // 관심 목록 추가/제거 토글
    private fun toggleWishlist() {
        val wishlistReference = databaseReference.child("users").child(uid).child("wishlist")
        val auctionRef = databaseReference.child("auctions").child(auctionId)

        wishlistReference.child(auctionId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // 이미 관심 목록에 존재 -> 삭제
                wishlistReference.child(auctionId).removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // favoritesCount 감소
                        auctionRef.child("favoritesCount").runTransaction(object : Transaction.Handler {
                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                val currentCount = currentData.getValue(Int::class.java) ?: 1
                                currentData.value = if (currentCount > 0) currentCount - 1 else 0
                                return Transaction.success(currentData)
                            }

                            override fun onComplete(error: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                                if (error == null && committed) {
                                    Toast.makeText(this@AuctionRoomActivity, "관심 목록에서 제거되었습니다.", Toast.LENGTH_SHORT).show()
                                    binding.btnAddToWishlist.setImageResource(R.drawable.baseline_star_25)
                                    // favoritesCount UI 업데이트
                                    favoritesCount = if (favoritesCount > 0) favoritesCount - 1 else 0
                                    binding.favoritesCount.text = "찜 수: $favoritesCount"
                                } else {
                                    Toast.makeText(this@AuctionRoomActivity, "관심 목록 제거에 실패했습니다: ${error?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    } else {
                        Toast.makeText(this@AuctionRoomActivity, "관심 목록 제거에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // 관심 목록에 없음 -> 추가
                wishlistReference.child(auctionId).setValue(true).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // favoritesCount 증가
                        auctionRef.child("favoritesCount").runTransaction(object : Transaction.Handler {
                            override fun doTransaction(currentData: MutableData): Transaction.Result {
                                val currentCount = currentData.getValue(Int::class.java) ?: 0
                                currentData.value = currentCount + 1
                                return Transaction.success(currentData)
                            }

                            override fun onComplete(error: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                                if (error == null && committed) {
                                    Toast.makeText(this@AuctionRoomActivity, "관심 목록에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                                    binding.btnAddToWishlist.setImageResource(R.drawable.baseline_star_24)
                                    // favoritesCount UI 업데이트
                                    favoritesCount += 1
                                    binding.favoritesCount.text = "찜 수: $favoritesCount"
                                } else {
                                    Toast.makeText(this@AuctionRoomActivity, "관심 목록 추가에 실패했습니다: ${error?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    } else {
                        Toast.makeText(this@AuctionRoomActivity, "관심 목록 추가에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateWishlistButtonState() {
        val wishlistReference = databaseReference.child("users").child(uid).child("wishlist")
        wishlistReference.child(auctionId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // 관심 목록에 있으면 채워진 별 모양으로 설정
                binding.btnAddToWishlist.setImageResource(R.drawable.baseline_star_24)
            } else {
                // 관심 목록에 없으면 빈 별 모양으로 설정
                binding.btnAddToWishlist.setImageResource(R.drawable.baseline_star_25)
            }
        }
    }

    private fun openChat() {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("auction_id", auctionId)
        intent.putExtra("seller_uid", creatorUid)
        intent.putExtra("bidder_uid", uid)
        startActivity(intent)
    }


    private fun fetchTopBidders() {
        val auctionRef = databaseReference.child("auctions").child(auctionId).child("participants")
        auctionRef.orderByValue().limitToLast(3)  // 상위 3명의 입찰자를 가져옵니다
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // 상위 3명의 입찰자를 가져와서 정렬하고 리스트 생성
                    val topBidders = snapshot.children.mapNotNull {
                        val bidderUid = it.key
                        val price = it.getValue(Long::class.java)
                        if (bidderUid != null && price != null) {
                            Log.d("AuctionRoomActivity", "Bidder UID: $bidderUid, Price: $price")
                            bidderUid to price
                        } else {
                            null
                        }
                    }.sortedByDescending { it.second } // 입찰가를 기준으로 내림차순 정렬

                    // 정렬된 입찰자 정보로 UI 업데이트
                    setupChatButtons(topBidders)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "상위 입찰자 가져오기 실패: ${error.message}")
                }
            })
    }






    private fun setupChatButtons(topBidders: List<Pair<String, Long>>) {
        val messageButtons = listOf(binding.btnMessage1, binding.btnMessage2, binding.btnMessage3)
        val bidderTexts = listOf(binding.highestBidder1, binding.highestBidder2, binding.highestBidder3)
        val priceTexts = listOf(binding.priceBidder1, binding.priceBidder2, binding.priceBidder3)

        // 현재 사용자가 판매자인지 확인
        val isSeller = uid == creatorUid

        for (i in messageButtons.indices) {
            if (i < topBidders.size && isSeller) {
                val (bidderUid, price) = topBidders[i]
                // 각 입찰자의 사용자 이름을 가져옵니다
                databaseReference.child("users").child(bidderUid).child("username")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val username = userSnapshot.getValue(String::class.java) ?: "사용자 없음"
                            bidderTexts[i].text = "$username (순위 ${i + 1})"
                            bidderTexts[i].visibility = View.VISIBLE
                            messageButtons[i].visibility = View.VISIBLE

                            // 입찰 금액 표시
                            priceTexts[i].text = "$price ₩"
                            priceTexts[i].visibility = View.VISIBLE

                            // 버튼 클릭 리스너 설정
                            messageButtons[i].setOnClickListener {
                                Log.d("AuctionRoomActivity", "Message button clicked for bidder: $bidderUid")
                                openChatWithBidder(bidderUid)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("FirebaseError", "사용자 이름 가져오기 실패: ${error.message}")
                        }
                    })
            } else {
                // 입찰자가 3명보다 적거나 판매자가 아닌 경우 버튼을 숨깁니다
                messageButtons[i].visibility = View.GONE
                bidderTexts[i].visibility = View.GONE
                priceTexts[i].visibility = View.GONE
            }
        }
    }


    private fun openChatWithBidder(bidderUid: String) {

        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("auction_id", auctionId)
            putExtra("seller_uid", creatorUid)
            putExtra("bidder_uid", bidderUid)
        }
        startActivity(intent)
    }




    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel() // 타이머가 계속 실행되지 않도록 해제
    }
}


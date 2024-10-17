package com.example.auction_gu_lee.Notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : AppCompatActivity() {

    private lateinit var notificationContainer: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)


        notificationContainer = findViewById(R.id.notification_container)
        recyclerView = findViewById(R.id.recyclerView_notifications)
        database = FirebaseDatabase.getInstance().reference
        auth = FirebaseAuth.getInstance()

        currentUserId = auth.currentUser?.uid

        setupRecyclerView()

        currentUserId?.let {
            loadNotifications(it)

            // 찜 목록 알림 설정 추가
            setupWishlistNotification(it)  // <-- 찜 목록 알림 설정을 호출하는 부분 추가
        } ?: run {
            showLoginRequiredMessage()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(mutableListOf()) { notification ->
            // 알림 클릭 시 처리
            handleNotificationClick(notification)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = notificationAdapter
    }

    private fun loadNotifications(userId: String) {
        val notificationsRef = database.child("users").child(userId).child("notifications")
        // 초기 알림 로드
        notificationsRef.orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val notifications = mutableListOf<Notification>()
                    for (notificationSnapshot in snapshot.children) {
                        // 데이터가 Boolean인지 확인하고 Boolean 데이터를 건너뜁니다.
                        if (notificationSnapshot.getValue() is Boolean) {
                            continue
                        }

                        val notification = notificationSnapshot.getValue(Notification::class.java)
                        if (notification != null) {
                            notifications.add(notification)
                        }
                    }
                    // 최신순으로 정렬 (timestamp 내림차순)
                    notifications.sortByDescending { it.timestamp }
                    // RecyclerView에 알림을 표시
                    notificationAdapter.setNotifications(notifications)

                    addRealTimeListener(notificationsRef)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NotificationActivity", "알림 가져오기 실패: ${error.message}")
                    Toast.makeText(
                        this@NotificationActivity,
                        "알림을 가져오는 데 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


    // 실시간으로 새로운 알림 추가 처리
    private fun addRealTimeListener(notificationsRef: DatabaseReference) {
        notificationsRef.orderByChild("timestamp")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    // Boolean 타입 데이터를 건너뜁니다.
                    if (snapshot.getValue() is Boolean) {
                        return
                    }

                    val notification = snapshot.getValue(Notification::class.java)
                    notification?.let {
                        // 이미 초기 로드에서 추가된 알림인지 확인
                        if (!notificationAdapter.contains(it)) {
                            notificationAdapter.addNotification(it)
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val updatedNotification = snapshot.getValue(Notification::class.java)
                    updatedNotification?.let {
                        notificationAdapter.updateNotification(it)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val removedNotification = snapshot.getValue(Notification::class.java)
                    removedNotification?.let {
                        notificationAdapter.removeNotification(it)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // 필요한 경우 구현
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NotificationActivity", "실시간 알림 가져오기 실패: ${error.message}")
                }
            })
    }


    private fun handleNotificationClick(notification: Notification) {
        when (notification.type) {
            "bid", "auction_end" -> {
                // 입찰 알림이나 경매 종료 알림 클릭 시 해당 경매 상세 페이지로 이동
                val intent = Intent(this, AuctionRoomActivity::class.java).apply {
                    putExtra("auction_id", notification.relatedAuctionId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
            }
            // 기타 알림 유형에 대한 처리
        }

        // 알림을 읽음 상태로 변경
        markNotificationAsRead(notification)
    }



    private fun markNotificationAsRead(notification: Notification) {
        currentUserId?.let { userId ->
            val notificationId = notification.id ?: return  // notification의 id가 null이면 함수 종료
            val notificationRef = database.child("users").child(userId)
                .child("notifications").child(notificationId)
            notificationRef.child("read").setValue(true)
        }
    }


    private fun showLoginRequiredMessage() {
        // 로그인이 필요하다는 메시지를 표시
        val messageView = TextView(this)
        messageView.text = "알림을 보려면 로그인이 필요합니다."
        messageView.textSize = 16f
        notificationContainer.addView(messageView)
    }

    private fun setupWishlistNotification(userId: String) {
        val wishlistRef = database.child("users").child(userId).child("wishlist")

        wishlistRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (auctionSnapshot in snapshot.children) {
                    val auctionId = auctionSnapshot.key ?: continue
                    checkAuctionEndTime(auctionId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationActivity", "찜 목록 불러오기 실패: ${error.message}")
            }
        })
    }

    private fun checkAuctionEndTime(auctionId: String) {
        val auctionRef = database.child("auctions").child(auctionId)

        auctionRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val endTime = snapshot.child("endTime").getValue(Long::class.java) ?: return
                val item = snapshot.child("item").getValue(String::class.java) ?: "상품"
                val currentTime = System.currentTimeMillis()
                val timeLeft = endTime - currentTime

                // 찜 목록 알림 여부 확인
                val notificationSentRef = database.child("users").child(currentUserId!!)
                    .child("notifications_sent").child(auctionId).child("auction_end")

                notificationSentRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(notificationSnapshot: DataSnapshot) {
                        // 이미 'auction_end' 알림이 전송된 경우 건너뜀
                        if (notificationSnapshot.exists()) {
                            return
                        }

                        // 1시간 이내에 경매가 종료될 경우 알림을 보냄
                        if (timeLeft in 0..3600000) {
                            // Firebase에 고유한 알림 ID 생성
                            val notificationRef = database.child("users").child(currentUserId!!)
                                .child("notifications").push()
                            val notificationId = notificationRef.key ?: throw IllegalStateException("Failed to generate notification ID")
                            // 고유한 ID

                            // 알림을 Firebase에 추가
                            val notification = Notification(
                                id = notificationId, // 고유한 ID를 저장
                                message = "찜 하신 $item, 1시간 남았습니다.",
                                relatedAuctionId = auctionId,
                                timestamp = System.currentTimeMillis(),
                                type = "auction_end"
                            )
                            notificationRef.setValue(notification)

                            // 'auction_end' 알림 전송 기록
                            notificationSentRef.setValue(true)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("NotificationActivity", "알림 전송 여부 확인 실패: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationActivity", "경매 종료 시간 불러오기 실패: ${error.message}")
            }
        })
    }







}






class NotificationAdapter(
    private val notifications: MutableList<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.message_text)
        val dateText: TextView = view.findViewById(R.id.date_text)
    }

    fun contains(notification: Notification): Boolean {
        return notifications.any { it.id == notification.id }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.messageText.text = notification.message ?: "No message"

        // Nullable timestamp 처리 및 로그 출력
        if (notification.timestamp is Long) {
            holder.dateText.text = formatDate(notification.timestamp as Long)
            Log.d("NotificationAdapter", "Notification ID: ${notification.id}, Timestamp: ${notification.timestamp}")
        } else {
            holder.dateText.text = "Unknown Date"
            Log.d("NotificationAdapter", "Notification ID: ${notification.id}, Timestamp is null or not a Long")
        }

        if (!notification.read) {
            holder.messageText.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            holder.messageText.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        holder.itemView.setOnClickListener { onItemClick(notification) }
    }


    override fun getItemCount() = notifications.size

    fun setNotifications(newNotifications: List<Notification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }


    fun addNotification(notification: Notification) {
        notifications.add(0, notification) // 최신 알림을 리스트 맨 앞에 추가
        notifyItemInserted(0)
    }

    fun updateNotification(notification: Notification) {
        val index = notifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            notifications[index] = notification
            notifyItemChanged(index)
        }
    }

    fun removeNotification(notification: Notification) {
        val index = notifications.indexOfFirst { it.id == notification.id }
        if (index != -1) {
            notifications.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    private fun formatDate(timestamp: Long?): String {
        return if (timestamp != null) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else {
            "Unknown Date"
        }
    }
}
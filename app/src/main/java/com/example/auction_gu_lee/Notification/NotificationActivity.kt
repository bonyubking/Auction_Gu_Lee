package com.example.auction_gu_lee.Notification

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
        } ?: run {
            // 사용자가 로그인하지 않은 경우 처리
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
        notificationsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val notification = snapshot.getValue(Notification::class.java)
                notification?.let {
                    notificationAdapter.addNotification(it)
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
                // 에러 처리
            }
        })
    }

    private fun handleNotificationClick(notification: Notification) {
        when (notification.type) {
            "bid" -> {
                // 입찰 알림 클릭 시 해당 경매 상세 페이지로 이동
                val intent = Intent(this, AuctionRoomActivity::class.java)
                intent.putExtra("auction_id", notification.relatedAuctionId)
                startActivity(intent)
            }
            "auction_end" -> {
                // 경매 종료 알림 클릭 시 처리
                // 예: 결제 페이지로 이동 또는 경매 결과 페이지로 이동
            }
            // 기타 알림 유형에 대한 처리
        }

        // 알림을 읽음 상태로 변경
        markNotificationAsRead(notification)
    }

    private fun markNotificationAsRead(notification: Notification) {
        currentUserId?.let { userId ->
            val notificationRef = database.child("users").child(userId)
                .child("notifications").child(notification.id)
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
}

class NotificationAdapter(
    private val notifications: MutableList<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.message_text)
        val dateText: TextView = view.findViewById(R.id.date_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.messageText.text = notification.message
        holder.dateText.text = formatDate(notification.timestamp)

        holder.itemView.setOnClickListener { onItemClick(notification) }
    }

    override fun getItemCount() = notifications.size

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

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
package com.example.auction_gu_lee.Notification

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.Board.PostDetailActivity
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Comment
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

        val btnMarkAllRead: Button = findViewById(R.id.btn_mark_all_read)
        btnMarkAllRead.setOnClickListener {
            markAllNotificationsAsRead()
        }

        currentUserId?.let {
            loadNotifications(it)

            // 찜 목록 알림 설정 추가
            setupWishlistNotification(it)

            // 댓글 알림 설정 추가
            setupCommentNotification(it)
        } ?: run {
            showLoginRequiredMessage()
        }
    }

    private fun markAllNotificationsAsRead() {
        currentUserId?.let { userId ->
            val notificationsRef = database.child("users").child(userId).child("notifications")

            notificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (notificationSnapshot in snapshot.children) {
                        val notification = notificationSnapshot.getValue(Notification::class.java)
                        if (notification != null && !notification.read) {
                            notificationSnapshot.ref.child("read").setValue(true)
                        }
                    }

                    // UI 업데이트
                    notificationAdapter.markAllAsRead()
                    Toast.makeText(this@NotificationActivity, "모든 알림을 읽음 처리했습니다.", Toast.LENGTH_SHORT).show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NotificationActivity", "알림 읽음 처리 실패: ${error.message}")
                    Toast.makeText(this@NotificationActivity, "알림 읽음 처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
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
                // 입찰 알림, 경매 종료 알림 클릭 시 AuctionRoomActivity로 이동
                val intent = Intent(this, AuctionRoomActivity::class.java).apply {
                    putExtra("auction_id", notification.relatedAuctionId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
            }
            "comment" -> {
                // 댓글 알림 클릭 시 PostDetailActivity로 이동
                val intent = Intent(this, PostDetailActivity::class.java).apply {
                    putExtra("postId", notification.relatedPostId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
            }
            // 기타 알림 유형에 대한 처리
            else -> {
                Toast.makeText(this, "알 수 없는 알림 유형입니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 알림을 읽음 상태로 변경
        markNotificationAsRead(notification)
    }



    private fun markNotificationAsRead(notification: Notification) {
        currentUserId?.let { userId ->
            val notificationId = notification.id ?: run {
                Log.e("NotificationActivity", "notification.id가 null입니다.")
                return
            }
            val notificationRef = database.child("users").child(userId)
                .child("notifications").child(notificationId)
            notificationRef.child("read").setValue(true)
                .addOnSuccessListener {
                    Log.d("NotificationActivity", "알림 읽음 상태로 변경됨: $notificationId")
                }
                .addOnFailureListener { exception ->
                    Log.e("NotificationActivity", "알림 읽음 상태 변경 실패: ${exception.message}")
                }
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
                val endTime = snapshot.child("endTime").getValue(Long::class.java) ?: run {
                    Log.e("NotificationActivity", "endTime을 가져오지 못했습니다: $auctionId")
                    return
                }
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
                            val notificationId = notificationRef.key ?: run {
                                Log.e("NotificationActivity", "알림 ID 생성 실패")
                                return
                            }

                            // 알림을 Firebase에 추가
                            val notification = Notification(
                                id = notificationId,
                                message = "찜 하신 $item, 1시간 남았습니다.",
                                relatedAuctionId = auctionId,
                                timestamp = System.currentTimeMillis(),
                                type = "auction_end",
                                read = false
                            )
                            notificationRef.setValue(notification)
                                .addOnSuccessListener {
                                    Log.d("NotificationActivity", "auction_end 알림 추가 성공: $notificationId")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("NotificationActivity", "auction_end 알림 추가 실패: ${exception.message}")
                                }

                            // 'auction_end' 알림 전송 기록
                            notificationSentRef.setValue(true)
                                .addOnSuccessListener {
                                    Log.d("NotificationActivity", "'auction_end' 알림 전송 기록 완료: $auctionId")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("NotificationActivity", "'auction_end' 알림 전송 기록 실패: ${exception.message}")
                                }
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

    private fun setupCommentNotification(userId: String) {
        val purchasePostsRef = database.child("purchase_posts")

        purchasePostsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val postId = snapshot.key ?: return
                val ownerId = snapshot.child("userId").getValue(String::class.java)
                if (ownerId != null) {
                    listenForNewComments(postId, ownerId)
                } else {
                    Log.e("NotificationActivity", "ownerId를 가져오지 못했습니다: $postId")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationActivity", "댓글 알림 설정 실패: ${error.message}")
            }
        })
    }


    private fun listenForNewComments(postId: String, ownerId: String?) {
        if (ownerId == null) {
            Log.e("NotificationActivity", "ownerId가 null입니다: $postId")
            return
        }

        val commentsRef = database.child("purchase_posts").child(postId).child("comments")

        // 서버 시간 가져오기
        val serverTimeOffsetRef = database.child(".info/serverTimeOffset")
        serverTimeOffsetRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offset = snapshot.getValue(Double::class.java) ?: 0.0
                val serverTime = System.currentTimeMillis() + offset.toLong()

                // 현재 서버 시간 이후의 댓글만 리스닝
                val query = commentsRef.orderByChild("timestamp").startAt(serverTime.toDouble())

                query.addChildEventListener(object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        val comment = snapshot.getValue(Comment::class.java)

                        // comment.userId가 null이거나 비어 있으면 알림을 생성하지 않음
                        if (comment != null && !comment.userId.isNullOrEmpty() && comment.userId != ownerId) {
                            // 댓글 작성자와 게시물 작성자가 다른 경우에만 알림 생성
                            val notificationMessage = "구매 요청 게시글에 새로운 댓글이 달렸습니다."
                            addNotificationToFirebase(ownerId, postId, comment.userId, notificationMessage)
                        } else {
                            Log.e("NotificationActivity", "comment.userId가 null이거나 비어있습니다. 알림을 생성하지 않음.")
                        }
                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onChildRemoved(snapshot: DataSnapshot) {}
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("NotificationActivity", "댓글 리스닝 중 오류 발생: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("NotificationActivity", "서버 시간 가져오기 실패: ${error.message}")
            }
        })
    }



    private fun addNotificationToFirebase(recipientUserId: String, postId: String, commentAuctionId: String?, message: String) {
        // commenterId가 null이거나 비어 있는 경우 알림을 생성하지 않음
        if (commentAuctionId.isNullOrEmpty()) {
            Log.e("NotificationActivity", "ccommentAuctionId가 null 또는 비어 있습니다.")
            return
        }

        Log.d("NotificationActivity", "Firebase에 알림 추가 중: $message to user: $recipientUserId")
        val notificationRef = database.child("users").child(recipientUserId).child("notifications")

        // 중복된 알림이 있는지 확인
        notificationRef.orderByChild("relatedPostId").equalTo(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var notificationExists = false

                    // 이미 같은 commenterId로 등록된 알림이 있는지 확인
                    for (child in snapshot.children) {
                        val notification = child.getValue(Notification::class.java)
                        if (notification?.relatedPostId == postId && notification.commentAuctionId == commentAuctionId) {
                            notificationExists = true
                            break
                        }
                    }

                    // 중복되지 않은 경우에만 알림을 추가
                    if (!notificationExists) {
                        val newNotificationRef = notificationRef.push()
                        val notificationId = newNotificationRef.key ?: return
                        val notificationData = mapOf(
                            "id" to notificationId,
                            "message" to message,
                            "relatedPostId" to postId,
                            "commentAuctionId" to commentAuctionId,  // commenterId가 이제 반드시 포함됨
                            "timestamp" to ServerValue.TIMESTAMP,
                            "type" to "comment",
                            "read" to false
                        )

                        newNotificationRef.setValue(notificationData)
                            .addOnSuccessListener {
                                Log.d("NotificationActivity", "알림이 성공적으로 추가되었습니다: $notificationId")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("NotificationActivity", "알림 추가 실패: ${exception.message}")
                            }
                    } else {
                        Log.d("NotificationActivity", "중복된 알림이 존재합니다.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("NotificationActivity", "알림 중복 확인 실패: ${error.message}")
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

    fun markAllAsRead() {
        notifications.forEach { it.read = true }
        notifyDataSetChanged()
    }

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

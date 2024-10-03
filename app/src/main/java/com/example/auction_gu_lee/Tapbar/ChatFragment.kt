package com.example.auction_gu_lee.Tapbar

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.google.firebase.database.* // Firebase Database 관련 클래스 임포트
import android.widget.ImageView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import android.text.format.DateUtils

// Firebase에서 가져올 데이터 모델
data class ChatItem(
    val item: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L,
    var isRead: Boolean = true // 읽음 여부 표시

)

class ChatFragment : Fragment() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var database: DatabaseReference
    private val chatList = mutableListOf<ChatItem>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 기존의 뒤로가기 버튼 비활성화 코드 유지
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 뒤로가기 버튼 동작을 무효화
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view)
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Firebase Database 초기화
        database = FirebaseDatabase.getInstance().getReference("auctions")

        // 어댑터 초기화 및 연결
        adapter = ChatAdapter(chatList)
        chatRecyclerView.adapter = adapter

        // Firebase에서 데이터 읽기
        loadDataFromFirebase()

        return view
    }

    // Firebase에서 데이터 불러오기 함수
    private fun loadDataFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()  // 기존 데이터 초기화
                for (auctionSnapshot in snapshot.children) {
                    val chatItem = auctionSnapshot.getValue(ChatItem::class.java)
                    chatItem?.let { chatList.add(it) }
                }
                chatList.sortByDescending { it.timestamp } // 최신 순으로 정렬
                adapter.notifyDataSetChanged() // 어댑터에 변경사항 알림
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리
            }
        })
    }
}

class ChatAdapter(private val chatItems: List<ChatItem>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.chat_profile_image)
        val itemTitle: TextView = itemView.findViewById(R.id.chat_item_title)
        val userName: TextView = itemView.findViewById(R.id.chat_user_name)
        val lastMessage: TextView = itemView.findViewById(R.id.chat_last_message)
        val chatTime: TextView = itemView.findViewById(R.id.chat_time)  // 마지막 채팅 시간 텍스트뷰
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatItem = chatItems[position]

        // Glide 사용하여 이미지 로드
        Glide.with(holder.profileImage.context)
            .load(chatItem.photoUrl)
            .placeholder(R.drawable.placehoder_image)
            .error(R.drawable.placehoder_image)
            .into(holder.profileImage)

        holder.itemTitle.text = chatItem.item
        holder.userName.text = chatItem.username

        // 마지막 채팅 내용 설정
        holder.lastMessage.text = if (chatItem.lastMessage.isNotEmpty()) {
            chatItem.lastMessage
        } else {
            "마지막 채팅 없음"
        }

        // 마지막 채팅 시간을 설정 (시간 형식: 24시간 이내 → hh:mm a, 24시간 이상 → X일 전)
        holder.chatTime.text = formatTimestamp(chatItem.timestamp)

        // 채팅 읽음 여부에 따른 텍스트 스타일 설정
        if (chatItem.isRead) {
            holder.lastMessage.setTypeface(null, Typeface.NORMAL)
        } else {
            holder.lastMessage.setTypeface(null, Typeface.BOLD)
        }

        // 구분선의 가시성 설정 (마지막 항목에서는 구분선 숨김)
    }

    override fun getItemCount(): Int = chatItems.size

    // timestamp 형식을 변환하는 함수 추가
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()

        return when {
            // 24시간 이내일 경우 시간 형식 (예: 12:30 PM)
            DateUtils.isToday(timestamp) -> {
                val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
            // 24시간 이상일 경우 "X일 전" 형식
            now - timestamp < 7 * 24 * 60 * 60 * 1000 -> {  // 일주일 이내일 경우 "X일 전"
                val daysAgo = (now - timestamp) / (24 * 60 * 60 * 1000)
                "${daysAgo}일 전"
            }
            // 일주일이 넘으면 날짜 표시 (예: 2024/10/12)
            else -> {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
}

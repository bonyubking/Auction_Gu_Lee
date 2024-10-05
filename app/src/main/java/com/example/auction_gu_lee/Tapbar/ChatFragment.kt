package com.example.auction_gu_lee.Tapbar

import android.content.Context
import android.content.Intent
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
import com.example.auction_gu_lee.Chat.ChatActivity
import com.example.auction_gu_lee.models.ChatItem
import com.google.firebase.auth.FirebaseAuth

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

        adapter = ChatAdapter(requireContext(), chatList)
        chatRecyclerView.adapter = adapter

        // Firebase Database 초기화
        database = FirebaseDatabase.getInstance().getReference("auctions")

        // Firebase에서 데이터 읽기
        loadDataFromFirebase()

        return view
    }

    private fun loadDataFromFirebase() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear() // 기존 데이터 초기화

                for (auctionSnapshot in snapshot.children) {
                    val auctionId = auctionSnapshot.key ?: continue
                    val auctionCreatorUid = auctionSnapshot.child("creatorUid").getValue(String::class.java) ?: ""
                    val photoUrl = auctionSnapshot.child("photoUrl").getValue(String::class.java) ?: ""

                    val chatsSnapshot = auctionSnapshot.child("chats")
                    for (chatRoomSnapshot in chatsSnapshot.children) {
                        val chatRoomId = chatRoomSnapshot.key ?: continue

                        // chatRoomId를 고유하게 생성
                        val generatedChatRoomId = if (auctionCreatorUid < currentUserId) {
                            "${auctionId}_${auctionCreatorUid}_$currentUserId"
                        } else {
                            "${auctionId}_${currentUserId}_$auctionCreatorUid"
                        }

                        if (chatRoomId == generatedChatRoomId) {
                            // 각 채팅방의 최신 메시지를 가져오기 위해 orderByChild와 limitToLast 사용
                            chatRoomSnapshot.ref.orderByChild("timestamp").limitToLast(1)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(messageSnapshot: DataSnapshot) {
                                        for (message in messageSnapshot.children) {
                                            val messageData = message.getValue(ChatItem::class.java) ?: continue

                                            // 현재 사용자가 판매자이거나 메시지의 발신자인 경우 처리
                                            if (auctionCreatorUid == currentUserId || messageData.senderUid == currentUserId) {
                                                val chatItem = messageData.apply {
                                                    this.auctionId = auctionId
                                                    this.creatorUid = auctionCreatorUid
                                                    this.bidderUid = messageData.senderUid // 발신자를 구매자로 간주
                                                    this.photoUrl = photoUrl
                                                }
                                                chatList.add(chatItem)
                                            }
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
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리
            }
        })
    }




}

class ChatAdapter(
    private val context: Context,
    private val chatItems: List<ChatItem>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.chat_profile_image)
        val itemTitle: TextView = itemView.findViewById(R.id.chat_item_title)
        val lastMessage: TextView = itemView.findViewById(R.id.chat_last_message)
        val chatTime: TextView = itemView.findViewById(R.id.chat_time)
        val creatorUsername: TextView = itemView.findViewById(R.id.chat_creator_username)
        val highestBid: TextView = itemView.findViewById(R.id.chat_highest_bid) // 최고 입찰가 추가
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatItem = chatItems[position]

        // Glide 사용하여 이미지 로드 (프로필 이미지)
        Glide.with(holder.profileImage.context)
            .load(chatItem.photoUrl)
            .placeholder(R.drawable.placeholder_image)
            .error(R.drawable.placeholder_image)
            .into(holder.profileImage)

        // Firebase에서 경매 데이터를 조회하여 품목명과 최고 입찰가 설정
        val auctionReference = FirebaseDatabase.getInstance().getReference("auctions").child(chatItem.auctionId)
        auctionReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 품목명 설정
                val itemName = snapshot.child("item").getValue(String::class.java) ?: "품목 없음"
                holder.itemTitle.text = itemName

                // 최고 입찰가 설정
                val highestPrice = snapshot.child("highestPrice").getValue(Long::class.java) ?: 0L
                holder.highestBid.text = if (highestPrice > 0) {
                    "${highestPrice}₩"
                } else {
                    "입찰 없음"
                }

                // 판매자 UID를 통해 사용자 이름 조회
                val creatorUid = snapshot.child("creatorUid").getValue(String::class.java)
                if (creatorUid != null) {
                    val userReference = FirebaseDatabase.getInstance().getReference("users").child(creatorUid)
                    userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val creatorUsername = userSnapshot.child("username").getValue(String::class.java) ?: "판매자 정보 없음"
                            holder.creatorUsername.text = creatorUsername
                        }

                        override fun onCancelled(error: DatabaseError) {
                            holder.creatorUsername.text = "판매자 정보 없음"
                        }
                    })
                } else {
                    holder.creatorUsername.text = "판매자 정보 없음"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                holder.itemTitle.text = "품목 없음"
                holder.creatorUsername.text = "판매자 정보 없음"
                holder.highestBid.text = "입찰 없음"
            }
        })

        // 마지막 메시지 설정
        holder.lastMessage.text = when {
            chatItem.message.isNotEmpty() -> chatItem.message
            chatItem.imageUrls.isNotEmpty() -> "이미지 메시지"
            else -> "마지막 채팅 없음"
        }

        // 채팅 시간 설정
        holder.chatTime.text = formatTimestamp(chatItem.timestamp)

        // 메시지 읽음 여부에 따라 스타일 설정
        if (chatItem.isRead) {
            holder.lastMessage.setTypeface(null, Typeface.NORMAL)
        } else {
            holder.lastMessage.setTypeface(null, Typeface.BOLD)
        }

        // 항목 클릭 리스너 추가 - 채팅 액티비티로 이동
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("auction_id", chatItem.auctionId)
            intent.putExtra("seller_uid", chatItem.creatorUid) // 판매자의 uid 전달
            intent.putExtra("bidder_uid", chatItem.bidderUid) // 구매자의 uid 전달
            context.startActivity(intent)
        }
    }



    override fun getItemCount(): Int = chatItems.size

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()

        return when {
            DateUtils.isToday(timestamp) -> {
                val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
            now - timestamp < 7 * 24 * 60 * 60 * 1000 -> {
                val daysAgo = (now - timestamp) / (24 * 60 * 60 * 1000)
                "${daysAgo}일 전"
            }
            else -> {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
}

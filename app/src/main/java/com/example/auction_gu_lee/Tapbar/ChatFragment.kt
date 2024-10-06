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
import com.google.firebase.database.*
import android.widget.ImageView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*
import android.text.format.DateUtils
import com.example.auction_gu_lee.Chat.ChatActivity
import com.example.auction_gu_lee.models.ChatItem
import com.google.firebase.auth.FirebaseAuth
import android.util.Log

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

        Log.d("ChatFragment", "Firebase 데이터 불러오기 시작")

        // Firebase에서 데이터 읽기
        loadDataFromFirebase()

        return view
    }

    private fun loadDataFromFirebase() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        Log.d("ChatFragment", "Current User ID: $currentUserId")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w("ChatFragment", "Snapshot 데이터가 존재하지 않습니다.")
                    return
                }

                Log.d("ChatFragment", "데이터 스냅샷을 불러왔습니다. 데이터 개수: ${snapshot.childrenCount}")
                chatList.clear()

                val chatMap = mutableMapOf<String, ChatItem>()

                for (auctionSnapshot in snapshot.children) {
                    val auctionId = auctionSnapshot.key ?: continue
                    Log.d("ChatFragment", "Auction ID: $auctionId")

                    val auctionCreatorUid = auctionSnapshot.child("creatorUid").getValue(String::class.java) ?: ""
                    val photoUrl = auctionSnapshot.child("photoUrl").getValue(String::class.java) ?: ""

                    val chatsSnapshot = auctionSnapshot.child("chats")
                    for (chatRoomSnapshot in chatsSnapshot.children) {
                        val chatRoomId = chatRoomSnapshot.key ?: continue
                        Log.d("ChatFragment", "Processing chatRoomId: $chatRoomId")

                        // chatRoomId를 분해하여 auctionId와 UID 추출
                        val chatRoomIdParts = chatRoomId.split("|")
                        if (chatRoomIdParts.size < 3) {
                            Log.w("ChatFragment", "Invalid chatRoomId format: $chatRoomId")
                            continue
                        }

                        val roomAuctionId = chatRoomIdParts[0]
                        val uid1 = chatRoomIdParts[1]
                        val uid2 = chatRoomIdParts[2]

                        // 방 ID의 경매 ID와 현재 경매 항목의 ID가 일치하지 않는 경우 무시
                        if (roomAuctionId != auctionId) {
                            Log.w("ChatFragment", "roomAuctionId와 auctionId가 일치하지 않습니다. roomAuctionId: $roomAuctionId, auctionId: $auctionId")
                            continue
                        }

                        // 현재 사용자가 채팅방에 참여하지 않은 경우 무시
                        if (currentUserId != uid1 && currentUserId != uid2) {
                            Log.w("ChatFragment", "현재 사용자는 이 채팅방에 참여하지 않았습니다. currentUserId: $currentUserId, uid1: $uid1, uid2: $uid2")
                            continue
                        }

                        val otherUid = if (uid1 == currentUserId) uid2 else uid1
                        Log.d("ChatFragment", "Found chatRoomId involving currentUser: $chatRoomId with otherUid: $otherUid")

                        // 최신 메시지를 가져옴
                        val lastMessageSnapshot = chatRoomSnapshot.children.lastOrNull()
                        val messageData = lastMessageSnapshot?.getValue(ChatItem::class.java) ?: continue
                        Log.d("ChatFragment", "최신 메시지 데이터: $messageData")

                        // 필요한 추가 정보 설정
                        messageData.auctionId = auctionId
                        messageData.creatorUid = auctionCreatorUid
                        messageData.bidderUid = if (currentUserId == auctionCreatorUid) otherUid else currentUserId
                        messageData.photoUrl = photoUrl

                        // 채팅방 ID를 키로 하여 중복 추가 방지
                        chatMap[chatRoomId] = messageData
                        Log.d("ChatFragment", "Added chatItem for chatRoomId $chatRoomId: $messageData")
                    }
                }
                // Map의 값들을 리스트로 변환하여 정렬 후 UI 업데이트
                chatList.clear()
                chatList.addAll(chatMap.values)
                Log.d("ChatFragment", "채팅 리스트 정렬 및 어댑터 업데이트")
                chatList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // 에러 처리
                Log.e("ChatFragment", "Database error: ${error.message}")
            }
        })
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
        val highestBid: TextView = itemView.findViewById(R.id.chat_highest_bid)
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

            // 현재 사용자가 판매자인지 구매자인지에 따라 UID 설정
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            if (currentUserId == chatItem.creatorUid) {
                // 현재 사용자가 판매자일 경우
                intent.putExtra("seller_uid", chatItem.creatorUid)
                intent.putExtra("bidder_uid", chatItem.bidderUid)
            } else {
                // 현재 사용자가 구매자일 경우
                intent.putExtra("seller_uid", chatItem.creatorUid)
                intent.putExtra("bidder_uid", currentUserId)
            }
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
}}

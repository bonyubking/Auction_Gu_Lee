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
    private lateinit var adapter: ChatAdapter  // 어댑터 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 기존의 뒤로가기 버튼 비활성화 코드 유지
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
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

        // 어댑터 초기화 (콜백 함수 전달)
        adapter = ChatAdapter(requireContext(), chatList) { auctionId, chatRoomId ->
            updateMessageAsRead(auctionId, chatRoomId)  // 메시지 읽음 처리 함수 전달
        }

        chatRecyclerView.adapter = adapter

        // Firebase 데이터 로드
        loadDataFromFirebase()

        return view
    }

    override fun onResume() {
        super.onResume()
        // 채팅 목록 UI 갱신을 위해 데이터를 다시 로드
        loadDataFromFirebase()
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

                    val auctionCreatorUid =
                        auctionSnapshot.child("creatorUid").getValue(String::class.java) ?: ""
                    val photoUrl =
                        auctionSnapshot.child("photoUrl").getValue(String::class.java) ?: ""

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
                            Log.w(
                                "ChatFragment",
                                "roomAuctionId와 auctionId가 일치하지 않습니다. roomAuctionId: $roomAuctionId, auctionId: $auctionId"
                            )
                            continue
                        }

                        // 현재 사용자가 채팅방에 참여하지 않은 경우 무시
                        if (currentUserId != uid1 && currentUserId != uid2) {
                            Log.w(
                                "ChatFragment",
                                "현재 사용자는 이 채팅방에 참여하지 않았습니다. currentUserId: $currentUserId, uid1: $uid1, uid2: $uid2"
                            )
                            continue
                        }

                        val otherUid = if (uid1 == currentUserId) uid2 else uid1
                        Log.d(
                            "ChatFragment",
                            "Found chatRoomId involving currentUser: $chatRoomId with otherUid: $otherUid"
                        )

                        // 채팅방에 읽지 않은 메시지가 있는지 확인
                        var hasUnreadMessages = false
                        for (messageSnapshot in chatRoomSnapshot.children) {
                            val isRead = messageSnapshot.child("isRead").getValue(Boolean::class.java) ?: true
                            val messageSenderUid = messageSnapshot.child("senderUid").getValue(String::class.java) ?: ""
                            if (!isRead && messageSenderUid != currentUserId) {
                                hasUnreadMessages = true
                                break
                            }
                        }

                        // 최신 메시지를 가져옴
                        val lastMessageSnapshot = chatRoomSnapshot.children.lastOrNull()
                        val messageData =
                            lastMessageSnapshot?.getValue(ChatItem::class.java) ?: continue
                        Log.d("ChatFragment", "최신 메시지 데이터: $messageData")



                            messageData.messageId = lastMessageSnapshot.key ?: ""  // 메시지 ID 설정
                            messageData.auctionId = auctionId
                            messageData.creatorUid = auctionCreatorUid
                            messageData.bidderUid =
                                if (currentUserId == auctionCreatorUid) otherUid else currentUserId
                            messageData.photoUrl = photoUrl
                            messageData.chatRoomId = chatRoomId

                        // 채팅방에 읽지 않은 메시지가 있으면 isRead를 false로 설정
                        messageData.isRead = !hasUnreadMessages

                            // 채팅방 ID를 키로 하여 중복 추가 방지
                            chatMap[chatRoomId] = messageData
                            Log.d(
                                "ChatFragment",
                                "Added chatItem for chatRoomId $chatRoomId: $messageData"
                            )

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

    private fun updateMessageAsRead(auctionId: String, chatRoomId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val chatReference = database.child(auctionId).child("chats").child(chatRoomId)

        Log.d("ChatFragment", "Updating messages in chatRoomId: $chatRoomId for auctionId: $auctionId")

        chatReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    val messageSenderUid =
                        messageSnapshot.child("senderUid").getValue(String::class.java) ?: continue
                    val isRead =
                        messageSnapshot.child("isRead").getValue(Boolean::class.java) ?: false

                    if (!isRead && messageSenderUid != currentUserId) {
                        // 메시지를 읽음 처리
                        messageSnapshot.ref.child("isRead").setValue(true)

                        Log.d("ChatFragment", "Updating message as read for chatRoomId: $chatRoomId")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatFragment", "Failed to update message as read: ${error.message}")
            }
        })
    }
}

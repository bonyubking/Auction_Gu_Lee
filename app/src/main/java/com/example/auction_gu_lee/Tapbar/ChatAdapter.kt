package com.example.auction_gu_lee.Tapbar

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.Chat.ChatActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.ChatItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val context: Context,
    private val chatItems: List<ChatItem>,
    private val onMessageRead: (auctionId: String, chatRoomId: String) -> Unit  // 함수 추가
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val database = FirebaseDatabase.getInstance().reference

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.chat_profile_image)
        val itemTitle: TextView = itemView.findViewById(R.id.chat_item_title)
        val lastMessage: TextView = itemView.findViewById(R.id.chat_last_message)
        val chatTime: TextView = itemView.findViewById(R.id.chat_time)
        val creatorUsername: TextView = itemView.findViewById(R.id.chat_creator_username)
        val highestBid: TextView = itemView.findViewById(R.id.chat_highest_bid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
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
                            Log.e("ChatAdapter", "Firebase error: ${error.message}")
                        }
                    })
                } else {
                    holder.creatorUsername.text = "판매자 정보 없음"
                    Log.e("ChatAdapter", "creatorUid is null for auctionId: ${chatItem.auctionId}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                holder.itemTitle.text = "품목 없음"
                holder.creatorUsername.text = "판매자 정보 없음"
                holder.highestBid.text = "입찰 없음"
                Log.e("ChatAdapter", "Firebase error: ${error.message}")
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

        // 메시지 읽음 여부에 따라 텍스트 스타일 설정
        if (chatItem.isRead) {
            holder.lastMessage.setTypeface(null, Typeface.NORMAL)
        } else {
            holder.lastMessage.setTypeface(null, Typeface.BOLD)
        }


        // 항목 클릭 리스너 추가 - 채팅 액티비티로 이동
        holder.itemView.setOnClickListener {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == null) {
                Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 메시지 읽음 처리 콜백 호출
            onMessageRead(chatItem.auctionId, chatItem.chatRoomId)

            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("auction_id", chatItem.auctionId)

            if (currentUserId == chatItem.creatorUid) {
                intent.putExtra("seller_uid", chatItem.creatorUid)
                intent.putExtra("bidder_uid", chatItem.bidderUid)
            } else {
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

    private fun updateMessageAsRead(chatRoomId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val chatReference = database.child("auctions").child("chats").child(chatRoomId)

        chatReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (messageSnapshot in snapshot.children) {
                    val messageSenderUid = messageSnapshot.child("senderUid").getValue(String::class.java) ?: continue
                    val isRead = messageSnapshot.child("isRead").getValue(Boolean::class.java) ?: false

                    if (!isRead && messageSenderUid != currentUserId) {
                        // 메시지 읽음 상태를 true로 업데이트
                        messageSnapshot.ref.child("isRead").setValue(true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatFragment", "Failed to update message as read: ${error.message}")
            }
        })
    }

}
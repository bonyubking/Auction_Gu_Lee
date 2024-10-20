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
import com.google.firebase.database.*
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

    fun markAllChatsAsRead() {
        chatItems.forEach { chatItem ->
            if (!chatItem.isRead) {
                chatItem.isRead = true
                onMessageRead(chatItem.auctionId, chatItem.chatRoomId)
            }
        }
        notifyDataSetChanged()
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

                // 판매자 UID를 통해 사용
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
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            // 현재 사용자가 판매자인지 구매자인지 확인
            val bidderUid = if (currentUserId == chatItem.creatorUid) {
                chatItem.bidderUid  // 현재 사용자가 판매자라면 구매자 UID를 사용
            } else {
                currentUserId  // 현재 사용자가 구매자라면 본인의 UID를 사용
            }

            val sellerUid = if (currentUserId == chatItem.creatorUid) {
                currentUserId  // 현재 사용자가 판매자라면 본인의 UID를 사용
            } else {
                chatItem.creatorUid  // 현재 사용자가 구매자라면 판매자 UID를 사용
            }

            // Intent에 auction_id와 UID들 전달
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra("auction_id", chatItem.auctionId)
                putExtra("seller_uid", sellerUid)
                putExtra("bidder_uid", bidderUid)
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
}

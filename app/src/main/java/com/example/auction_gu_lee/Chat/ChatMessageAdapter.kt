package com.example.auction_gu_lee.Chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.ChatItem
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter(
    private val context: Context,
    private val messages: List<ChatItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_SENT = 1
        const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderUid == FirebaseAuth.getInstance().currentUser?.uid) {
            VIEW_TYPE_SENT // 내가 보낸 메시지
        } else {
            VIEW_TYPE_RECEIVED // 받은 메시지
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatItem = messages[position]
        if (holder is SentMessageViewHolder) {
            bindMessageViewHolder(holder, chatItem)
        } else if (holder is ReceivedMessageViewHolder) {
            bindMessageViewHolder(holder, chatItem)
        }
    }

    private fun bindMessageViewHolder(holder: RecyclerView.ViewHolder, chatItem: ChatItem) {
        if (holder is SentMessageViewHolder) {
            // 텍스트 메시지 설정
            if (chatItem.message.isNotEmpty()) {
                holder.messageText.visibility = View.VISIBLE
                holder.messageText.text = chatItem.message
            } else {
                holder.messageText.visibility = View.GONE
            }

            // 시간 설정
            holder.messageTime.text = formatTimestamp(chatItem.timestamp)

            // 이미지 메시지 처리
            if (chatItem.imageUrls.isNotEmpty()) {
                holder.imageContainer.visibility = View.VISIBLE
                holder.imageContainer.removeAllViews() // 기존 이미지 뷰 초기화

                chatItem.imageUrls.forEach { imageUrl ->
                    val imageView = ImageView(context)
                    imageView.layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                        setMargins(8, 8, 8, 8)
                    }
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .into(imageView)
                    holder.imageContainer.addView(imageView)
                }
            } else {
                holder.imageContainer.visibility = View.GONE
            }
        } else if (holder is ReceivedMessageViewHolder) {
            // 텍스트 메시지 설정
            if (chatItem.message.isNotEmpty()) {
                holder.messageText.visibility = View.VISIBLE
                holder.messageText.text = chatItem.message
            } else {
                holder.messageText.visibility = View.GONE
            }

            // 시간 설정
            holder.messageTime.text = formatTimestamp(chatItem.timestamp)

            // 이미지 메시지 처리
            if (chatItem.imageUrls.isNotEmpty()) {
                holder.imageContainer.visibility = View.VISIBLE
                holder.imageContainer.removeAllViews() // 기존 이미지 뷰 초기화

                chatItem.imageUrls.forEach { imageUrl ->
                    val imageView = ImageView(context)
                    imageView.layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                        setMargins(8, 8, 8, 8)
                    }
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .into(imageView)
                    holder.imageContainer.addView(imageView)
                }
            } else {
                holder.imageContainer.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    // 타임스탬프를 포맷하는 메서드 추가
    private fun formatTimestamp(timestamp: Long?): String {
        return if (timestamp != null && timestamp > 0) {
            try {
                val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            } catch (e: Exception) {
                "시간 형식 오류" // 포맷 오류 시 표시할 기본 메시지
            }
        } else {
            "시간 없음" // 타임스탬프가 유효하지 않을 경우 기본 메시지
        }
    }

    // 보낸 메시지 ViewHolder
    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val messageTime: TextView = itemView.findViewById(R.id.message_time)
        val imageContainer: LinearLayout = itemView.findViewById(R.id.image_container) // 이미지 컨테이너 추가
    }

    // 받은 메시지 ViewHolder
    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val messageTime: TextView = itemView.findViewById(R.id.message_time)
        val imageContainer: LinearLayout = itemView.findViewById(R.id.image_container) // 이미지 컨테이너 추가
    }
}

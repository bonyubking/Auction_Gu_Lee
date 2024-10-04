package com.example.auction_gu_lee.models

// Firebase에서 가져올 데이터 모델
data class ChatItem(
    val item: String = "",
    val creatorUsername: String = "",
    val photoUrl: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L,
    var isRead: Boolean = true // 읽음 여부 표시

)
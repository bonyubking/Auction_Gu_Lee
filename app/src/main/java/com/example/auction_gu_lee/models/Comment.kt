package com.example.auction_gu_lee.models

// Comment.kt
data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val auctionId: String = "",  // 정확한 필드 이름 확인
    val timestamp: Long = 0L, // 필요시 추가 필드
)
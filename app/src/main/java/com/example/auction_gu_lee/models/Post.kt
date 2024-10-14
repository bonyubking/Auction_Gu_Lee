package com.example.auction_gu_lee.models

// Post.kt
data class Post(
    val postId: String = "",
    val userId: String = "",
    val item: String = "",
    val desiredPrice: Long = 0L,
    val quantity: String = "",
    val detail: String = "",
    val timestamp: Long = 0L
)


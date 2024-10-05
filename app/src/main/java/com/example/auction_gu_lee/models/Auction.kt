package com.example.auction_gu_lee.models

data class Auction(
    val item: String? = null,
    val username: String? = null,
    val quantity: String? = null,
    val detail: String? = null,
    val startingPrice: Long? = null,  // Long 타입으로 변경
    val remainingTime: String? = null,
    val highestPrice: Long? = null,   // Long 타입으로 변경
    val photoUrl: String? = null,
    val timestamp: Long? = null,
    val endTime: Long? = null,
    val creatorUid: String? = null,
    val uid: String? = null,
    val chats: Map<String, ChatItem>? = null // 새로운 필드 추가
)

package com.example.auction_gu_lee.models

data class Auction(
    var id: String? =  null,
    val item: String? = null,
    val username: String? = null,
    val quantity: String? = null,
    val detail: String? = null,
    var startingPrice: Long? = 0L,  // 기본값 0L로 설정
    val remainingTime: String? = null,
    var highestPrice: Long? = 0L,  // 기본값 0L로 설정
    var photoUrl: String? = null,
    val timestamp: Long? = null,
    val endTime: Long? = null,
    val creatorUid: String? = null,
    var highestBidderUid: String? = null,
    val uid: String? = null,
    val chats: Map<String, ChatItem>? = null,
    var favoritesCount: Int? = 0,    // 기본값 0으로 설정
    var biddersCount: Int? = 0,      // 기본값 0으로 설정
    var participants: MutableMap<String, Long> = mutableMapOf(),
    val status: String = ""
)
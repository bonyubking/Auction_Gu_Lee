package com.example.auction_gu_lee.models

data class Auction(
    var id: String? = null,
    val item: String? = null,
    val username: String? = null,
    val quantity: String? = null,
    val detail: String? = null,
    val startingPrice: Long? = null,
    val remainingTime: String? = null,
    var highestPrice: Long? = null,
    val photoUrl: String? = null,
    val timestamp: Long? = null,
    val endTime: Long? = null,
    val creatorUid: String? = null,
    var highestBidderUid: String? = null,
    val uid: String? = null,
    val chats: Map<String, ChatItem>? = null,
    var favoritesCount: Int? = 0,    // 찜 수
    var biddersCount: Int? = 0 ,      // 입찰자 수
    var participants: MutableMap<String, Long> = mutableMapOf()  // UID와 입찰가를 저장할 수 있도록 변경
)
package com.example.auction_gu_lee.models

data class ChatItem(
    var auctionId: String = "",
    var creatorUid: String = "",
    var item: String = "",
    var creatorUsername: String = "",
    var photoUrl: String = "",
    var uid: String = "",
    var senderUid: String = "",
    var message: String = "",
    var bidderUid: String = "",
    var timestamp: Long = 0L,
    var isRead: Boolean = true,
    var imageUrls: List<String> = listOf()
)
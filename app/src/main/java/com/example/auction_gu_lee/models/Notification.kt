package com.example.auction_gu_lee.models

data class Notification(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    var read: Boolean = false,
    val type: String = "",  // 예: "bid", "auction_end" 등
    val relatedAuctionId: String = ""
)
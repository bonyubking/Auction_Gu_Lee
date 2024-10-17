package com.example.auction_gu_lee.models

data class Notification(
    var id: String = "",
    val message: String? = null,
    val timestamp: Long? = null,
    val type: String? = null, // 예: "bid", "auction_end"
    val relatedAuctionId: String? = null,
    var read: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "message" to message,
            "timestamp" to timestamp,
            "type" to type,
            "relatedAuctionId" to relatedAuctionId,
            "read" to read
        )
    }
}

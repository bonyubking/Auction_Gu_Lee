package com.example.auction_gu_lee.models

data class Notification(
    val id: String? = null,
    val message: String? = null,
    val relatedAuctionId: String? = null,
    val relatedPostId: String? = null,
    val commentAuctionId: String? = null,
    val timestamp: Long? = null,
    val type: String? = null,
    var read: Boolean = false
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "message" to message,
            "timestamp" to timestamp,
            "type" to type,
            "relatedAuctionId" to relatedAuctionId,
            "relatedPostId" to relatedPostId,
            "commentAuctionId" to commentAuctionId,
            "read" to read
        )
    }
}

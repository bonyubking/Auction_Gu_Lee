package com.example.auction_gu_lee.models

data class Notification(
    val id: String? = null,
    val message: String? = null,
    val relatedAuctionId: String? = null,
    val relatedPostId: String? = null, // 새로 추가된 필드
    val timestamp: Long? = null,
    val type: String? = null,
    val read: Boolean = false
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

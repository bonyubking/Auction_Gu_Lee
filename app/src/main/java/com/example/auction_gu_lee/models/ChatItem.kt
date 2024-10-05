package com.example.auction_gu_lee.models

// Firebase에서 가져올 데이터 모델
data class ChatItem(
    var auctionId: String = "",     // 경매 ID 추가, var로 수정
    var creatorUid: String = "",    // 판매자 UID 추가
    val item: String = "",
    val creatorUsername: String = "",
    var photoUrl: String = "",
    val Uid : String = "",
    var senderUid: String = "",
    val message : String = "",
    var bidderUid : String = "",
    val timestamp: Long = 0L,
    var isRead: Boolean = true,
    var imageUrls: List<String> = listOf() // 이미지 URL 리스트 추가
)
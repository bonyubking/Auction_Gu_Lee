package com.example.auction_gu_lee.models

data class Auction(
    val item: String? = null,
    val quantity: String? = null,
    val detail: String? = null,
    val moredetail: String? = null,
    val startingPrice: String? = null,
    val remainingTime: String? = null,  // 남은 시간 필드 추가
    val highestPrice: String? = null,
    val photoUrl: String? = null,
    val timestamp: Long? = null,
    val endTime: Long? = null, // 여기서 long 타입을 사용하고 있는지 확인 // End time in milliseconds (timestamp)
    val creatorUsername: String? = null,
    )

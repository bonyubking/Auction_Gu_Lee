package com.example.auction_gu_lee.models

data class User(
    val uid: String? = "",   // Firebase UID
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    var loggedin: Boolean = false,
    )
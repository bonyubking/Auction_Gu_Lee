package com.example.auction_gu_lee

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,  // Primary Key
    val username: String,
    val name: String,
    val email: String,
    val password: String,
    val phoneNumber: String
)

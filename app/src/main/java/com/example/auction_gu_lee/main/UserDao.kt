package com.example.auction_gu_lee

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserDao {
    @Insert
    fun insertUser(user: User)

    @Query("SELECT * FROM user_table WHERE email = :email AND password = :password")
    fun getUser(email: String, password: String): User?  // 반환 타입 명확히 지정
}

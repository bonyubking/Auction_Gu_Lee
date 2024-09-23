package com.example.auction_gu_lee

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Intent에서 username 가져오기
        val username = intent.getStringExtra("username")

        // 텍스트뷰에 "username 님, 반갑습니다!" 문구 표시
        val welcomeTextView = findViewById<TextView>(R.id.welcomeTextView)
        welcomeTextView.text = "$username 님, 반갑습니다!"
    }
}
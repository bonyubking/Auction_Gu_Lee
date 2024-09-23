package com.example.auction_gu_lee

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 오프닝 이미지 설정
        val splashImage: ImageView = findViewById(R.id.splash_image)
        splashImage.setImageResource(R.drawable.opening_imang) // 이미지 파일 이름을 바꿔주세요

        // 3초 후 페이드 아웃 후 메인 화면으로 전환
        Handler(Looper.getMainLooper()).postDelayed({

            val intent = Intent(this@SplashActivity, LobbyActivty::class.java)
            startActivity(intent)
            overridePendingTransition(0,0)
            finish()
        }, 3000) // 3초 대기
    }
}

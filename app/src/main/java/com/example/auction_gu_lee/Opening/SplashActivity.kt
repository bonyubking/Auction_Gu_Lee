package com.example.auction_gu_lee.Opening

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.auction_gu_lee.Lobby.LobbyActivity
import com.example.auction_gu_lee.Main.MainActivity
import com.example.auction_gu_lee.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 오프닝 이미지 설정
        val splashImage: ImageView = findViewById(R.id.splash_image)
        splashImage.setImageResource(R.drawable.opening_imang) // 이미지 파일 이름을 바꿔주세요

        // 자동 로그인 여부 확인
        val sharedPreferences = getSharedPreferences("autoLoginPrefs", Context.MODE_PRIVATE)
        val isAutoLoginEnabled = sharedPreferences.getBoolean("autoLogin", false)  // 자동 로그인 여부 확인

        // 3초 후 화면 전환
        Handler(Looper.getMainLooper()).postDelayed({

            // 자동 로그인 여부에 따라 화면 전환
            if (isAutoLoginEnabled) {
                // 자동 로그인 설정 시 MainActivity로 바로 이동
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
            } else {
                // 자동 로그인이 설정되어 있지 않으면 LobbyActivity로 이동
                val intent = Intent(this@SplashActivity, LobbyActivity::class.java)
                startActivity(intent)
            }

            overridePendingTransition(0, 0)
            finish()
        }, 3000) // 3초 대기
    }
}

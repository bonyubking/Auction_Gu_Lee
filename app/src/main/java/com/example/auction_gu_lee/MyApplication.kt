package com.example.auction_gu_lee

import android.app.Application
import com.google.firebase.FirebaseApp
import com.kakao.sdk.common.KakaoSdk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 카카오 SDK 초기화
        KakaoSdk.init(this, "73062d9c979d7cd22d10ff5e6c18eb04")
        FirebaseApp.initializeApp(this)
    }
}
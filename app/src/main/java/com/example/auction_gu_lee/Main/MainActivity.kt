package com.example.auction_gu_lee.Main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.Tapbar.ChatFragment
import com.example.auction_gu_lee.Tapbar.HomeFragment
import com.example.auction_gu_lee.Tapbar.PastFragment
import com.example.auction_gu_lee.Tapbar.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // BottomNavigationView 설정
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Intent에서 전달된 값 확인
        val fragmentName = intent.getStringExtra("fragment")  // "home" 값을 받을 변수
        val auctionId = intent.getStringExtra("auction_id")   // "auction_id" 값을 받을 변수

        // 기본 프래그먼트 설정
        if (fragmentName == "home" && auctionId != null) {
            // HomeFragment로 이동하며 auction_id를 전달
            val homeFragment = HomeFragment().apply {
                arguments = Bundle().apply {
                    putString("auction_id", auctionId)
                }
            }
            replaceFragment(homeFragment)
        } else {
            // 기본적으로 HomeFragment를 표시
            replaceFragment(HomeFragment())
        }

        // BottomNavigationView의 메뉴 아이템 클릭 시 프래그먼트 교체
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_auction -> {
                    replaceFragment(PastFragment())
                    true
                }
                R.id.nav_chat -> {
                    replaceFragment(ChatFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    // 프래그먼트를 교체하는 함수
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame_layout, fragment)
        transaction.commit()
    }
}
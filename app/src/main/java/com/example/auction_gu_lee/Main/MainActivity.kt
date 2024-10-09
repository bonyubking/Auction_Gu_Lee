package com.example.auction_gu_lee.Main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.auction_gu_lee.Lobby.LobbyActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.Tapbar.ChatFragment
import com.example.auction_gu_lee.Tapbar.HomeFragment
import com.example.auction_gu_lee.Tapbar.PastFragment
import com.example.auction_gu_lee.Tapbar.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth  // FirebaseAuth 인스턴스 변수 선언
    private lateinit var database: DatabaseReference  // Firebase Database Reference
    private var userUid: String? = null  // 현재 사용자 UID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        // 로그인 상태 확인
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // 로그인이 되어 있지 않은 경우 LobbyActivity로 이동
            val intent = Intent(this, LobbyActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        userUid = currentUser.uid

        // 사용자의 loggedin 상태를 true로 설정하고, 연결이 끊어지면 false로 설정
        setUserOnlineStatus(true)

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

    private fun setUserOnlineStatus(isOnline: Boolean) {
        if (userUid == null) return

        val userStatusRef = database.child(userUid!!).child("loggedin")

        if (isOnline) {
            // 온라인 상태 설정
            userStatusRef.setValue(true)

            // 연결이 끊어지면 자동으로 loggedin을 false로 설정
            userStatusRef.onDisconnect().setValue(false)
        } else {
            // 오프라인 상태 설정
            userStatusRef.setValue(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 앱이 정상적으로 종료될 때 loggedin 상태를 false로 설정
        setUserOnlineStatus(false)
    }

    override fun onPause() {
        super.onPause()
        // 앱이 백그라운드로 이동할 때도 loggedin 상태를 false로 설정
        setUserOnlineStatus(false)
    }

    override fun onResume() {
        super.onResume()
        // 앱이 포그라운드로 돌아올 때 loggedin 상태를 true로 설정
        setUserOnlineStatus(true)
    }
}

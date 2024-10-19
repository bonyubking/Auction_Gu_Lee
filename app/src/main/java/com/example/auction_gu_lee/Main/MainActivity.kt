package com.example.auction_gu_lee.Main

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.auction_gu_lee.Lobby.LobbyActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.Tapbar.ChatFragment
import com.example.auction_gu_lee.Tapbar.HomeFragment
import com.example.auction_gu_lee.Tapbar.PastFragment
import com.example.auction_gu_lee.Tapbar.PostFragment
import com.example.auction_gu_lee.Tapbar.ProfileFragment
import com.example.auction_gu_lee.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.badge.BadgeDrawable
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth  // FirebaseAuth 인스턴스 변수 선언
    private lateinit var database: DatabaseReference  // Firebase Database Reference
    private var userUid: String? = null  // 현재 사용자 UID

    // BottomNavigationView와 BadgeDrawable을 클래스 레벨에서 선언
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var badge: BadgeDrawable

    // 채팅 데이터 참조를 위한 변수 선언
    private lateinit var chatsRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 이제 binding을 사용하여 뷰에 접근
        bottomNavigationView = binding.bottomNavigation

        // FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

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

        // Intent에서 전달된 값 확인
        val fragmentName = intent.getStringExtra("fragment")  // "home" 값을 받을 변수
        val auctionId = intent.getStringExtra("auction_id")   // "auction_id" 값을 받을 변수

        // 기본 프래그먼트 설정
        when (fragmentName) {
            "home" -> {
                if (auctionId != null) {
                    // HomeFragment로 이동하며 auction_id를 전달
                    val homeFragment = HomeFragment().apply {
                        arguments = Bundle().apply {
                            putString("auction_id", auctionId)
                        }
                    }
                    replaceFragment(homeFragment)
                } else {
                    replaceFragment(HomeFragment())
                }
            }
            "post" -> {
                // PostFragment로 이동
                replaceFragment(PostFragment())
            }
            else -> {
                // 기본적으로 HomeFragment를 표시
                replaceFragment(HomeFragment())
            }
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
                R.id.nav_board -> {
                    replaceFragment(PostFragment())
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

        setupChatBadge()
    }


    private fun setupChatBadge() {
        val chatMenuItemId = R.id.nav_chat

        // BadgeDrawable 생성 및 설정
        badge = bottomNavigationView.getOrCreateBadge(chatMenuItemId)
        badge.isVisible = false
        badge.backgroundColor = Color.RED
        badge.badgeGravity = BadgeDrawable.TOP_END
        badge.maxCharacterCount = 3

        // Firebase Realtime Database에서 읽지 않은 채팅 수 감지
        database.child("auctions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var unreadCount = 0
                for (auctionSnapshot in snapshot.children) {
                    val chatsSnapshot = auctionSnapshot.child("chats")
                    for (chatRoomSnapshot in chatsSnapshot.children) {
                        val bidderUid = chatRoomSnapshot.child("bidderUid").getValue(String::class.java)
                        val sellerUid = auctionSnapshot.child("creatorUid").getValue(String::class.java)

                        if (userUid == bidderUid || userUid == sellerUid) {
                            val messagesSnapshot = chatRoomSnapshot.child("messages")
                            for (messageSnapshot in messagesSnapshot.children) {
                                val isRead = messageSnapshot.child("isRead").getValue(Boolean::class.java) ?: true
                                val senderUid = messageSnapshot.child("senderUid").getValue(String::class.java)
                                if (!isRead && senderUid != userUid) {
                                    unreadCount++
                                }
                            }
                        }
                    }
                }

                if (unreadCount > 0) {
                    badge.isVisible = true
                    badge.number = unreadCount
                } else {
                    badge.isVisible = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "채팅 배지 업데이트 실패: ${error.message}")
            }
        })
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

    }

    override fun onResume() {
        super.onResume()
        // 앱이 포그라운드로 돌아올 때 loggedin 상태를 true로 설정
        setUserOnlineStatus(true)
    }
}

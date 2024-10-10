package com.example.auction_gu_lee.Tapbar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.example.auction_gu_lee.Lobby.LobbyActivity
import com.example.auction_gu_lee.Profile.MyAuction.BiddingHistoryActivity
import com.example.auction_gu_lee.Profile.MyAuction.RecentlyViewedActivity
import com.example.auction_gu_lee.Profile.MyAuction.SalesHistoryActivity
import com.example.auction_gu_lee.Profile.MyAuction.WishlistActivity
import com.example.auction_gu_lee.Profile.Settings.NotificationSettingActivity
import com.example.auction_gu_lee.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 뒤로가기 버튼 비활성화
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 아무 동작도 하지 않게 뒤로가기 버튼 무효화
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_profile.xml 레이아웃을 인플레이트
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FirebaseAuth를 통해 현재 로그인된 사용자 가져오기
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            // Firebase Realtime Database에서 추가 사용자 정보 가져오기
            val databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId)
            databaseReference.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val userName =
                        snapshot.child("username").getValue(String::class.java) ?: "사용자 없음"
                    val userIdTextView = view.findViewById<TextView>(R.id.tv_user_Id)
                    userIdTextView.text = "반갑습니다, $userName 님"
                } else {
                    view.findViewById<TextView>(R.id.tv_user_Id).text = "사용자 정보를 불러올 수 없습니다."
                }
            }.addOnFailureListener {
                view.findViewById<TextView>(R.id.tv_user_Id).text = "데이터베이스 오류가 발생했습니다."
            }
        } else {
            view.findViewById<TextView>(R.id.tv_user_Id).text = "로그인된 사용자가 없습니다."
        }

        // 관심 목록 버튼 클릭 리스너 설정
        val wishlistButton = view.findViewById<TextView>(R.id.btn_wishlist)
        wishlistButton.setOnClickListener {
            val intent = Intent(requireContext(), WishlistActivity::class.java)
            startActivity(intent)
        }

        // 나머지 버튼 클릭 리스너 설정
        setUpButtonListeners(view)
    }

    private fun setUpButtonListeners(view: View) {
        // 최근 본 내역 버튼 클릭 리스너 설정
        val recentViewButton = view.findViewById<TextView>(R.id.btn_recently_viewed)
        recentViewButton.setOnClickListener {
            val intent = Intent(requireContext(), RecentlyViewedActivity::class.java)
            startActivity(intent)
        }

        // 판매 내역 버튼 클릭 리스너 설정
        val salesHistoryButton = view.findViewById<TextView>(R.id.btn_sales_history)
        salesHistoryButton.setOnClickListener {
            // 판매 내역 액티비티로 이동하는 인텐트 설정
            val intent = Intent(requireContext(), SalesHistoryActivity::class.java)
            startActivity(intent)
        }

        // 입찰 내역 버튼 클릭 리스너 설정
        val bidHistoryButton = view.findViewById<TextView>(R.id.btn_bid_history)
        bidHistoryButton.setOnClickListener {
            val intent = Intent(requireContext(), BiddingHistoryActivity::class.java)
            startActivity(intent)
        }

        // 내 비지니스 광고하기 버튼 클릭 리스너 설정
        val businessAdButton = view.findViewById<TextView>(R.id.btn_business_ad)
        businessAdButton.setOnClickListener {
            Toast.makeText(requireContext(), "내 비지니스 광고하기 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 알림 설정 버튼 클릭 리스너 설정
        val notificationSettingsButton = view.findViewById<TextView>(R.id.btn_notification_settings)
        notificationSettingsButton.setOnClickListener {
            val intent = Intent(requireContext(), NotificationSettingActivity::class.java)
            startActivity(intent)
        }

        // 사용자 설정 버튼 클릭 리스너 설정
        val userSettingButton = view.findViewById<TextView>(R.id.btn_user_setting)
        userSettingButton.setOnClickListener {
            Toast.makeText(requireContext(), "사용자 설정 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }


        // 최신 버전 업데이트 버튼 클릭 리스너 설정
        val updateButton = view.findViewById<TextView>(R.id.btn_update)
        updateButton.setOnClickListener {
            Toast.makeText(requireContext(), "최신 버전 업데이트 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 기타 설정 버튼 클릭 리스너 설정
        val otherSettingsButton = view.findViewById<TextView>(R.id.btn_other_settings)
        otherSettingsButton.setOnClickListener {
            Toast.makeText(requireContext(), "기타 설정 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 로그아웃 버튼 클릭 리스너 설정
        val logoutButton = view.findViewById<TextView>(R.id.btn_logout)
        logoutButton.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        val currentActivity = activity
        if (currentActivity != null) {
            val builder = AlertDialog.Builder(currentActivity)
            builder.setTitle("로그아웃")
            builder.setMessage("정말 로그아웃 하시겠습니까?")
            builder.setPositiveButton("예") { _, _ ->
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Log.d("ProfileFragment", "Logging out user: ${user.uid}")
                    // Realtime Database에서 loggedin을 false로 업데이트
                    val databaseReference = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
                    val updates = mapOf<String, Any>("loggedin" to false)

                    // 로그아웃 전에 데이터베이스 업데이트 수행
                    databaseReference.updateChildren(updates).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("ProfileFragment", "loggedin set to false successfully.")
                            // FirebaseAuth에서 로그아웃
                            FirebaseAuth.getInstance().signOut()
                            Log.d("ProfileFragment", "FirebaseAuth signOut completed.")

                            // SharedPreferences 초기화 및 autoLogin을 false로 설정
                            val sharedPreferences = requireContext().getSharedPreferences("autoLoginPrefs", Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("autoLogin", false)
                            editor.remove("email")
                            editor.remove("password")
                            editor.apply()
                            Log.d("ProfileFragment", "autoLogin set to false and credentials removed.")

                            // LobbyActivity로 이동
                            moveToLobbyActivity()
                        } else {
                            Log.e("ProfileFragment", "Failed to update loggedin: ${task.exception?.message}")
                            Toast.makeText(currentActivity, "로그아웃 상태 업데이트 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("ProfileFragment", "No current user found during logout.")
                    Toast.makeText(currentActivity, "사용자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNegativeButton("아니요") { dialog, _ -> dialog.dismiss() }
            builder.show()
        } else {
            Toast.makeText(context, "현재 액티비티를 찾을 수 없습니다. 로그아웃할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveToLobbyActivity() {
        val currentActivity = activity
        if (currentActivity != null) {
            val intent = Intent(currentActivity, LobbyActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            Log.d("ProfileFragment", "Navigated to LobbyActivity.")
        }
    }
}

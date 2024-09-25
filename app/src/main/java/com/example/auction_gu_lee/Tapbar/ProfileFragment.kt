package com.example.auction_gu_lee.Tapbar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.auction_gu_lee.R
import android.widget.TextView
import com.example.auction_gu_lee.Profile.DeleteDataActivity
import androidx.activity.OnBackPressedCallback  // 뒤로가기 비활성화를 위한 import 추가
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 뒤로가기 버튼 비활성화
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 아무 동작도 하지 않게 뒤로가기 버튼 무효화
                // 이곳에 필요한 동작을 추가할 수 있음
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

    // onViewCreated에서 버튼 클릭 리스너 설정
    // onViewCreated에서 버튼 클릭 리스너 설정
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FirebaseAuth를 통해 현재 로그인된 사용자 가져오기
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // 사용자 ID 가져오기 (Firebase에서 UID로 사용됨)
            val userId = currentUser.uid

            // Firebase Realtime Database에서 추가 사용자 정보 가져오기
            val databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId)
            databaseReference.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val userName = snapshot.child("username").getValue(String::class.java) ?: "사용자 없음"

                    // TextView를 찾아서 사용자 이름 출력
                    val userIdTextView = view.findViewById<TextView>(R.id.tv_user_Id)
                    userIdTextView.text = "반갑습니다, $userName 님"
                } else {
                    // 사용자 정보가 없을 때 처리
                    val userIdTextView = view.findViewById<TextView>(R.id.tv_user_Id)
                    userIdTextView.text = "사용자 정보를 불러올 수 없습니다."
                }
            }.addOnFailureListener {
                // 데이터베이스 읽기 실패 시 처리
                val userIdTextView = view.findViewById<TextView>(R.id.tv_user_Id)
                userIdTextView.text = "데이터베이스 오류가 발생했습니다."
            }
        } else {
            // 로그인되지 않은 경우 처리
            val userIdTextView = view.findViewById<TextView>(R.id.tv_user_Id)
            userIdTextView.text = "로그인된 사용자가 없습니다."
        }

        // 관심 목록 버튼 클릭 리스너 설정
        val wishlistButton = view.findViewById<TextView>(R.id.btn_wishlist)
        wishlistButton.setOnClickListener {
            Toast.makeText(requireContext(), "관심 목록 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 최근 본 내역 버튼 클릭 리스너 설정
        val recentViewButton = view.findViewById<TextView>(R.id.btn_recent_view)
        recentViewButton.setOnClickListener {
            Toast.makeText(requireContext(), "최근 본 내역 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 판매 내역 버튼 클릭 리스너 설정
        val salesHistoryButton = view.findViewById<TextView>(R.id.btn_sales_history)
        salesHistoryButton.setOnClickListener {
            Toast.makeText(requireContext(), "판매 내역 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 입찰 내역 버튼 클릭 리스너 설정
        val bidHistoryButton = view.findViewById<TextView>(R.id.btn_bid_history)
        bidHistoryButton.setOnClickListener {
            Toast.makeText(requireContext(), "입찰 내역 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 내 비지니스 광고하기 버튼 클릭 리스너 설정
        val businessAdButton = view.findViewById<TextView>(R.id.btn_business_ad)
        businessAdButton.setOnClickListener {
            Toast.makeText(requireContext(), "내 비지니스 광고하기 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 알림 설정 버튼 클릭 리스너 설정
        val notificationSettingsButton = view.findViewById<TextView>(R.id.btn_notification_settings)
        notificationSettingsButton.setOnClickListener {
            Toast.makeText(requireContext(), "알림 설정 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 채팅 버튼 클릭 리스너 설정
        val chatButton = view.findViewById<TextView>(R.id.btn_chat)
        chatButton.setOnClickListener {
            Toast.makeText(requireContext(), "채팅 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 키워드 알림 버튼 클릭 리스너 설정
        val keywordNotificationsButton = view.findViewById<TextView>(R.id.btn_keyword_notifications)
        keywordNotificationsButton.setOnClickListener {
            Toast.makeText(requireContext(), "키워드 알림 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 사용자 설정 버튼 클릭 리스너 설정
        val userSettingButton = view.findViewById<TextView>(R.id.btn_user_setting)
        userSettingButton.setOnClickListener {
            Toast.makeText(requireContext(), "사용자 설정 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 언어 설정 버튼 클릭 리스너 설정
        val languageSettingButton = view.findViewById<TextView>(R.id.btn_language_setting)
        languageSettingButton.setOnClickListener {
            Toast.makeText(requireContext(), "언어 설정 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 캐시 데이터 삭제하기 버튼 클릭 리스너 설정
        val deleteDataButton = view.findViewById<TextView>(R.id.btn_delete_data)
        deleteDataButton.setOnClickListener {
            // DeleteDataActivity 호출
            val intent = Intent(requireContext(), DeleteDataActivity::class.java)
            startActivity(intent)  // DeleteDataActivity로 이동
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
    }
}

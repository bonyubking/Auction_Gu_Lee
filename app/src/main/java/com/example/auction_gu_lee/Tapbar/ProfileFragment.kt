package com.example.auction_gu_lee.Tapbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.auction_gu_lee.R
import android.widget.TextView

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_profile.xml 레이아웃을 인플레이트
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    // onViewCreated에서 버튼 클릭 리스너 설정
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        // 판매 중 버튼 클릭 리스너 설정
        val currentSalesButton = view.findViewById<TextView>(R.id.btn_current_sales)
        currentSalesButton.setOnClickListener {
            Toast.makeText(requireContext(), "판매 중 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 판매 완료 버튼 클릭 리스너 설정
        val completedSalesButton = view.findViewById<TextView>(R.id.btn_completed_sales)
        completedSalesButton.setOnClickListener {
            Toast.makeText(requireContext(), "판매 완료 버튼 클릭됨", Toast.LENGTH_SHORT).show()
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

        // 알림 수신 설정 버튼 클릭 리스너 설정
        val receiveNotificationsButton = view.findViewById<TextView>(R.id.btn_receive_notifications)
        receiveNotificationsButton.setOnClickListener {
            Toast.makeText(requireContext(), "알림 수신 설정 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 알림음 설정 버튼 클릭 리스너 설정
        val notificationsSoundButton = view.findViewById<TextView>(R.id.btn_notifications_sound)
        notificationsSoundButton.setOnClickListener {
            Toast.makeText(requireContext(), "알림음 설정 버튼 클릭됨", Toast.LENGTH_SHORT).show()
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

        // 키워드 등록 버튼 클릭 리스너 설정
        val registerKeywordButton = view.findViewById<TextView>(R.id.btn_register_keyword)
        registerKeywordButton.setOnClickListener {
            Toast.makeText(requireContext(), "키워드 등록 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 사용자 설정 버튼 클릭 리스너 설정
        val userSettingButton = view.findViewById<TextView>(R.id.btn_user_setting)
        userSettingButton.setOnClickListener {
            Toast.makeText(requireContext(), "사용자 설정 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 계정 정보 관리 버튼 클릭 리스너 설정
        val userInformationButton = view.findViewById<TextView>(R.id.btn_user_information)
        userInformationButton.setOnClickListener {
            Toast.makeText(requireContext(), "계정 정보 관리 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 언어 설정 버튼 클릭 리스너 설정
        val languageSettingButton = view.findViewById<TextView>(R.id.btn_language_setting)
        languageSettingButton.setOnClickListener {
            Toast.makeText(requireContext(), "언어 설정 버튼 클릭됨", Toast.LENGTH_SHORT).show()
        }

        // 캐시 데이터 삭제하기 버튼 클릭 리스너 설정
        val deleteDataButton = view.findViewById<TextView>(R.id.btn_delete_data)
        deleteDataButton.setOnClickListener {
            Toast.makeText(requireContext(), "캐시 데이터 삭제하기 버튼 클릭됨", Toast.LENGTH_SHORT).show()
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
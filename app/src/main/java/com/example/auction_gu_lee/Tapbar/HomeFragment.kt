package com.example.auction_gu_lee.Tapbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.home.CreateRoomActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Intent

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    // onCreateView 이후에 뷰가 생성된 후 호출되는 메서드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // fragment_home.xml의 ImageView를 가져와서 클릭 리스너 설정
        val magnifierImageView = view.findViewById<ImageView>(R.id.magnifier)
        magnifierImageView.setOnClickListener {
            Toast.makeText(requireContext(), "Magnifier clicked!", Toast.LENGTH_SHORT).show()
        }
        // FloatingActionButton 클릭 이벤트
        val floatingActionButton = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)
        floatingActionButton.setOnClickListener {
            // 새 방 만들기 화면으로 이동하는 Intent
            val intent = Intent(requireContext(), CreateRoomActivity::class.java)
            startActivity(intent)
        }

    }
}
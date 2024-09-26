package com.example.auction_gu_lee.Tapbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.home.CreateRoomActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.Intent
import androidx.activity.OnBackPressedCallback  // 뒤로가기 비활성화를 위한 import 추가
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.auction_gu_lee.Home.SearchRoomActivity
import com.google.firebase.database.*
import com.example.auction_gu_lee.models.Auction



class HomeFragment : Fragment() {

    private lateinit var auctionAdapter: AuctionAdapter
    private lateinit var auctionList: MutableList<Auction>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 뒤로가기 버튼 비활성화
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 뒤로가기 버튼 동작을 무효화
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    // onCreateView 이후에 뷰가 생성된 후 호출되는 메서드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView 설정
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView_auctions)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // DividerItemDecoration 추가 (구분선 추가)
        val dividerItemDecoration = DividerItemDecoration(recyclerView.context, LinearLayoutManager.VERTICAL)
        recyclerView.addItemDecoration(dividerItemDecoration)

        // fragment_home.xml의 ImageView를 가져와서 클릭 리스너 설정
        val magnifierImageView = view.findViewById<ImageView>(R.id.magnifier)
        magnifierImageView.setOnClickListener {
            val intent = Intent(requireContext(), SearchRoomActivity::class.java)
            startActivity(intent)
        }

        // FloatingActionButton 클릭 이벤트
        val floatingActionButton = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)
        floatingActionButton.setOnClickListener {
            // 새 방 만들기 화면으로 이동하는 Intent
            val intent = Intent(requireContext(), CreateRoomActivity::class.java)
            startActivity(intent)
        }

        auctionList = mutableListOf()
        auctionAdapter = AuctionAdapter(auctionList)
        recyclerView.adapter = auctionAdapter

        // Firebase에서 최신 데이터 가져오기
        fetchLatestAuctions()
    }

    // Firebase Realtime Database에서 최신 경매 데이터를 가져오는 함수
    private fun fetchLatestAuctions() {
        val database = FirebaseDatabase.getInstance().reference
        val auctionRef = database.child("auctions")

        auctionRef.orderByChild("timestamp").limitToLast(10)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    auctionList.clear()
                    for (auctionSnapshot in snapshot.children) {
                        val auction = auctionSnapshot.getValue(Auction::class.java)
                        auction?.let { auctionList.add(it) }
                    }

                    // 리스트 역순 정렬 후 어댑터 갱신
                    auctionList.reverse()
                    auctionAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

    }
}


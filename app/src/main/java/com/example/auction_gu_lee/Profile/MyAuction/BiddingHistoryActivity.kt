package com.example.auction_gu_lee.Profile.MyAuction

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.databinding.ActivityBiddingHistoryBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BiddingHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBiddingHistoryBinding
    private val auctionList = mutableListOf<Auction>()
    private val filteredList = mutableListOf<Auction>()
    private lateinit var adapter: BiddingHistoryAdapter
    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBiddingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 상태에서 검색창과 닫기 버튼은 숨김
        binding.searchEditText.visibility = View.GONE
        binding.btnCloseSearch.visibility = View.GONE

        // 초기 경고 팝업 상태를 체크박스와 동기화
        val isRebidWarningEnabled = binding.checkboxRebidWarning.isChecked

        // Adapter 초기화 시 체크박스의 초기 상태를 전달
        adapter = BiddingHistoryAdapter(filteredList, isRebidWarningEnabled) { auctionId ->
            val intent = Intent(this, AuctionRoomActivity::class.java)
            intent.putExtra("auction_id", auctionId)
            startActivity(intent)
        }

        binding.biddingHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.biddingHistoryRecyclerView.adapter = adapter

        // 체크박스 상태 변경 시 Adapter에 상태 업데이트
        binding.checkboxRebidWarning.setOnCheckedChangeListener { _, isChecked ->
            adapter.setRebidWarningEnabled(isChecked)  // Adapter에 상태 변경 알림
        }

        loadBiddingHistory()

        // 검색 버튼 클릭 시 입찰 내역 제목 숨기고 검색창 표시
        binding.btnSearch.setOnClickListener {
            binding.biddingHistoryTitle.visibility = View.GONE
            binding.btnSearch.visibility = View.GONE
            binding.searchEditText.visibility = View.VISIBLE
            binding.btnCloseSearch.visibility = View.VISIBLE
            binding.searchEditText.requestFocus()
        }

        // 검색창 닫기 버튼 클릭 시 제목 표시 및 검색창 숨김
        binding.btnCloseSearch.setOnClickListener {
            binding.searchEditText.setText("")
            binding.biddingHistoryTitle.visibility = View.VISIBLE
            binding.btnSearch.visibility = View.VISIBLE
            binding.searchEditText.visibility = View.GONE
            binding.btnCloseSearch.visibility = View.GONE
            binding.noResultsText.visibility = View.GONE
            filteredList.clear()
            filteredList.addAll(auctionList)  // 전체 목록 다시 표시
            adapter.notifyDataSetChanged()
        }

        // 검색어 입력 시마다 필터링 수행
        binding.searchEditText.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isNotEmpty()) {
                filterBiddingHistory(query)
            } else {
                filteredList.clear()
                filteredList.addAll(auctionList)
                adapter.notifyDataSetChanged()
                binding.noResultsText.visibility = View.GONE
            }
        }
    }

    private fun loadBiddingHistory() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        databaseReference.child("auctions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                if (snapshot.exists()) {
                    for (auctionSnapshot in snapshot.children) {
                        val auction = auctionSnapshot.getValue(Auction::class.java)
                        val auctionId = auctionSnapshot.key

                        if (auction != null && auctionId != null) {
                            auction.id = auctionId
                            val participantsSnapshot = auctionSnapshot.child("participants")
                            if (participantsSnapshot.hasChild(userId)) {
                                auctionList.add(auction)
                            }
                        }
                    }

                    // 타임스탬프 기준으로 내림차순 정렬
                    auctionList.sortByDescending { it.timestamp }

                    filteredList.clear()
                    filteredList.addAll(auctionList)  // 전체 데이터를 filteredList에 저장
                    filterBiddingHistory(binding.searchEditText.text.toString()) // 검색된 상태 유지
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BiddingHistoryActivity, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterBiddingHistory(query: String) {
        filteredList.clear()
        val lowerCaseQuery = query.lowercase()

        for (auction in auctionList) {
            if (auction.item?.lowercase()?.contains(lowerCaseQuery) == true) {
                filteredList.add(auction)
            }
        }

        if (filteredList.isEmpty()) {
            binding.noResultsText.visibility = View.VISIBLE
        } else {
            binding.noResultsText.visibility = View.GONE
        }

        adapter.notifyDataSetChanged()
    }
}
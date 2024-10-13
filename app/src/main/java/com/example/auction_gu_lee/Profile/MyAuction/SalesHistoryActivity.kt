package com.example.auction_gu_lee.Profile.MyAuction

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.databinding.ActivitySalesHistoryBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SalesHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesHistoryBinding
    private val auctionList = mutableListOf<Auction>()
    private val filteredList = mutableListOf<Auction>()
    private lateinit var adapter: SalesHistoryAdapter
    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기 상태에서 검색창과 닫기 버튼은 숨김
        binding.searchEditText.visibility = View.GONE
        binding.btnCloseSearch.visibility = View.GONE

        adapter = SalesHistoryAdapter(filteredList) { auctionId ->
            val intent = Intent(this, AuctionRoomActivity::class.java)
            intent.putExtra("auction_id", auctionId)
            startActivity(intent)
        }

        binding.salesHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.salesHistoryRecyclerView.adapter = adapter

        // 검색 버튼 클릭 시 제목과 검색 버튼 숨기고, 검색창과 닫기 버튼 표시
        binding.btnSearch.setOnClickListener {
            binding.salesHistoryTitle.visibility = View.GONE
            binding.btnSearch.visibility = View.GONE
            binding.searchEditText.visibility = View.VISIBLE
            binding.btnCloseSearch.visibility = View.VISIBLE
            binding.searchEditText.requestFocus()
        }

        // 닫기 버튼 클릭 시 제목과 검색 버튼 다시 표시, 검색창과 닫기 버튼 숨김
        binding.btnCloseSearch.setOnClickListener {
            binding.searchEditText.setText("")
            binding.salesHistoryTitle.visibility = View.VISIBLE
            binding.btnSearch.visibility = View.VISIBLE
            binding.searchEditText.visibility = View.GONE
            binding.btnCloseSearch.visibility = View.GONE
            binding.noResultsText.visibility = View.GONE
            filteredList.clear()
            filteredList.addAll(auctionList) // 전체 목록 다시 표시
            adapter.notifyDataSetChanged()
        }

        loadSalesHistory()

        // 검색어 입력 시마다 필터링 수행
        binding.searchEditText.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.isNotEmpty()) {
                filterSalesHistory(query)
            } else {
                filteredList.clear()
                filteredList.addAll(auctionList) // 검색어가 없을 경우 전체 목록 다시 표시
                adapter.notifyDataSetChanged()
                binding.noResultsText.visibility = View.GONE
            }
        }
    }

    private fun loadSalesHistory() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        // 모든 경매 항목을 가져와서 필터링
        databaseReference.child("auctions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                if (snapshot.exists()) {
                    for (auctionSnapshot in snapshot.children) {
                        val auction = auctionSnapshot.getValue(Auction::class.java)
                        auction?.let {
                            if (it.creatorUid == userId) { // 현재 사용자가 판매한 항목만 추가
                                it.id = auctionSnapshot.key
                                auctionList.add(it)
                            }
                        }
                    }
                    // 데이터가 모두 추가된 후 정렬 수행
                    auctionList.sortByDescending { it.timestamp }

                    filteredList.clear()
                    filteredList.addAll(auctionList)  // 초기 목록을 filteredList에 설정
                    filterSalesHistory(binding.searchEditText.text.toString()) // 검색된 상태 유지
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SalesHistoryActivity, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterSalesHistory(query: String) {
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
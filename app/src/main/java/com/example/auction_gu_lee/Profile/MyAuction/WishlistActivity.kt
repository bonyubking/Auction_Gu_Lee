package com.example.auction_gu_lee.Profile.MyAuction

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.Tapbar.AuctionAdapter
import com.example.auction_gu_lee.databinding.ActivityWishlistBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class WishlistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWishlistBinding
    private val auctionList = mutableListOf<Auction>()
    private lateinit var originalAuctionList: MutableList<Auction>  // 검색 전 원본 리스트
    private lateinit var adapter: AuctionAdapter
    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWishlistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        originalAuctionList = mutableListOf()

        // AuctionAdapter에서 Auction 객체 전체를 전달받음
        adapter = AuctionAdapter(auctionList) { auction ->
            // auction 객체에서 auctionId를 추출
            val auctionId = auction.id
            if (auctionId != null) {
                val intent = Intent(this, AuctionRoomActivity::class.java)
                intent.putExtra("auction_id", auctionId)  // auctionId 전달
                startActivity(intent)
            } else {
                Toast.makeText(this, "경매 ID를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.wishlistRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.wishlistRecyclerView.adapter = adapter

        // 관심 목록 로드
        loadWishlist()

        // 검색 버튼 클릭 리스너 설정
        binding.btnSearch.setOnClickListener {
            showSearchLayout()
        }

        // 검색창 닫기 버튼 클릭 리스너 설정
        binding.btnCloseSearch.setOnClickListener {
            hideSearchLayout()
        }

        // 검색 입력창에 입력 변화 감지
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                filterWishlist(query)  // 실시간으로 검색어에 따라 필터링
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // 검색창을 보여주는 함수
    private fun showSearchLayout() {
        binding.headerLayout.visibility = View.GONE  // 관심 목록 제목 숨김
        binding.searchLayout.visibility = View.VISIBLE  // 검색창 표시
    }

    // 검색창을 숨기고 원본 리스트로 복원
    private fun hideSearchLayout() {
        binding.headerLayout.visibility = View.VISIBLE  // 관심 목록 제목 표시
        binding.searchLayout.visibility = View.GONE  // 검색창 숨김
        binding.searchEditText.text.clear()  // 검색어 초기화
        // 원본 리스트로 복원
        auctionList.clear()
        auctionList.addAll(originalAuctionList)
        adapter.notifyDataSetChanged()
    }

    // Firebase에서 관심 목록 불러오기
    private fun loadWishlist() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        val wishlistReference = databaseReference.child("users").child(userId).child("wishlist")
        wishlistReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                originalAuctionList.clear()
                if (snapshot.exists()) {
                    val auctionIds = snapshot.children.mapNotNull { it.key }
                    loadAuctions(auctionIds)
                } else {
                    adapter.notifyDataSetChanged()  // 관심 목록이 비었을 때 UI 갱신
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // 오류 처리
            }
        })
    }

    // 관심 목록에 있는 경매 ID들로부터 경매 데이터를 로드
    private fun loadAuctions(auctionIds: List<String>) {
        auctionList.clear()
        originalAuctionList.clear()
        for (auctionId in auctionIds) {
            databaseReference.child("auctions").child(auctionId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val auction = snapshot.getValue(Auction::class.java)
                        auction?.let {
                            it.id = auctionId
                            auctionList.add(it)
                            originalAuctionList.add(it)  // 원본 리스트에도 추가
                            // 최신순으로 정렬
                            auctionList.sortByDescending { it.timestamp }
                            adapter.notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // 오류 처리
                    }
                })
        }
    }

    // 검색어에 따라 관심 목록 필터링
    private fun filterWishlist(query: String) {
        if (query.isBlank()) {
            // 검색어가 없을 경우 전체 리스트를 표시
            auctionList.clear()
            auctionList.addAll(originalAuctionList)  // 원본 리스트로 복원
            adapter.notifyDataSetChanged()
            binding.noResultsText.visibility = View.GONE
            return
        }

        // 검색어가 포함된 항목 필터링
        val filteredList = originalAuctionList.filter {
            it.item?.contains(query, ignoreCase = true) ?: false
        }

        auctionList.clear()
        auctionList.addAll(filteredList)
        adapter.notifyDataSetChanged()

        // 검색 결과가 없을 때 메시지 표시
        if (filteredList.isEmpty()) {
            binding.noResultsText.visibility = View.VISIBLE
        } else {
            binding.noResultsText.visibility = View.GONE
        }
    }
}

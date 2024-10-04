package com.example.auction_gu_lee.Home

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.Tapbar.AuctionAdapter
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.content.Intent

class SearchRoomActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var auctionAdapter: AuctionAdapter
    private lateinit var auctionList: MutableList<Auction>
    private lateinit var originalAuctionList: MutableList<Auction> // 원본 데이터 리스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_room)

        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        recyclerView = findViewById(R.id.recyclerView)

        // 처음에는 RecyclerView를 숨김 (검색 전)
        recyclerView.visibility = View.GONE

        auctionList = mutableListOf()
        originalAuctionList = mutableListOf()  // 원본 데이터 리스트 초기화
        auctionAdapter = AuctionAdapter(auctionList) { auction ->
            // 경매 항목 클릭 시 AuctionRoomActivity로 이동
            val intent = Intent(this, AuctionRoomActivity::class.java).apply {
                putExtra("username", auction.username)
                putExtra("item_name", auction.item)
                putExtra("item_detail", auction.detail)
                putExtra("starting_price", auction.startingPrice)
                val remainingTime = auction.endTime?.minus(System.currentTimeMillis()) ?: 0L
                putExtra("remaining_time", remainingTime)
            }
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = auctionAdapter

        fetchAuctionData()  // Firebase 데이터 가져오기

        searchButton.setOnClickListener {
            val query = searchEditText.text.toString()
            filterAuctions(query)
        }
    }

    private fun fetchAuctionData() {
        val database = FirebaseDatabase.getInstance().reference
        val auctionRef = database.child("auctions")

        auctionRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                originalAuctionList.clear()  // 원본 리스트 초기화
                for (auctionSnapshot in snapshot.children) {
                    val auction = auctionSnapshot.getValue(Auction::class.java)
                    auction?.let { originalAuctionList.add(it) }  // originalAuctionList에 데이터를 추가
                }
                auctionList.clear()
                auctionList.addAll(originalAuctionList)  // 초기에는 전체 리스트를 보여줌
                auctionAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SearchRoomActivity, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterAuctions(query: String) {
        if (query.isBlank()) {
            recyclerView.visibility = View.GONE  // 검색어가 없으면 RecyclerView 숨김
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return  // 더 이상 실행하지 않음
        }

        val filteredList = originalAuctionList.filter {
            it.item?.contains(query, ignoreCase = true) ?: false
        }.reversed()  // 필터링된 리스트를 역순으로 정렬

        if (filteredList.isNotEmpty()) {
            recyclerView.visibility = View.VISIBLE  // 검색 결과가 있으면 RecyclerView 표시
        } else {
            recyclerView.visibility = View.GONE  // 검색 결과가 없으면 숨김
            Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
        }

        auctionAdapter.updateList(filteredList)
    }
}

package com.example.auction_gu_lee.Home

import android.os.Bundle
import android.view.View
import android.widget.*
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
import android.util.Log

class SearchRoomActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var auctionAdapter: AuctionAdapter
    private lateinit var auctionList: MutableList<Auction>
    private lateinit var auctionIdList: MutableList<String>
    private lateinit var sortSpinner: Spinner
    private var currentSortType: String = "등록시간"
    private var auctionCategory: String = "home"  // 기본 카테고리를 "home"으로 설정

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_room)

        // 인텐트에서 전달된 auctionCategory 값을 가져옴
        auctionCategory = intent.getStringExtra("auction_category") ?: "home"

        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        recyclerView = findViewById(R.id.recyclerView)
        sortSpinner = findViewById(R.id.SortSpinner)

        recyclerView.visibility = View.GONE

        auctionList = mutableListOf()
        auctionIdList = mutableListOf()
        auctionAdapter = AuctionAdapter(auctionList) { auction ->
            val position = auctionList.indexOf(auction)
            val auctionId = auctionIdList[position]

            val intent = Intent(this, AuctionRoomActivity::class.java).apply {
                putExtra("auction_id", auctionId)
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

        setupSortSpinner()
        fetchAuctionData()

        searchButton.setOnClickListener {
            val query = searchEditText.text.toString()
            filterAuctions(query)
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = resources.getStringArray(R.array.sort_options)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = adapter

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSortType = when (position) {
                    0 -> "time"
                    1 -> "participants"
                    2 -> "favorites"
                    3 -> "highestPriceDesc"
                    4 -> "highestPriceAsc"
                    5 -> "startingPriceDesc"
                    6 -> "startingPriceAsc"
                    7 -> "remainingTime"
                    else -> "time"
                }
                sortAuctionListBy(currentSortType)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun sortAuctionListBy(sortType: String) {
        // auctionList와 auctionIdList를 Pair로 묶음
        val auctionPairs = auctionList.zip(auctionIdList).toMutableList()

        // 정렬 기준에 따라 auctionPairs를 정렬
        when (sortType) {
            "time" -> auctionPairs.sortByDescending { it.first.timestamp ?: 0L }
            "participants" -> auctionPairs.sortWith(
                compareByDescending<Pair<Auction, String>> { it.first.biddersCount ?: 0 }
                    .thenByDescending { it.first.timestamp ?: 0L }
            )
            "favorites" -> auctionPairs.sortWith(
                compareByDescending<Pair<Auction, String>> { it.first.favoritesCount ?: 0 }
                    .thenByDescending { it.first.timestamp ?: 0L }
            )
            "highestPriceDesc" -> auctionPairs.sortByDescending { it.first.highestPrice ?: 0L }
            "highestPriceAsc" -> auctionPairs.sortBy { it.first.highestPrice ?: 0L }
            "startingPriceDesc" -> auctionPairs.sortByDescending { it.first.startingPrice ?: 0L }
            "startingPriceAsc" -> auctionPairs.sortBy { it.first.startingPrice ?: 0L }
            "remainingTime" -> auctionPairs.sortBy {
                it.first.endTime?.minus(System.currentTimeMillis()) ?: Long.MAX_VALUE
            }
        }

        // 정렬된 Pair를 다시 분리하여 auctionList와 auctionIdList에 적용
        val (sortedAuctions, sortedIds) = auctionPairs.unzip()
        auctionList.clear()
        auctionList.addAll(sortedAuctions)
        auctionIdList.clear()
        auctionIdList.addAll(sortedIds)

        // 어댑터에 데이터 변경 알림
        auctionAdapter.notifyDataSetChanged()

        if (auctionList.isNotEmpty()) {
            recyclerView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.GONE
            Toast.makeText(this, "정렬된 결과가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterAuctions(query: String) {
        if (query.isBlank()) {
            recyclerView.visibility = View.GONE
            Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 검색어로 필터링된 경매 리스트 생성
        val filteredList = auctionList.zip(auctionIdList).filter {
            it.first.item?.contains(query, ignoreCase = true) ?: false
        }.toMutableList()

        if (filteredList.isNotEmpty()) {
            val (filteredAuctions, filteredIds) = filteredList.unzip()
            auctionList.clear()
            auctionList.addAll(filteredAuctions)
            auctionIdList.clear()
            auctionIdList.addAll(filteredIds)

            recyclerView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.GONE
            Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
        }

        auctionAdapter.notifyDataSetChanged()
    }

    private fun fetchAuctionData() {
        val database = FirebaseDatabase.getInstance().reference
        val auctionRef = database.child("auctions")

        auctionRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                auctionIdList.clear()
                for (auctionSnapshot in snapshot.children) {
                    val auction = auctionSnapshot.getValue(Auction::class.java)
                    auction?.let {
                        // 전달된 카테고리와 일치하는 항목만 추가
                        if (it.category == auctionCategory) {
                            it.biddersCount = it.biddersCount ?: 0
                            it.favoritesCount = it.favoritesCount ?: 0
                            it.startingPrice = it.startingPrice ?: 0L
                            it.highestPrice = it.highestPrice ?: 0L

                            auctionList.add(it)
                            auctionIdList.add(auctionSnapshot.key ?: "")
                        }
                    }
                }

                sortAuctionListBy(currentSortType)
                auctionAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SearchRoomActivity, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}


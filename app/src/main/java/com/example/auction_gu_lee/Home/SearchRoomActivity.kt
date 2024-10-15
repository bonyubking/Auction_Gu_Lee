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
    private lateinit var sortIcon: ImageView
    private var currentSortType: String = "remainingTime"
    private var auctionCategory: String = "home"  // 기본 카테고리 설정

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_room)

        auctionCategory = intent.getStringExtra("auction_category") ?: "home"

        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        recyclerView = findViewById(R.id.recyclerView)
        sortIcon = findViewById(R.id.sortIcon)

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

        // 정렬 아이콘 클릭 시 팝업창 띄우기
        sortIcon.setOnClickListener {
            showSortDialog()
        }

        // 힌트를 포커스 여부에 따라 표시하거나 숨기기
        searchEditText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                searchEditText.hint = ""  // 포커스가 있을 때 힌트를 지움
            } else {
                searchEditText.hint = "검색어를 입력하세요"  // 포커스가 없을 때 다시 힌트를 설정
            }
        }

        fetchAuctionData()

        searchButton.setOnClickListener {
            val query = searchEditText.text.toString()
            filterAuctions(query)
        }
    }

    // 정렬 기준 선택 팝업창을 띄우는 함수
    private fun showSortDialog() {
        val sortOptions = arrayOf("남은 시간 순", "입찰자 수 순", "관심 높은 순", "입찰가 높은 순", "입찰가 낮은 순", "시작가 높은 순", "시작가 낮은 순", "등록 시간 순")

        // 현재 선택된 정렬 타입에 해당하는 인덱스를 찾음
        val selectedIndex = when (currentSortType) {
            "remainingTime" -> 0
            "participants" -> 1
            "favorites" -> 2
            "highestPriceDesc" -> 3
            "highestPriceAsc" -> 4
            "startingPriceDesc" -> 5
            "startingPriceAsc" -> 6
            "time" -> 7
            else -> 0
        }

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("정렬 기준 선택")
        builder.setSingleChoiceItems(sortOptions, selectedIndex) { dialog, which ->
            currentSortType = when (which) {
                0 -> "remainingTime"
                1 -> "participants"
                2 -> "favorites"
                3 -> "highestPriceDesc"
                4 -> "highestPriceAsc"
                5 -> "startingPriceDesc"
                6 -> "startingPriceAsc"
                7 -> "time"
                else -> "remainingTime"
            }
            // 항목을 선택하면 즉시 정렬을 적용하고 다이얼로그 닫음
            sortAuctionListBy(currentSortType)
            dialog.dismiss()
        }

        builder.show()
    }

    private fun sortAuctionListBy(sortType: String) {
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

        val (sortedAuctions, sortedIds) = auctionPairs.unzip()
        auctionList.clear()
        auctionList.addAll(sortedAuctions)
        auctionIdList.clear()
        auctionIdList.addAll(sortedIds)

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


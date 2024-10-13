package com.example.auction_gu_lee.Tapbar

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import android.content.Intent
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.Home.CreateRoomActivity
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.Home.SearchRoomActivity
import com.example.auction_gu_lee.models.Auction
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private lateinit var auctionAdapter: AuctionAdapter
    private lateinit var auctionList: MutableList<Auction>
    private lateinit var auctionIdList: MutableList<String>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var sortIcon: ImageView
    private var auctionId: String? = null

    private var currentSortType: String = "remainingTime"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            auctionId = it.getString("auction_id")
        }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SwipeRefreshLayout 설정
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchLatestAuctions()
        }

        // RecyclerView 설정
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView_auctions)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 정렬 아이콘 설정
        setupSortIcon(view)

        // 검색 아이콘 클릭 이벤트 추가
        val magnifierImageView = view.findViewById<ImageView>(R.id.magnifier)
        magnifierImageView.setOnClickListener {
            val intent = Intent(requireContext(), SearchRoomActivity::class.java)
            startActivity(intent)
        }

        // FloatingActionButton 클릭 이벤트
        val floatingActionButton = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)
        floatingActionButton.setOnClickListener {
            val intent = Intent(requireContext(), CreateRoomActivity::class.java)
            startActivity(intent)
        }

        // 경매 목록 및 어댑터 설정
        auctionList = mutableListOf()
        auctionIdList = mutableListOf()
        auctionAdapter = AuctionAdapter(auctionList, { auction ->
            val position = auctionList.indexOf(auction)
            val auctionId = auctionIdList[position]

            // Recently Viewed에 경매 ID 추가
            addToRecentlyViewed(auctionId)

            val intent = Intent(requireContext(), AuctionRoomActivity::class.java).apply {
                putExtra("auction_id", auctionId)
                putExtra("item_name", auction.item)
                putExtra("item_detail", auction.detail)
                putExtra("starting_price", auction.startingPrice)
                putExtra("photo_url", auction.photoUrl)
                val remainingTime = auction.endTime?.minus(System.currentTimeMillis()) ?: 0L
                putExtra("remaining_time", remainingTime)
            }
            startActivity(intent)
        })
        recyclerView.adapter = auctionAdapter

        // 최신 데이터를 자동으로 로드하지 않음
        // 사용자가 다른 화면에서 돌아오거나, 수동으로 새로고침을 할 때만 데이터 로드
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때 최신 데이터를 가져옴
        fetchLatestAuctions()
    }

    // 정렬 아이콘 설정 함수
    private fun setupSortIcon(view: View) {
        sortIcon = view.findViewById(R.id.sortIcon)

        // 클릭 시 다이얼로그 띄우기
        sortIcon.setOnClickListener {
            showSortDialog()
        }
    }

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

        val builder = AlertDialog.Builder(requireContext())
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
    }


    private fun fetchLatestAuctions() {
        val database = FirebaseDatabase.getInstance().reference
        val auctionRef = database.child("auctions")

        auctionRef.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                auctionIdList.clear()

                val currentTime = System.currentTimeMillis()  // 현재 시간

                for (auctionSnapshot in snapshot.children) {
                    val auction = auctionSnapshot.getValue(Auction::class.java)
                    auction?.let {
                        // 경매가 종료되지 않은 항목만 리스트에 추가
                        if (auction.endTime != null && auction.endTime!! > currentTime) {
                            auctionList.add(auction)
                            auctionIdList.add(auctionSnapshot.key ?: "")
                        }
                    }
                }

                sortAuctionListBy(currentSortType)
                auctionAdapter.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun addToRecentlyViewed(auctionId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        val currentTime = System.currentTimeMillis()  // 현재 시각의 타임스탬프

        val databaseReference = FirebaseDatabase.getInstance().reference
        val recentlyViewedRef = databaseReference.child("users").child(userId).child("recentlyviewed").child(auctionId)

        // auctionId와 함께 현재 타임스탬프를 저장
        val data = mapOf("timestamp" to currentTime)
        recentlyViewedRef.setValue(data)  // 중복 클릭 시 타임스탬프 업데이트
    }
}
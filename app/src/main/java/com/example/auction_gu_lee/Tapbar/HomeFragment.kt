package com.example.auction_gu_lee.Tapbar

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
    private lateinit var sortSpinner: Spinner
    private var auctionId: String? = null

    private var currentSortType: String = "time"

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

        auctionId?.let {
            // 필요한 경우 auctionId로 특정 경매 항목을 가져와서 UI 업데이트
            Toast.makeText(requireContext(), "새로운 경매 생성됨!", Toast.LENGTH_SHORT).show()
        }

        // SwipeRefreshLayout 설정
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchLatestAuctions()
        }

        // RecyclerView 설정
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView_auctions)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // fragment_home.xml의 ImageView를 가져와서 클릭 리스너 설정
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

        // 정렬 Spinner 설정
        setupSortSpinner(view)

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

    // 정렬 Spinner 설정 함수
    private fun setupSortSpinner(view: View) {
        sortSpinner = view.findViewById(R.id.sortSpinner)

        // Spinner 항목을 두 개로 나눔: 등록시간, 입찰자, 관심, 입찰가(높은 순), 입찰가(낮은 순), 시작가(높은 순), 시작가(낮은 순), 남은시간
        val sortOptions = arrayOf("등록 시간 순", "입찰자 수 순", "관심 높은 순", "입찰가 높은 순", "입찰가 낮은 순", "시작가 높은 순", "시작가 낮은 순", "남은 시간")

        // Adapter에 커스텀 레이아웃 적용 (필요시 적용)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = adapter

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentSortType = when (position) {
                    0 -> "time"
                    1 -> "participants"
                    2 -> "favorites"
                    3 -> "highestPriceDesc"  // 입찰가(높은 순)
                    4 -> "highestPriceAsc"   // 입찰가(낮은 순)
                    5 -> "startingPriceDesc" // 시작가(높은 순)
                    6 -> "startingPriceAsc"  // 시작가(낮은 순)
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
    }


    private fun fetchLatestAuctions() {
        val database = FirebaseDatabase.getInstance().reference
        val auctionRef = database.child("auctions")

        auctionRef.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                auctionIdList.clear()

                val updateTasks = mutableListOf<DatabaseReference>() // 카테고리 업데이트 목록

                for (auctionSnapshot in snapshot.children) {
                    val auction = auctionSnapshot.getValue(Auction::class.java)

                    // 경매가 null이면 무시 (continue 대신 if 문 사용)
                    if (auction == null) {
                        continue  // 여기서 발생하는 오류는 `continue` 키워드를 잘못된 위치에서 사용한 것입니다.
                    }

                    val currentTime = System.currentTimeMillis()
                    val auctionId = auctionSnapshot.key ?: ""

                    if (auctionId.isEmpty()) {
                        continue
                    }

                    // 경매 종료 여부 체크 및 카테고리 업데이트
                    if (auction.endTime != null && auction.endTime!! <= currentTime) {
                        // 만약 종료되었으면 카테고리를 "past"로 업데이트
                        if (auction.category != "past") {
                            auctionRef.child(auctionId).child("category").setValue("past")
                            updateTasks.add(auctionRef.child(auctionId)) // 업데이트 목록에 추가
                        }
                    } else if (auction.endTime != null && auction.endTime!! > currentTime && auction.category == "home") {
                        // 종료되지 않은 경매만 목록에 추가
                        auctionList.add(auction)
                        auctionIdList.add(auctionId)
                    }
                }

                // 모든 업데이트가 끝난 후, 목록을 다시 정렬하고 업데이트
                if (updateTasks.isEmpty()) {
                    sortAndRefreshUI()
                } else {
                    for (task in updateTasks) {
                        task.child("category").setValue("past").addOnCompleteListener {
                            if (it.isSuccessful) {
                                sortAndRefreshUI()
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun sortAndRefreshUI() {
        sortAuctionListBy(currentSortType)
        auctionAdapter.notifyDataSetChanged()
        swipeRefreshLayout.isRefreshing = false
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
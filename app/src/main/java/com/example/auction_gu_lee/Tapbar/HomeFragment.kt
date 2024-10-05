package com.example.auction_gu_lee.Tapbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import android.content.Intent
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

class HomeFragment : Fragment() {

    private lateinit var auctionAdapter: AuctionAdapter
    private lateinit var auctionList: MutableList<Auction>
    private lateinit var auctionIdList: MutableList<String>
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

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
            val intent = Intent(requireContext(), CreateRoomActivity::class.java)
            startActivity(intent)
        }

        // 경매 목록 및 어댑터 설정
        auctionList = mutableListOf()
        auctionIdList = mutableListOf()
        auctionAdapter = AuctionAdapter(auctionList) { auction ->
            // 경매 항목 클릭 시 AuctionRoomActivity로 이동
            val position = auctionList.indexOf(auction)
            val auctionId = auctionIdList[position]

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
        }
        recyclerView.adapter = auctionAdapter

        // 최신 데이터를 자동으로 로드하지 않음
        // 사용자가 다른 화면에서 돌아오거나, 수동으로 새로고침을 할 때만 데이터 로드
    }

    override fun onResume() {
        super.onResume()
        // 화면이 다시 보일 때 최신 데이터를 가져옴
        fetchLatestAuctions()
    }

    private fun fetchLatestAuctions() {
        val database = FirebaseDatabase.getInstance().reference
        val auctionRef = database.child("auctions")

        auctionRef.orderByChild("timestamp").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                auctionIdList.clear()
                for (auctionSnapshot in snapshot.children) {
                    val auction = auctionSnapshot.getValue(Auction::class.java)
                    auction?.let {
                        if (auction.endTime is Long) {
                            auctionList.add(it)
                            auctionIdList.add(auctionSnapshot.key ?: "")
                        }
                    }
                }
                // 역순으로 정렬 (가장 최근 경매가 맨 위로 오게)
                auctionList.reverse()
                auctionIdList.reverse()

                auctionAdapter.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false  // 새로고침 UI 종료
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false  // 새로고침 UI 종료
            }
        })
    }
}

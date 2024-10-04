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

class HomeFragment : Fragment() {

    private lateinit var auctionAdapter: AuctionAdapter
    private lateinit var auctionList: MutableList<Auction>
    private lateinit var auctionIdList: MutableList<String>

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
                putExtra("auction_id", auctionId)  // Firebase 데이터베이스에서 가져온 auction의 id 전달
                putExtra("item_name", auction.item)
                putExtra("item_detail", auction.detail)
                putExtra("starting_price", auction.startingPrice)
                putExtra("photo_url", auction.photoUrl)  // photoUrl 전달
                val remainingTime = auction.endTime?.minus(System.currentTimeMillis()) ?: 0L
                putExtra("remaining_time", remainingTime)
            }
            startActivity(intent)
        }
        recyclerView.adapter = auctionAdapter

        // Firebase에서 최신 데이터 가져오기
        fetchLatestAuctions()
    }

    private fun fetchLatestAuctions() {
        val database = FirebaseDatabase.getInstance().reference
        val auctionRef = database.child("auctions")

        auctionRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
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
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

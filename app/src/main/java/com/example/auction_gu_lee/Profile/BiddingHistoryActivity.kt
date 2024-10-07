package com.example.auction_gu_lee.Profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ActivityBiddingHistoryBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BiddingHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBiddingHistoryBinding
    private val auctionList = mutableListOf<Auction>()
    private lateinit var adapter: BiddingHistoryAdapter
    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBiddingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = BiddingHistoryAdapter(auctionList) { auctionId ->
            // AuctionRoomActivity로 이동
            val intent = Intent(this, AuctionRoomActivity::class.java)
            intent.putExtra("auction_id", auctionId)
            startActivity(intent)
        }

        binding.biddingHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.biddingHistoryRecyclerView.adapter = adapter

        loadBiddingHistory()
    }

    private fun loadBiddingHistory() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        // 모든 경매 데이터를 가져옴
        databaseReference.child("auctions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                if (snapshot.exists()) {
                    for (auctionSnapshot in snapshot.children) {
                        val auction = auctionSnapshot.getValue(Auction::class.java)
                        val auctionId = auctionSnapshot.key

                        // auction이 null이 아니고 auctionId가 null이 아닐 때만 처리
                        if (auction != null && auctionId != null) {
                            auction.id = auctionId

                            // 해당 경매의 participants에 사용자의 UID가 있는지 확인
                            val participantsSnapshot = auctionSnapshot.child("participants")
                            if (participantsSnapshot.hasChild(userId)) {
                                auctionList.add(auction)
                            }
                        }
                    }
                    // 입찰 내역 리스트를 최신 순으로 정렬하고 UI 갱신
                    auctionList.sortByDescending { it.timestamp }
                    adapter.notifyDataSetChanged()
                } else {
                    adapter.notifyDataSetChanged()  // 입찰 내역이 비었을 때 UI 갱신
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@BiddingHistoryActivity, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("FirebaseError", "데이터 읽기 실패: ${error.message}")
            }
        })
    }
}

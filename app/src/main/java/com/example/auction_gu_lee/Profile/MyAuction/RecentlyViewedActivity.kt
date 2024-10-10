package com.example.auction_gu_lee.Profile.MyAuction

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.databinding.ActivityRecentlyViewedBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RecentlyViewedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecentlyViewedBinding
    private val auctionList = mutableListOf<Auction>()
    private val databaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var adapter: RecentlyViewedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentlyViewedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RecentlyViewedAdapter(auctionList) { auctionId ->
            val intent = Intent(this, AuctionRoomActivity::class.java)
            intent.putExtra("auction_id", auctionId)
            startActivity(intent)
        }

        binding.recentlyViewedRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.recentlyViewedRecyclerView.adapter = adapter

        loadRecentlyViewed()
    }

    private fun loadRecentlyViewed() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        val recentlyViewedRef = databaseReference.child("users").child(userId).child("recentlyviewed")
        recentlyViewedRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                if (snapshot.exists()) {
                    val auctionInfoList = mutableListOf<Pair<String, Long>>()
                    for (auctionSnapshot in snapshot.children) {
                        val auctionId = auctionSnapshot.key
                        val timestamp = auctionSnapshot.child("timestamp").getValue(Long::class.java)
                        if (auctionId != null && timestamp != null) {
                            auctionInfoList.add(Pair(auctionId, timestamp))
                        }
                    }

                    // 타임스탬프를 기준으로 정렬
                    auctionInfoList.sortByDescending { it.second }

                    // 정렬된 ID로 경매 정보 로드
                    loadAuctionsDetails(auctionInfoList.map { it.first })
                } else {
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@RecentlyViewedActivity, "최근 본 내역이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RecentlyViewedActivity, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAuctionsDetails(auctionIds: List<String>) {
        auctionList.clear()
        for (auctionId in auctionIds) {
            val auctionRef = databaseReference.child("auctions").child(auctionId)
            auctionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val auction = snapshot.getValue(Auction::class.java)
                    if (auction != null) {
                        auction.id = auctionId
                        auctionList.add(auction)
                        adapter.notifyDataSetChanged()  // 데이터 변경 후 어댑터 업데이트
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RecentlyViewedActivity", "경매 정보 로드 실패: ${error.message}")
                }
            })
        }
    }
}
package com.example.auction_gu_lee.Profile

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
                    val auctionIds = mutableListOf<String>()
                    for (auctionSnapshot in snapshot.children) {
                        val auctionId = auctionSnapshot.key
                        if (auctionId != null) {
                            auctionIds.add(auctionId)
                        }
                    }
                    // 가져온 auctionIds를 사용하여 해당 경매 정보를 불러옴
                    loadAuctionsDetails(auctionIds)
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
        var auctionsLoaded = 0

        for (auctionId in auctionIds) {
            val auctionRef = databaseReference.child("auctions").child(auctionId)
            auctionRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val auction = snapshot.getValue(Auction::class.java)
                    if (auction != null) {
                        auction.id = auctionId
                        auctionList.add(auction)
                    }

                    // 모든 경매 데이터를 불러왔을 때만 정렬
                    auctionsLoaded++
                    if (auctionsLoaded == auctionIds.size) {
                        // 최신순으로 정렬
                        auctionList.sortByDescending { it.timestamp ?: 0L }
                        adapter.notifyDataSetChanged()  // 어댑터에 데이터 변경 알림
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RecentlyViewedActivity", "경매 정보 로드 실패: ${error.message}")
                }
            })
        }
    }
}
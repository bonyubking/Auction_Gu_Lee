package com.example.auction_gu_lee.Profile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.databinding.ActivitySalesHistoryBinding
import com.example.auction_gu_lee.models.Auction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SalesHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesHistoryBinding
    private val auctionList = mutableListOf<Auction>()
    private lateinit var adapter: SalesHistoryAdapter
    private val databaseReference = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SalesHistoryAdapter(auctionList) { auctionId ->
            val intent = Intent(this, AuctionRoomActivity::class.java)
            intent.putExtra("auction_id", auctionId)
            startActivity(intent)
        }

        binding.salesHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.salesHistoryRecyclerView.adapter = adapter

        loadSalesHistory()
    }

    private fun loadSalesHistory() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        // auctions 경로에서 모든 경매 항목을 가져옴
        val auctionsReference = databaseReference.child("auctions")
        auctionsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()
                for (auctionSnapshot in snapshot.children) {
                    val auction = auctionSnapshot.getValue(Auction::class.java)
                    auction?.let {
                        // creatorUid가 현재 사용자와 일치하는지 확인
                        if (it.creatorUid == userId) {
                            it.id = auctionSnapshot.key // auctionId 설정
                            auctionList.add(it)
                        }
                    }
                }
                // 최신 순으로 정렬
                auctionList.sortByDescending { auction -> auction.timestamp }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // 오류 처리
            }
        })
    }
}

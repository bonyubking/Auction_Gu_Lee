package com.example.auction_gu_lee.Profile.MyAuction

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.models.Post
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.Board.PostAdapter
import com.example.auction_gu_lee.Board.PostDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PurchaseWishlistActivity : AppCompatActivity() {

    private lateinit var postAdapter: PostAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var postList: MutableList<Post>
    private lateinit var originalPostList: MutableList<Post> // 검색을 위한 원본 리스트
    private lateinit var databaseReference: DatabaseReference
    private lateinit var searchEditText: EditText
    private lateinit var btnSearch: ImageButton
    private lateinit var btnCloseSearch: ImageButton
    private lateinit var noResultsText: TextView
    private lateinit var purchasePostTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_wishlist)

        // View 초기화
        searchEditText = findViewById(R.id.search_edit_text)
        btnSearch = findViewById(R.id.btn_search)
        btnCloseSearch = findViewById(R.id.btn_close_search)
        noResultsText = findViewById(R.id.no_results_text)
        purchasePostTitle = findViewById(R.id.purchase_post_title)

        // RecyclerView 초기화
        recyclerView = findViewById(R.id.purchase_posts_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        postList = mutableListOf()
        originalPostList = mutableListOf()
        postAdapter = PostAdapter(postList) { post ->
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra("postId", post.postId)  // 선택한 게시글의 ID를 전달
            startActivity(intent)
        }
        recyclerView.adapter = postAdapter

        // Firebase에서 데이터 가져오기
        fetchPurchasePosts()

        // 검색 버튼 클릭 시
        btnSearch.setOnClickListener {
            purchasePostTitle.visibility = View.GONE // 제목 숨김
            searchEditText.visibility = View.VISIBLE
            btnCloseSearch.visibility = View.VISIBLE
            btnSearch.visibility = View.GONE
        }

        // 닫기 버튼 클릭 시
        btnCloseSearch.setOnClickListener {
            purchasePostTitle.visibility = View.VISIBLE // 제목 다시 표시
            searchEditText.visibility = View.GONE
            btnCloseSearch.visibility = View.GONE
            btnSearch.visibility = View.VISIBLE
            searchEditText.text.clear() // 검색어 초기화
            filterPosts("") // 검색 초기화 (전체 리스트 표시)
        }

        // 검색창에 텍스트 입력 시 필터링
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                filterPosts(query) // 검색어에 따른 필터링
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // 검색어에 따라 리스트 필터링
    private fun filterPosts(query: String) {
        if (query.isEmpty()) {
            postList.clear()
            postList.addAll(originalPostList) // 검색어가 없을 경우 원본 리스트 복원
            noResultsText.visibility = View.GONE // 검색 결과가 없을 때 메시지 숨김
        } else {
            val filteredList = originalPostList.filter {
                it.item?.contains(query, ignoreCase = true) == true ||
                        it.detail?.contains(query, ignoreCase = true) == true
            }

            postList.clear()
            postList.addAll(filteredList)

            // 검색 결과가 없으면 메시지 표시
            if (filteredList.isEmpty()) {
                noResultsText.visibility = View.VISIBLE
            } else {
                noResultsText.visibility = View.GONE
            }
        }
        postAdapter.notifyDataSetChanged() // 리스트 업데이트
    }

    private fun fetchPurchasePosts() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("purchase_posts")

            // 현재 로그인한 사용자의 userId와 일치하는 데이터만 필터링하고, timestamp로 내림차순 정렬
            databaseReference.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        postList.clear() // 기존 데이터를 지우고 새로 받아온 데이터로 업데이트
                        originalPostList.clear() // 원본 리스트도 초기화
                        val tempPostList = mutableListOf<Post>() // 임시 리스트

                        for (postSnapshot in snapshot.children) {
                            val post = postSnapshot.getValue(Post::class.java)
                            post?.let {
                                tempPostList.add(it) // 임시 리스트에 추가
                            }
                        }

                        // timestamp를 기준으로 내림차순 정렬
                        tempPostList.sortByDescending { it.timestamp }
                        postList.addAll(tempPostList) // 정렬된 데이터를 postList에 추가
                        originalPostList.addAll(tempPostList) // 원본 리스트에 추가
                        postAdapter.notifyDataSetChanged() // 어댑터에 데이터 변경 알림
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // 오류 처리
                    }
                })
        }
    }
}

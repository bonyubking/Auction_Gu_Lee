package com.example.auction_gu_lee.Board

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.Board.PostAdapter
import com.example.auction_gu_lee.Board.PostDetailActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Post
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SearchPostActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var postList: MutableList<Post>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_post)

        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        recyclerView = findViewById(R.id.recyclerView)

        postList = mutableListOf()
        postAdapter = PostAdapter(postList) { post ->
            val intent = Intent(this, PostDetailActivity::class.java)
            intent.putExtra("postId", post.postId)  // 선택한 게시글의 ID를 전달
            startActivity(intent)
            // 클릭 시 처리 로직 (필요 시 추가)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = postAdapter

        // 초기 전체 목록 불러오기
        loadAllPosts()

        // 힌트를 포커스 여부에 따라 표시하거나 숨기기
        searchEditText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                searchEditText.hint = ""  // 포커스가 있을 때 힌트를 지움
            } else {
                searchEditText.hint = "검색어를 입력하세요"  // 포커스가 없을 때 다시 힌트를 설정
            }
        }

        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                searchPosts(query)
            } else {
                Toast.makeText(this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAllPosts() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("purchase_posts")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        postList.add(it)
                    }
                }
                // 최신순으로 정렬 (timestamp 내림차순)
                postList.sortByDescending { it.timestamp }

                if (postList.isNotEmpty()) {
                    postAdapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@SearchPostActivity, "게시글이 없습니다.", Toast.LENGTH_SHORT).show()
                    recyclerView.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SearchPostActivity, "데이터를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchPosts(query: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("purchase_posts")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let {
                        // 검색어가 item 필드에 포함되어 있는지 확인 (대소문자 구분 없이)
                        if (it.item.contains(query, ignoreCase = true)) {
                            postList.add(it)
                        }
                    }
                }
                if (postList.isNotEmpty()) {
                    postAdapter.notifyDataSetChanged()
                    recyclerView.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this@SearchPostActivity, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    recyclerView.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SearchPostActivity, "데이터를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
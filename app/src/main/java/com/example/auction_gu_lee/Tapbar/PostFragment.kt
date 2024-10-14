package com.example.auction_gu_lee.Tapbar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.Board.AddPostActivity
import com.example.auction_gu_lee.Board.PostAdapter
import com.example.auction_gu_lee.Board.PostDetailActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Post
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

// PostFragment.kt
class PostFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private val database = FirebaseDatabase.getInstance().getReference("purchase_posts")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.postRecyclerView)
        postAdapter = PostAdapter(postList) { post ->
            // 구매 요청 글 상세보기로 이동
            val intent = Intent(activity, PostDetailActivity::class.java)
            intent.putExtra("postId", post.postId)
            startActivity(intent)
        }
        recyclerView.adapter = postAdapter
        recyclerView.layoutManager = LinearLayoutManager(context)

        // 글쓰기 버튼 초기화 및 클릭 리스너 설정
        val addPostButton: FloatingActionButton = view.findViewById(R.id.addPostButton)
        addPostButton.setOnClickListener {
            // 글쓰기 액티비티로 이동
            val intent = Intent(activity, AddPostActivity::class.java)
            startActivity(intent)
        }

        loadPosts()
    }

    private fun loadPosts() {
        database.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList.clear()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    if (post != null) {
                        postList.add(post)
                    }
                }
                // 최신순으로 정렬 (timestamp 내림차순)
                postList.sortByDescending { it.timestamp }
                postAdapter.notifyDataSetChanged()            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}


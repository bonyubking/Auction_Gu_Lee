package com.example.auction_gu_lee.Board

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auction_gu_lee.Home.AuctionRoomActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.Tapbar.AuctionAdapter
import com.example.auction_gu_lee.models.Auction
import com.example.auction_gu_lee.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.auction_gu_lee.models.Comment
import java.util.UUID

// PostDetailActivity.kt
// PostDetailActivity.kt
class PostDetailActivity : AppCompatActivity() {


    private lateinit var buttonDeletePost: ImageButton
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var textViewItem: TextView
    private lateinit var textViewDesiredPrice: TextView
    private lateinit var textViewQuantity: TextView
    private lateinit var textViewDetail: TextView
    private lateinit var buttonAddComment: Button
    private lateinit var recyclerViewComments: RecyclerView
    private lateinit var auctionAdapter: AuctionAdapter

    // 전역 변수로 선언
    private val auctionList = mutableListOf<Auction>()  // Auction 리스트 전역으로 변경
    private val database = FirebaseDatabase.getInstance().getReference()  // Firebase 경로 수정
    private lateinit var postId: String
    private lateinit var post: Post

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        buttonDeletePost = findViewById(R.id.button_delete_post)

        textViewItem = findViewById(R.id.textView_item)
        textViewDesiredPrice = findViewById(R.id.textView_desired_price)
        textViewQuantity = findViewById(R.id.textView_quantity)
        textViewDetail = findViewById(R.id.textView_detail)
        buttonAddComment = findViewById(R.id.button_add_comment)
        recyclerViewComments = findViewById(R.id.recyclerView_comments)

        postId = intent.getStringExtra("postId") ?: ""
        if (postId.isNotEmpty()) {
            loadPostDetails()
            setupComments()
            buttonAddComment.setOnClickListener {
                loadUserAuctions()
            }
            buttonDeletePost.setOnClickListener {
                showDeleteConfirmationDialog()
            }
        } else {
            Toast.makeText(this, "게시글 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadPostDetails() {
        database.child("purchase_posts").child(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    post = snapshot.getValue(Post::class.java) ?: return
                    textViewItem.text = post.item
                    textViewDesiredPrice.text = "구매 희망 가격: ${String.format("%,d원", post.desiredPrice)}"
                    textViewQuantity.text = "수량: ${post.quantity}"
                    textViewDetail.text = post.detail

                    // 게시글이 현재 사용자에 의해 등록된 것인지 확인
                    if (post.userId == currentUserUid) {
                        buttonDeletePost.visibility = View.VISIBLE  // 삭제 버튼 노출
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PostDetailActivity, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showDeleteConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("게시글 삭제")
        builder.setMessage("정말로 이 게시글을 삭제하시겠습니까?")
        builder.setPositiveButton("삭제") { dialog, _ ->
            deletePost()
            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun deletePost() {
        database.child("purchase_posts").child(postId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "게시글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                finish()  // 게시글 삭제 후 액티비티 종료
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupComments() {
        auctionAdapter = AuctionAdapter(auctionList) { auction ->
            if (auction.id.isNullOrEmpty()) {
                Log.e("PostDetailActivity", "auctionId is empty.")
                Toast.makeText(this, "유효하지 않은 경매 ID입니다.", Toast.LENGTH_SHORT).show()
                return@AuctionAdapter
            }

            // 판매 내역 상세보기로 이동
            val intent = Intent(this, AuctionRoomActivity::class.java)
            intent.putExtra("auction_id", auction.id)

            Log.d("PostDetailActivity", "Auction ID 전달: ${auction.id}")
            startActivity(intent)
        }

        recyclerViewComments.adapter = auctionAdapter
        recyclerViewComments.layoutManager = LinearLayoutManager(this)
        loadAuctionDataForComments()
    }

    private fun loadAuctionDataForComments() {
        val commentsRef = database.child("purchase_posts").child(postId).child("comments")

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                auctionList.clear()  // 기존 리스트 초기화

                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    if (comment != null) {
                        val auctionId = comment.auctionId
                        if (!auctionId.isNullOrEmpty()) {
                            // auctionId로 경매 정보 로드
                            loadAuctionData(auctionId)
                        } else {
                            Log.e("PostDetailActivity", "auctionId is null or empty for commentId: ${comment.commentId}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, "댓글을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAuctionData(auctionId: String) {
        val auctionsRef = database.child("auctions").child(auctionId)

        auctionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val auction = snapshot.getValue(Auction::class.java)
                if (auction != null) {
                    auction.id = auctionId // auction.id에 Firebase 키 할당
                    auctionList.add(auction)  // 리스트에 추가
                    auctionAdapter.notifyDataSetChanged()  // 어댑터에 변경 사항 알림
                } else {
                    Log.e("PostDetailActivity", "Auction data is null for auctionId: $auctionId")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, "경매 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun loadUserAuctions() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val dbRef = database.child("auctions")

        // 판매 내역을 현재 사용자 ID로 필터링하여 불러옴
        dbRef.orderByChild("creatorUid").equalTo(currentUserUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    auctionList.clear()  // 기존 리스트 초기화
                    for (auctionSnapshot in snapshot.children) {
                        val auction = auctionSnapshot.getValue(Auction::class.java)
                        if (auction != null) {
                            val auctionId = auctionSnapshot.key
                            if (auctionId.isNullOrEmpty()) {
                                Log.e("PostDetailActivity", "auctionSnapshot.key가 비어 있습니다.")
                                continue
                            }

                            auction.id = auctionId
                            auctionList.add(auction)
                        }
                    }
                    if (auctionList.isNotEmpty()) {
                        showAuctionSelectionDialog(auctionList) // 판매 내역이 있을 때 다이얼로그 호출
                    } else {
                        Toast.makeText(this@PostDetailActivity, "판매 내역이 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PostDetailActivity, "판매 내역을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun showAuctionSelectionDialog(auctionList: List<Auction>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_auction_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView_auctions)

        val dialog = AlertDialog.Builder(this)
            .setTitle("판매 내역 선택")
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .create()

        val adapter = AuctionSelectionAdapter(auctionList) { selectedAuction ->
            submitComment(selectedAuction)
            dialog.dismiss()
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        dialog.show()
    }

    private fun submitComment(selectedAuction: Auction) {
        val auctionId = selectedAuction.id
        if (auctionId.isNullOrEmpty()) {
            Toast.makeText(this@PostDetailActivity, "경매 ID가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val commentsRef = database.child("purchase_posts").child(postId).child("comments")
        val query = commentsRef.orderByChild("auctionId").equalTo(auctionId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(this@PostDetailActivity, "이미 이 판매 내역을 등록하셨습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val commentId = commentsRef.push().key ?: UUID.randomUUID().toString()
                    val comment = Comment(
                        commentId = commentId,
                        postId = postId,
                        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous",
                        auctionId = auctionId,
                        timestamp = System.currentTimeMillis()
                    )

                    commentsRef.child(commentId).setValue(comment)
                        .addOnSuccessListener {
                            Toast.makeText(this@PostDetailActivity, "판매 내역이 댓글로 등록되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@PostDetailActivity, "댓글 등록 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, "댓글 등록 중 오류가 발생했습니다: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}


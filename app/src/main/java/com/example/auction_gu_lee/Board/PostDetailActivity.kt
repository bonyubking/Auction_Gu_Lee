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
import com.example.auction_gu_lee.models.Auction
import com.example.auction_gu_lee.models.Post
import com.example.auction_gu_lee.models.Comment
import com.example.auction_gu_lee.models.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.UUID

class PostDetailActivity : AppCompatActivity() {

    private lateinit var buttonDeletePost: ImageButton
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private lateinit var textViewItem: TextView
    private lateinit var textViewDesiredPrice: TextView
    private lateinit var textViewQuantity: TextView
    private lateinit var textViewDetail: TextView
    private lateinit var buttonAddComment: Button
    private lateinit var recyclerViewComments: RecyclerView
    private lateinit var commentAdapter: CommentAdapter  // AuctionAdapter 대신 CommentAdapter 사용

    // 전역 변수로 선언
    private val commentList = mutableListOf<Comment>()  // Comment 리스트 전역으로 변경
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
                        buttonAddComment.isEnabled = false  // "내 판매 내역 첨부하기" 버튼 비활성화
                        buttonAddComment.alpha = 0.5f  // 시각적으로 비활성화 상태 표시 (optional)
                    } else {
                        buttonAddComment.isEnabled = true  // "내 판매 내역 첨부하기" 버튼 활성화
                        buttonAddComment.alpha = 1.0f  // 원래 상태로 복원 (optional)
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
        commentAdapter = CommentAdapter(
            commentList,
            currentUserUid,
            { comment -> showDeleteCommentConfirmationDialog(comment) },
            { auction -> openAuctionDetail(auction) }
        )

        recyclerViewComments.adapter = commentAdapter
        recyclerViewComments.layoutManager = LinearLayoutManager(this)
        loadAuctionDataForComments()
    }

    private fun loadAuctionDataForComments() {
        val commentsRef = database.child("purchase_posts").child(postId).child("comments")

        commentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                commentList.clear()  // 기존 리스트 초기화

                for (commentSnapshot in snapshot.children) {
                    val comment = commentSnapshot.getValue(Comment::class.java)
                    if (comment != null) {
                        commentList.add(comment)
                    }
                }

                commentAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, "댓글을 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserAuctions() {
        val dbRef = database.child("auctions")

        // 판매 내역을 현재 사용자 ID로 필터링하여 불러옴
        dbRef.orderByChild("creatorUid").equalTo(currentUserUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userAuctions = mutableListOf<Auction>()
                    for (auctionSnapshot in snapshot.children) {
                        val auction = auctionSnapshot.getValue(Auction::class.java)
                        if (auction != null) {
                            val auctionId = auctionSnapshot.key
                            if (auctionId.isNullOrEmpty()) {
                                Log.e("PostDetailActivity", "auctionSnapshot.key가 비어 있습니다.")
                                continue
                            }

                            auction.id = auctionId
                            userAuctions.add(auction)
                        }
                    }
                    if (userAuctions.isNotEmpty()) {
                        showAuctionSelectionDialog(userAuctions) // 판매 내역이 있을 때 다이얼로그 호출
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

                            // 알림 추가
                            addCommentNotification(post.userId, postId, "구매 요청 게시글에 새로운 댓글이 달렸습니다.")
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


    private fun openAuctionDetail(auction: Auction) {
        val auctionId = auction.id
        if (!auctionId.isNullOrEmpty()) {
            val intent = Intent(this, AuctionRoomActivity::class.java)
            intent.putExtra("auction_id", auctionId)
            startActivity(intent)
        } else {
            Toast.makeText(this, "경매 ID가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 댓글 삭제 확인 다이얼로그
    private fun showDeleteCommentConfirmationDialog(comment: Comment) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("댓글 삭제")
        builder.setMessage("이 댓글을 삭제하시겠습니까?")
        builder.setPositiveButton("삭제") { dialog, _ ->
            deleteComment(comment)
            dialog.dismiss()
        }
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // 댓글 삭제 함수
    private fun deleteComment(comment: Comment) {
        val commentRef = database.child("purchase_posts").child(postId).child("comments").child(comment.commentId)
        commentRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "댓글 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addCommentNotification(postAuthorId: String, postId: String, message: String) {
        val notificationRef = database.child("users").child(postAuthorId).child("notifications").push()
        val notificationId = notificationRef.key ?: run {
            Log.e("PostDetailActivity", "알림 ID 생성 실패")
            return
        }
        val notification = Notification(
            id = notificationId,
            message = message,
            relatedPostId = postId, // 관련 게시글 ID 설정
            timestamp = System.currentTimeMillis(),
            type = "comment",
            read = false
        )

        notificationRef.setValue(notification)
            .addOnSuccessListener {
                Log.d("PostDetailActivity", "댓글 알림이 성공적으로 추가되었습니다: $notificationId")
            }
            .addOnFailureListener { exception ->
                Log.e("PostDetailActivity", "댓글 알림 추가 실패: ${exception.message}")
            }
    }
}

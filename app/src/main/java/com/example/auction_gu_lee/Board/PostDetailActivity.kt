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
                    textViewDesiredPrice.text =
                        "구매 희망 가격: ${String.format("%,d원", post.desiredPrice)}"
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
                    Toast.makeText(this@PostDetailActivity, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT)
                        .show()
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
                Toast.makeText(
                    this@PostDetailActivity,
                    "댓글을 불러오는 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
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
                        Toast.makeText(this@PostDetailActivity, "판매 내역이 없습니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@PostDetailActivity,
                        "판매 내역을 불러올 수 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
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
                    Toast.makeText(
                        this@PostDetailActivity,
                        "이미 이 판매 내역을 등록하셨습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser == null) {
                        // 사용자가 로그인되지 않은 경우 처리 (에러 메시지 표시 등)
                        Toast.makeText(this@PostDetailActivity, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                        return // 여기서 함수를 종료하여 더 이상 진행하지 않음
                    }

                    val commentId = commentsRef.push().key ?: UUID.randomUUID().toString()
                    val commentData = mapOf(
                        "commentId" to commentId,
                        "postId" to postId,
                        "userId" to currentUser.uid,
                        "auctionId" to auctionId,
                        "timestamp" to ServerValue.TIMESTAMP // 서버 타임스탬프 사용
                    )


                    commentsRef.child(commentId).setValue(commentData)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@PostDetailActivity,
                                "판매 내역이 댓글로 등록되었습니다.",
                                Toast.LENGTH_SHORT
                            ).show()

                            // 게시글 작성자와 현재 사용자가 다른 경우에만 알림 추가
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                            if (post.userId != currentUserId && currentUserId != null) {
                                addCommentNotification(
                                    post.userId,
                                    postId,
                                    auctionId,
                                    "${post.item} 구매 요청글에 새로운 댓글이 추가되었습니다."
                                )
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this@PostDetailActivity,
                                "댓글 등록 실패: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@PostDetailActivity,
                    "댓글 등록 중 오류가 발생했습니다: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
        val commentRef = database.child("purchase_posts").child(postId).child("comments")
            .child(comment.commentId)
        commentRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "댓글이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "댓글 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addCommentNotification(
        postAuthorId: String,
        postId: String,
        commentAuctionId: String,
        message: String
    ) {
        val userNotificationsRef =
            database.child("users").child(postAuthorId).child("notifications")


        userNotificationsRef.orderByChild("relatedPostId").equalTo(postId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var notificationExists = false

                    // Snapshot 안에 있는 데이터를 순회하면서 중복 여부 확인
                    for (child in snapshot.children) {
                        val notification = child.getValue(Notification::class.java)
                        // 같은 postId와 commenterId로 알림이 존재하는지 확인
                        if (notification?.relatedPostId == postId && notification.commentAuctionId == commentAuctionId) {
                            notificationExists = true
                            break
                        }
                    }

                    // 중복 알림이 없는 경우에만 새 알림 추가
                    if (!notificationExists) {
                        createNotification(userNotificationsRef, postId, commentAuctionId, message)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PostDetailActivity", "알림 확인 중 오류 발생: ${error.message}")
                }
            })
    }

    // 중복 알림이 없을 때 실제로 알림을 생성하는 함수
// 중복 알림이 없을 때 실제로 알림을 생성하는 함수
    private fun createNotification(
        userNotificationsRef: DatabaseReference,
        postId: String,
        commentAuctionId: String, // commenterId는 필수 매개변수로 받음
        message: String
    ) {
        val notificationRef = userNotificationsRef.push()
        val notificationId = notificationRef.key ?: return

        // 알림 객체 생성 시 commenterId를 반드시 포함
        val notificationData = mapOf(
            "id" to notificationId,
            "message" to message,
            "relatedPostId" to postId,
            "commentAuctionId" to commentAuctionId,
            "timestamp" to ServerValue.TIMESTAMP,  // 서버 타임스탬프 사용
            "type" to "comment",
            "read" to false
        )

        notificationRef.setValue(notificationData)
            .addOnSuccessListener {
                Log.d("PostDetailActivity", "새 알림이 성공적으로 추가되었습니다: $notificationId")
            }
            .addOnFailureListener { e ->
                Log.e("PostDetailActivity", "알림 추가 실패: ${e.message}")
            }
    }


}

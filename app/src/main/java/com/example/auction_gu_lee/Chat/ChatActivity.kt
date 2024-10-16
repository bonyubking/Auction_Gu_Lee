package com.example.auction_gu_lee.Chat

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.databinding.ActivityChatBinding
import com.example.auction_gu_lee.models.ChatItem
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import androidx.core.app.ActivityCompat

class ChatActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var binding: ActivityChatBinding
    private lateinit var auctionId: String
    private lateinit var chatRoomId: String
    private lateinit var bidderUid: String // 구매자 UID
    private lateinit var sellerUid: String // 판매자 UID
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_IMAGE_CAPTURE = 2

    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter
    private lateinit var photoURI: Uri

    private var isActivityVisible = false  // 액티비티 가시성 상태 변수 추가
    private lateinit var messagesListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent로부터 경매 ID와 구매자 UID 가져오기
        auctionId = intent.getStringExtra("auction_id") ?: run {
            Toast.makeText(this, "경매 ID를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bidderUid = intent.getStringExtra("bidder_uid") ?: run {
            Toast.makeText(this, "구매자 UID를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        Log.d("ChatActivity", "Successfully fetched bidderUid: $bidderUid")

        // 상단에 경매의 사진 및 정보를 표시
        loadAuctionDetails()

        // 이미지 미리보기 RecyclerView 설정
        imagePreviewAdapter = ImagePreviewAdapter(selectedImageUris) { uri ->
            // 이미지 삭제 이벤트 처리
            selectedImageUris.remove(uri)
            imagePreviewAdapter.notifyDataSetChanged()
        }
        binding.selectedImagesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.selectedImagesRecyclerView.adapter = imagePreviewAdapter

        // 전송 버튼 클릭 리스너 설정
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString()
            if (message.isNotEmpty() || selectedImageUris.isNotEmpty()) {
                uploadSelectedImagesAndSendMessage(message)
                binding.messageInput.text.clear()
                selectedImageUris.clear()
                imagePreviewAdapter.notifyDataSetChanged()

                // 이미지가 모두 삭제되면 RecyclerView를 숨김
                if (selectedImageUris.isEmpty()) {
                    binding.selectedImagesRecyclerView.visibility = View.GONE
                }
            }
        }

        // 나가기 버튼 클릭 리스너 설정
        binding.exitChatButton.setOnClickListener {
            showExitConfirmationDialog()
        }

        // 이미지 첨부 버튼 클릭 리스너 설정
        binding.buttonAttach.setOnClickListener {
            showImageSourceDialog()
        }
    }

    /**
     * 경매의 상세 정보를 로드하여 UI에 표시하는 메서드
     */
    private fun loadAuctionDetails() {
        // 경매 정보 가져오기
        database.child("auctions").child(auctionId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                    val itemName = snapshot.child("item").getValue(String::class.java)
                    val creatorUidValue = snapshot.child("creatorUid").getValue(String::class.java)

                    photoUrl?.let {
                        Glide.with(this@ChatActivity)
                            .load(it)
                            .placeholder(R.drawable.placeholder_image)
                            .into(binding.chatItemImage)
                    }

                    binding.chatItemTitle.text = itemName ?: "품목명 없음"

                    if (creatorUidValue != null) {
                        sellerUid = creatorUidValue // set sellerUid
                        database.child("users").child(sellerUid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    val username = userSnapshot.child("username").getValue(String::class.java)
                                    binding.chatCreatorUsername.text = username ?: "판매자 정보 없음"

                                    // After fetching seller's username, create chatRoomId
                                    createChatRoomId()

                                    // 채팅 메시지 가져오기
                                    loadChatMessages()
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    binding.chatCreatorUsername.text = "판매자 정보 없음"
                                    createChatRoomId() // proceed even if username is not fetched
                                    loadChatMessages()
                                }
                            })
                    } else {
                        binding.chatCreatorUsername.text = "판매자 정보 없음"
                        // if creatorUid is null, cannot create chatRoomId properly
                        Toast.makeText(this@ChatActivity, "판매자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "경매 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }


    /**
     * 새로운 채팅방 ID를 생성하는 메서드
     */
    private fun createChatRoomId() {
        // sellerUid와 bidderUid가 제대로 초기화되었는지 확인
        if (!::sellerUid.isInitialized || !::bidderUid.isInitialized) {
            Log.e("ChatActivity", "sellerUid 또는 bidderUid가 초기화되지 않았습니다.")
            Toast.makeText(this, "채팅방을 생성할 수 없습니다. 판매자 또는 구매자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // chatRoomId를 auctionId|bidderUid|sellerUid 형식으로 생성
        chatRoomId = "${auctionId}|${bidderUid}|${sellerUid}"
        Log.d("ChatActivity", "Created new chatRoomId: $chatRoomId")
    }


    /**
     * 채팅 메시지를 로드하는 메서드
     */
    private fun loadChatMessages() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // messagesListener 초기화 및 할당
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatList = mutableListOf<ChatItem>()
                val updates = mutableMapOf<String, Any>()  // 업데이트할 데이터 저장용

                for (messageSnapshot in snapshot.children) {
                    try {
                        // 메시지 데이터가 ChatItem 객체인지 확인
                        val chatItem = messageSnapshot.getValue(ChatItem::class.java)
                        if (chatItem != null) {
                            chatList.add(chatItem)
                            Log.d("ChatActivity", "Loaded chat item: $chatItem")

                            val messageKey = messageSnapshot.key ?: continue  // 메시지 키 null 체크 후 continue

                            // 액티비티가 가시적인 경우에만 읽음 처리
                            if (isActivityVisible) {
                                // 만약 메시지가 읽지 않은 상태이고, 상대방이 보낸 메시지라면
                                if (!chatItem.isRead && chatItem.senderUid != currentUserId) {
                                    // isRead 값을 true로 설정
                                    updates["$messageKey/isRead"] = true
                                }
                            }
                        } else {
                            // 메시지 데이터가 ChatItem 객체가 아닌 경우 로그 출력
                            val rawValue = messageSnapshot.getValue(String::class.java)
                            Log.w("ChatActivity", "Invalid message format for messageId: ${messageSnapshot.key}, value: $rawValue")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatActivity", "Error parsing messageId: ${messageSnapshot.key}, error: ${e.message}")
                    }
                }

                // 채팅 UI 업데이트
                updateChatUI(chatList)

                // 읽음 상태 업데이트
                if (updates.isNotEmpty()) {
                    // 메시지 노드에 직접 접근하여 업데이트
                    snapshot.ref.updateChildren(updates)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Failed to load chat messages: ${error.message}")
            }
        }

        // 리스너 등록
        database.child("auctions").child(auctionId).child("chats").child(chatRoomId).child("messages")
            .addValueEventListener(messagesListener)
    }

    private fun updateChatUI(chatList: List<ChatItem>) {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.chatMessagesRecyclerView.layoutManager = layoutManager

        val adapter = ChatMessageAdapter(this, chatList)
        binding.chatMessagesRecyclerView.adapter = adapter

        binding.chatMessagesRecyclerView.scrollToPosition(chatList.size - 1)
    }

    /**
     * 메시지를 전송하는 메서드
     */
// ChatActivity.kt 내의 sendMessage() 메서드 수정

    private fun sendMessage(message: String, imageUrls: List<String> = emptyList()) {
        if (!::chatRoomId.isInitialized) {
            Log.e("ChatActivity", "chatRoomId가 초기화되지 않았습니다. 메시지를 전송할 수 없습니다.")
            Toast.makeText(this, "채팅방이 초기화되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val senderUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()

        val messageData = mutableMapOf<String, Any>(
            "senderUid" to senderUid,
            "message" to message,
            "timestamp" to timestamp,
            "imageUrls" to imageUrls,
            "isRead" to false
        )

        // 메시지 참조를 생성하여 메시지 ID를 가져옵니다.
        val chatReference = database.child("auctions").child(auctionId).child("chats").child(chatRoomId)
        val messageRef = chatReference.child("messages").push()
        val messageId = messageRef.key ?: "unknown"

        // 채팅방에 bidderUid와 sellerUid를 저장합니다.
        chatReference.child("bidderUid").setValue(bidderUid)
        chatReference.child("sellerUid").setValue(sellerUid)

        // 메시지를 전송합니다.
        messageRef.setValue(messageData)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("ChatActivity", "Message send failed: ${task.exception?.message}")
                    Toast.makeText(this, "메시지 전송 실패", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("ChatActivity", "Message sent successfully with ID: $messageId and Data: $messageData")
                }
            }
    }



    /**
     * 선택된 이미지를 업로드하고 메시지를 전송하는 메서드
     */
    private fun uploadSelectedImagesAndSendMessage(message: String) {
        val imageUrls = mutableListOf<String>()
        val uploadTasks = mutableListOf<Task<Uri>>()

        if (selectedImageUris.isNotEmpty()) {
            for (uri in selectedImageUris) {
                val uniqueFileName = UUID.randomUUID().toString()
                val ref = storage.child("chat_images/$uniqueFileName")

                val uploadTask = ref.putFile(uri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        ref.downloadUrl
                    }
                uploadTasks.add(uploadTask)
            }

            Tasks.whenAllSuccess<Uri>(uploadTasks)
                .addOnSuccessListener { uris ->
                    for (uri in uris) {
                        imageUrls.add(uri.toString())
                    }
                    // 모든 이미지 업로드가 완료되었으므로 메시지를 전송합니다.
                    sendMessage(message, imageUrls)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
        } else {
            // 업로드할 이미지가 없으므로 메시지를 바로 전송합니다.
            sendMessage(message, imageUrls)
        }
    }


    private fun showExitConfirmationDialog() {
        // AlertDialog.Builder를 사용하여 다이얼로그 생성
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("채팅방 나가기")
        builder.setMessage("채팅방을 나가시겠습니까?")

        // "예" 버튼 설정
        builder.setPositiveButton("예") { dialog, which ->
            // "예"를 선택했을 때 채팅방 나가기 처리
            leaveChatRoomAndReturn()
        }

        // "아니오" 버튼 설정
        builder.setNegativeButton("아니오") { dialog, which ->
            // "아니오"를 선택했을 때 다이얼로그 닫기
            dialog.dismiss()
        }

        // 다이얼로그 표시
        builder.create().show()
    }
    /**
     * 채팅방을 나가고 이전 화면으로 돌아가는 메서드
     */
    private fun leaveChatRoomAndReturn() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val chatReference = database.child("auctions").child(auctionId).child("chats").child(chatRoomId)

        // Firebase에 현재 사용자의 나가기 상태와 타임스탬프 저장 (isRead 필드 제외)
        val exitData = mapOf(
            "exited" to true,
            "timestamp" to System.currentTimeMillis()
        )
        chatReference.child("metadata").child("exitedUsers").child(currentUserId).setValue(exitData)
            .addOnSuccessListener {
                Toast.makeText(this, "채팅방을 나갔습니다.", Toast.LENGTH_SHORT).show()
                // ChatFragment로 돌아가기
                finish() // 현재 Activity 종료
            }
            .addOnFailureListener {
                Log.e("ChatActivity", "Failed to leave chat room: ${it.message}")
                Toast.makeText(this, "채팅방 나가기 실패", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 메시지 리스너 제거
        if (::messagesListener.isInitialized) {
            database.child("auctions").child(auctionId).child("chats").child(chatRoomId).child("messages")
                .removeEventListener(messagesListener)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::chatRoomId.isInitialized) {
            updateMessagesAsRead()
        } else {
            Log.w("ChatActivity", "chatRoomId가 초기화되지 않았습니다. updateMessagesAsRead()를 호출하지 않습니다.")
        }
        isActivityVisible = true  // 액티비티가 가시적으로 변경됨
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false  // 액티비티가 가시적이지 않음
    }

    /**
     * 메시지를 읽음으로 업데이트하는 메서드
     */
    private fun updateMessagesAsRead() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val chatReference = database.child("auctions").child(auctionId).child("chats").child(chatRoomId).child("messages")

        chatReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updates = mutableMapOf<String, Any>()
                for (messageSnapshot in snapshot.children) {
                    val messageSenderUid = messageSnapshot.child("senderUid").getValue(String::class.java) ?: continue
                    val isRead = messageSnapshot.child("isRead").getValue(Boolean::class.java) ?: false

                    if (!isRead && messageSenderUid != currentUserId) {
                        // 메시지를 읽음 처리
                        updates["${messageSnapshot.key}/isRead"] = true
                        Log.d("ChatActivity", "Updating message as read for messageId: ${messageSnapshot.key}")
                    }
                }

                if (updates.isNotEmpty()) {
                    // 메시지 노드에 직접 접근하여 업데이트
                    chatReference.updateChildren(updates)
                        .addOnSuccessListener {
                            Log.d("ChatActivity", "Messages marked as read successfully.")
                        }
                        .addOnFailureListener { error ->
                            Log.e("ChatActivity", "Failed to update messages as read: ${error.message}")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatActivity", "Failed to update messages as read: ${error.message}")
            }
        })
    }

    /**
     * 이미지 소스 선택 다이얼로그를 표시하는 메서드
     */
    private fun showImageSourceDialog() {
        val options = arrayOf("갤러리에서 선택", "카메라로 촬영")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("사진 첨부")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> openImagePicker()
                1 -> {
                    // 카메라 권한 체크 및 요청
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
                    } else {
                        captureImageFromCamera()
                    }
                }
            }
        }
        builder.show()
    }

    // onRequestPermissionsResult() 메서드 추가
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되었으므로 카메라를 실행합니다.
                captureImageFromCamera()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun captureImageFromCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // 이미지 파일을 생성하여 URI를 가져옵니다.
            val photoFile = createImageFile()
            photoFile?.let {
                photoURI = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                // URI 권한 제공 (카메라 앱이 해당 URI에 쓸 수 있도록 허용)
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        } else {
            Toast.makeText(this, "카메라를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
            Log.d("ChatActivity", "Created file: ${file.absolutePath}")
            file
        } catch (ex: IOException) {
            Log.e("ChatActivity", "파일 생성 실패: ${ex.message}")
            null
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // 여러 장의 사진을 선택할 수 있도록 설정
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    data?.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            val imageUri = clipData.getItemAt(i).uri
                            selectedImageUris.add(imageUri)
                        }
                    } ?: data?.data?.let { imageUri ->
                        selectedImageUris.add(imageUri)
                    }
                    imagePreviewAdapter.notifyDataSetChanged()

                    // 이미지를 선택한 후에 RecyclerView를 표시
                    if (selectedImageUris.isNotEmpty()) {
                        binding.selectedImagesRecyclerView.visibility = View.VISIBLE
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    // 카메라 촬영 후 저장된 파일의 URI를 사용합니다.
                    selectedImageUris.add(photoURI)
                    imagePreviewAdapter.notifyDataSetChanged()

                    // 이미지를 촬영한 후에 RecyclerView를 표시
                    binding.selectedImagesRecyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }
}

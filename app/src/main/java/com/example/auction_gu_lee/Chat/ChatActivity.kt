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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
import androidx.core.app.ActivityCompat


class ChatActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var binding: ActivityChatBinding
    private lateinit var auctionId: String
    private lateinit var chatRoomId: String
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_IMAGE_CAPTURE = 2

    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imagePreviewAdapter: ImagePreviewAdapter
    private lateinit var photoURI: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent로부터 경매 ID와 판매자 UID 가져오기
        auctionId = intent.getStringExtra("auction_id") ?: run {
            Toast.makeText(this, "경매 ID를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sellerUid = intent.getStringExtra("seller_uid") ?: run {
            Toast.makeText(this, "판매자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val bidderUid = intent.getStringExtra("bidder_uid") ?: run {
            Toast.makeText(this, "구매자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 1:1 채팅방 고유 ID 생성
        val uidList = listOf(sellerUid, bidderUid).sorted()
        chatRoomId = "${auctionId}|${uidList.joinToString("|")}"

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
                uploadSelectedImagesAndSendMessage(bidderUid, message)
                binding.messageInput.text.clear()
                selectedImageUris.clear()
                imagePreviewAdapter.notifyDataSetChanged()
            }
        }

        // 이미지 첨부 버튼 클릭 리스너 설정
        binding.buttonAttach.setOnClickListener {
            showImageSourceDialog()
        }

        // 채팅 메시지 가져오기
        loadChatMessages(bidderUid)
    }

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
                }
                REQUEST_IMAGE_CAPTURE -> {
                    // 카메라 촬영 후 저장된 파일의 URI를 사용합니다.
                    selectedImageUris.add(photoURI)
                    imagePreviewAdapter.notifyDataSetChanged()
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

    private fun uploadSelectedImagesAndSendMessage(bidderUid: String, message: String) {
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
                    sendMessage(bidderUid, message, imageUrls)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                }
        } else {
            // 업로드할 이미지가 없으므로 메시지를 바로 전송합니다.
            sendMessage(bidderUid, message, imageUrls)
        }
    }


    private fun loadAuctionDetails() {
        // 경매 정보 가져오기
        database.child("auctions").child(auctionId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                    val itemName = snapshot.child("item").getValue(String::class.java)
                    val creatorUid = snapshot.child("creatorUid").getValue(String::class.java)

                    photoUrl?.let {
                        Glide.with(this@ChatActivity)
                            .load(it)
                            .placeholder(R.drawable.placeholder_image)
                            .into(binding.chatItemImage)
                    }

                    binding.chatItemTitle.text = itemName ?: "품목명 없음"

                    if (creatorUid != null) {
                        database.child("users").child(creatorUid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    val username = userSnapshot.child("username").getValue(String::class.java)
                                    binding.chatCreatorUsername.text = username ?: "판매자 정보 없음"
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    binding.chatCreatorUsername.text = "판매자 정보 없음"
                                }
                            })
                    } else {
                        binding.chatCreatorUsername.text = "판매자 정보 없음"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatActivity, "경매 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun sendMessage(bidderUid: String, message: String, imageUrls: List<String> = emptyList()) {
        val messageId = database.child("auctions").child(auctionId).child("chats").child(chatRoomId).push().key ?: return
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()

        val messageData = mutableMapOf<String, Any>(
            "senderUid" to senderUid,
            "message" to message,
            "timestamp" to timestamp,
            "imageUrls" to imageUrls  // 빈 리스트라도 추가
        )

        Log.d("ChatActivity", "Sending message with image URLs: $messageData")

        database.child("auctions").child(auctionId).child("chats").child(chatRoomId).child(messageId)
            .setValue(messageData)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("ChatActivity", "Message send failed: ${task.exception?.message}")
                    Toast.makeText(this, "메시지 전송 실패", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d("ChatActivity", "Message sent successfully with ID: $messageId and Data: $messageData")
                }
            }
    }


    private fun loadChatMessages(bidderUid: String) {
        database.child("auctions").child(auctionId).child("chats").child(chatRoomId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val chatList = mutableListOf<ChatItem>()
                    for (messageSnapshot in snapshot.children) {
                        val chatItem = messageSnapshot.getValue(ChatItem::class.java)
                        chatItem?.let {
                            chatList.add(it)
                            Log.d("ChatActivity", "Loaded chat item: $chatItem")
                        }
                    }
                    updateChatUI(chatList)
                }

                override fun onCancelled(error: DatabaseError) {
                    // 에러 처리
                }
            })
    }


    private fun updateChatUI(chatList: List<ChatItem>) {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        binding.chatMessagesRecyclerView.layoutManager = layoutManager

        val adapter = ChatMessageAdapter(this, chatList)
        binding.chatMessagesRecyclerView.adapter = adapter

        binding.chatMessagesRecyclerView.scrollToPosition(chatList.size - 1)
    }
}


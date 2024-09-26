package com.example.auction_gu_lee.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.auction_gu_lee.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CreateRoomActivity : AppCompatActivity() {

    private lateinit var EA: CheckBox
    private lateinit var kg: CheckBox
    private lateinit var box: CheckBox
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private lateinit var editTextItem: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextDetail: EditText
    private lateinit var editTextStartingPrice: EditText
    private lateinit var imageViewPreview: ImageView
    private lateinit var buttonAttachPhoto: Button
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var buttonComplete: Button

    private val REQUEST_CAMERA_PERMISSION = 101



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_auction)

        // CheckBox 연결
        EA = findViewById(R.id.EA)
        kg = findViewById(R.id.kg)
        box = findViewById(R.id.box)

        // CheckBox 선택 시 다른 체크박스 해제
        EA.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                kg.isChecked = false
                box.isChecked = false
            }
        }

        kg.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                EA.isChecked = false
                box.isChecked = false
            }
        }

        box.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                EA.isChecked = false
                kg.isChecked = false
            }
        }


        // 버튼과 이미지뷰 초기화 초기화
        editTextItem = findViewById(R.id.editText_item) // xml의 EditText id와 연결
        editTextQuantity = findViewById(R.id.editText_quantity) // xml의 EditText id와 연결
        editTextDetail = findViewById(R.id.editText_detail) // xml의 EditText id와 연결
        editTextStartingPrice = findViewById(R.id.editText_startingprice) // xml의 EditText id와 연결
        imageViewPreview = findViewById(R.id.imageView_preview)
        buttonAttachPhoto = findViewById(R.id.button_attach_photo)
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()
        buttonComplete = findViewById(R.id.button_complete)


        // 사진 첨부 버튼 클릭 리스너
        buttonAttachPhoto.setOnClickListener {
            showImageOptions()
        }
        // 완료 버튼 클릭 리스너 (서버에 데이터 저장)
        buttonComplete.setOnClickListener {
            if (::photoUri.isInitialized && photoUri != null) {
                uploadAuctionData() // Firebase에 데이터 저장 함수 호출
            } else {
                Toast.makeText(this, "사진을 첨부해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 사진 선택 또는 촬영 옵션 제공
    private fun showImageOptions() {
        val options = arrayOf("사진첩에서 선택", "직접 촬영하기")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("사진 첨부")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> openGallery()  // 사진첩에서 선택
                1 -> checkCameraPermission()  // 카메라 권한 확인 후 촬영
            }
        }
        builder.show()
    }

    // 카메라 권한 확인 및 요청
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 거부되었는지 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // 권한 설명 다이얼로그 표시
                AlertDialog.Builder(this)
                    .setTitle("카메라 권한 필요")
                    .setMessage("사진 촬영을 위해 카메라 권한이 필요합니다.")
                    .setPositiveButton("권한 요청") { _, _ ->
                        requestCameraPermission()
                    }
                    .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
                    .show()
            } else {
                // 권한 직접 요청
                requestCameraPermission()
            }
        } else {
            // 권한이 이미 허용된 경우
            takePhoto()
        }
    }

    // 카메라 권한 요청
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 카메라 권한이 허용된 경우 사진 촬영 시작
                takePhoto()
            } else {
                // 권한이 거부된 경우
                AlertDialog.Builder(this)
                    .setTitle("카메라 권한 거부됨")
                    .setMessage("카메라 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
                    .setPositiveButton("설정으로 이동") { _, _ ->
                        goToAppSettings()
                    }
                    .setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    // 설정으로 이동
    private fun goToAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    // 사진첩에서 사진 선택
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val selectedImageUri: Uri? = result.data?.data
            selectedImageUri?.let {

                photoUri = it

                imageViewPreview.setImageURI(it)
                imageViewPreview.visibility = ImageView.VISIBLE
            }
        }
    }

    // 카메라로 사진 촬영
    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // 사진을 저장할 파일 생성
        try {
            photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                photoFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraLauncher.launch(intent)
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // 카메라로 촬영된 이미지 처리
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            imageViewPreview.setImageURI(photoUri)
            imageViewPreview.visibility = ImageView.VISIBLE
        }
    }
    // Firebase에 데이터 업로드
    private fun uploadAuctionData() {
        val database = FirebaseDatabase.getInstance().getReference("auctions")
        val storage = FirebaseStorage.getInstance().reference.child("auction_photos/${UUID.randomUUID()}")

        // 사진 업로드
        val uploadTask = storage.putFile(photoUri!!)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            storage.downloadUrl.addOnSuccessListener { uri ->
                val unit = when {
                    EA.isChecked -> "개"
                    box.isChecked -> "박스"
                    kg.isChecked -> "kg"
                    else -> ""}
                // Firebase Realtime Database에 저장할 경매 데이터
                val auction = hashMapOf(
                    "item" to editTextItem.text.toString(),  // EditText에서 문자열 값 가져오기
                    "quantity" to editTextQuantity.text.toString()+""+unit,  // EditText에서 문자열 값 가져오기
                    "detail" to editTextDetail.text.toString(),  // EditText에서 문자열 값 가져오기
                    "startingPrice" to editTextStartingPrice.text.toString(),  // EditText에서 문자열 값 가져오기
                    "photoUrl" to uri.toString(),  // 업로드된 사진의 URL
                    "timestamp" to System.currentTimeMillis() // 경매 생성 시간
                )
                database.push().setValue(auction).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "경매가 성공적으로 생성되었습니다", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "경매 생성에 실패했습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "사진 업로드에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }
}



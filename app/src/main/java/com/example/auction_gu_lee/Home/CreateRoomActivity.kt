package com.example.auction_gu_lee.Home

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.auction_gu_lee.Main.MainActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.Tapbar.HomeFragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.example.auction_gu_lee.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class CreateRoomActivity : AppCompatActivity() {

    private lateinit var EA: CheckBox
    private lateinit var kg: CheckBox
    private lateinit var box: CheckBox
    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore
    private lateinit var database: DatabaseReference  // 수정된 부분
    private lateinit var editTextItem: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextDetail: EditText
    private lateinit var editTextStartingPrice: EditText
    private lateinit var imageViewPreview: ImageView
    private lateinit var buttonAttachPhoto: Button
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var buttonComplete: Button
    private lateinit var resultTextView: TextView
    private lateinit var dateTimeButton: Button
    private lateinit var Uid: String  // 현재 로그인한 사용자의 UID를 저장할 변수
    private var selectedDateTime: Calendar = Calendar.getInstance()
    private var countDownTimer: CountDownTimer? = null

    private val REQUEST_CAMERA_PERMISSION = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_auction)

        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance().getReference("auctions")
        val auth = FirebaseAuth.getInstance()

        // 현재 로그인한 사용자의 UID 가져오기
        Uid = auth.currentUser?.uid ?: ""
        if (Uid.isNotEmpty()) {
            FirebaseDatabase.getInstance().getReference("users").child(Uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentUser = snapshot.getValue(User::class.java)
                        if (currentUser != null) {
                            buttonComplete.isEnabled = true // UID를 가져온 후 버튼 활성화
                        } else {
                            Toast.makeText(this@CreateRoomActivity, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@CreateRoomActivity, "사용자 정보를 가져올 수 없습니다: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Toast.makeText(this, "사용자 인증 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
        // FirebaseDatabase 인스턴스 초기화

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

        // 버튼과 이미지뷰 초기화

        editTextItem = findViewById(R.id.editText_item)
        editTextQuantity = findViewById(R.id.editText_quantity)
        editTextDetail = findViewById(R.id.editText_detail)
        editTextStartingPrice = findViewById(R.id.editText_startingprice)
        imageViewPreview = findViewById(R.id.imageView_preview)
        buttonAttachPhoto = findViewById(R.id.button_attach_photo)
        buttonComplete = findViewById(R.id.button_complete)
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()
        dateTimeButton = findViewById(R.id.dateTimeButton)
        resultTextView = findViewById(R.id.resultTextView)

        // 사진 첨부 버튼 클릭 리스너
        buttonAttachPhoto.setOnClickListener {
            showImageOptions()
        }

        // 완료 버튼 클릭 리스너 (서버에 데이터 저장)
        buttonComplete.setOnClickListener {
            val startingPriceText = editTextStartingPrice.text.toString().replace(",", "") // 콤마 제거
            if (startingPriceText.isEmpty()) {
                Toast.makeText(this, "시작 가격을 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val startingPrice = startingPriceText.toLongOrNull()
            if (startingPrice == null || startingPrice < 100) {
                Toast.makeText(this, "시작 가격은 최소 100 이상이어야 합니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (::photoUri.isInitialized && photoUri != null) {
                uploadAuctionData() // Firebase에 데이터 저장 함수 호출
            } else {
                Toast.makeText(this, "사진을 첨부해주세요", Toast.LENGTH_SHORT).show()
            }
        }


        // 날짜 및 시간 선택 버튼 클릭 시
        dateTimeButton.setOnClickListener {
            hideKeyboard()
            showDateTimePicker()
        }

        editTextStartingPrice.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing
            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString() != current) {
                    editTextStartingPrice.removeTextChangedListener(this) // Avoid infinite loop

                    val cleanString = s.toString().replace(",", "") // Remove existing commas
                    if (cleanString.isNotEmpty()) {
                        val formatted = String.format("%,d", cleanString.toLong()) // Add commas
                        current = formatted
                        editTextStartingPrice.setText(formatted)
                        editTextStartingPrice.setSelection(formatted.length) // Set cursor at the end
                    }

                    editTextStartingPrice.addTextChangedListener(this) // Re-attach the listener
                }
            }
        })
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

    // 사진첩에서 사진 선택
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        galleryLauncher.launch(intent)
    }

    // 날짜 및 시간 선택 다이얼로그 표시
    private fun showDateTimePicker() {
        val currentDate = Calendar.getInstance()

        // 날짜 선택 다이얼로그
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDateTime.set(Calendar.YEAR, year)
                selectedDateTime.set(Calendar.MONTH, month)
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // 시간 선택 다이얼로그
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDateTime.set(Calendar.MINUTE, minute)
                        startCountDown() // 남은 시간 계산 및 표시

                    },
                    currentDate.get(Calendar.HOUR_OF_DAY),
                    currentDate.get(Calendar.MINUTE),
                    true
                ).show()
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // 키보드를 숨기는 함수
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // 카운트다운 타이머 시작
    private fun startCountDown() {
        val currentTime = Calendar.getInstance().timeInMillis
        val targetTime = selectedDateTime.timeInMillis
        val remainingTime = targetTime - currentTime

        if (remainingTime > 0) {
            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer(remainingTime, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val days = millisUntilFinished / (1000 * 60 * 60 * 24)
                    val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
                    val minutes = (millisUntilFinished / (1000 * 60)) % 60
                    val seconds = (millisUntilFinished / 1000) % 60

                    resultTextView.text = String.format(
                        Locale.getDefault(),
                        "%02d:%02d:%02d:%02d",
                        days, hours, minutes, seconds
                    )
                }

                override fun onFinish() {
                    resultTextView.text = "시간이 종료되었습니다!"
                }
            }.start()
        } else {
            resultTextView.text = "목표 시간은 현재 시간보다 이후여야 합니다."
        }
    }

    // 카메라 권한 확인 및 요청
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 거부되었는지 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
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
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(null)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    // 카메라로 촬영된 이미지 처리
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imageViewPreview.setImageURI(photoUri)
                imageViewPreview.visibility = ImageView.VISIBLE
            }
        }

    // Firebase에 데이터 업로드
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
                    else -> ""
                }
                val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val formattedDateTime = dateTimeFormat.format(selectedDateTime.time)

                // 콤마 제거 후 숫자로 변환
                val startingPriceText = editTextStartingPrice.text.toString().replace(",", "")
                val startingPrice = startingPriceText.toLongOrNull() ?: 0L

                // Firebase Realtime Database에 저장할 경매 데이터
                val auction = hashMapOf(
                    "item" to editTextItem.text.toString(),
                    "quantity" to "${editTextQuantity.text} $unit",
                    "detail" to editTextDetail.text.toString(),
                    "startingPrice" to startingPrice,  // 콤마 제거한 숫자 값을 저장
                    "photoUrl" to uri.toString(),
                    "timestamp" to System.currentTimeMillis(),
                    "endTime" to selectedDateTime.timeInMillis,
                    "remainingTime" to resultTextView.text.toString(),
                    "creatorUid" to Uid,
                    "biddersCount" to 0,  // 참가자 수를 0으로 초기화
                    "favoritesCount" to 0,  // 찜 수를 0으로 초기화
                    "category" to "home"
                )

                val auctionRef = database.push() // 새로운 경매 노드 생성
                auctionRef.setValue(auction).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "경매가 성공적으로 생성되었습니다", Toast.LENGTH_SHORT).show()

                        // HomeFragment로 이동 (auction의 ID를 전달)
                        val intent = Intent(this, MainActivity::class.java).apply {
                            putExtra("fragment", "home")
                            putExtra("auction_id", auctionRef.key) // auction의 고유 ID를 전달
                        }
                        startActivity(intent)

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

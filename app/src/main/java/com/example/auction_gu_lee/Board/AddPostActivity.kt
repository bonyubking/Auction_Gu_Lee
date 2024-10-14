package com.example.auction_gu_lee.Board

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.auction_gu_lee.Main.MainActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddPostActivity : AppCompatActivity() {

    private lateinit var editTextItem: EditText
    private lateinit var editTextQuantity: EditText
    private lateinit var editTextDetail: EditText
    private lateinit var editTextDesiredPrice: EditText
    private lateinit var EA: CheckBox
    private lateinit var kg: CheckBox
    private lateinit var box: CheckBox
    private lateinit var buttonComplete: Button

    private val database = FirebaseDatabase.getInstance().getReference("purchase_posts")
    private val auth = FirebaseAuth.getInstance()
    private val currentUserUid = auth.currentUser?.uid ?: "anonymous"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        // 뷰 초기화
        editTextItem = findViewById(R.id.editText_item)
        editTextQuantity = findViewById(R.id.editText_quantity)
        editTextDetail = findViewById(R.id.editText_detail)
        editTextDesiredPrice = findViewById(R.id.editText_desired_price)
        EA = findViewById(R.id.EA)
        kg = findViewById(R.id.kg)
        box = findViewById(R.id.box)
        buttonComplete = findViewById(R.id.button_complete)

        // 체크박스 로직 설정
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

        // 완료 버튼 클릭 리스너 설정
        buttonComplete.setOnClickListener {
            submitPost()
        }
    }

    private fun submitPost() {
        val item = editTextItem.text.toString().trim()
        val quantityValue = editTextQuantity.text.toString().trim()
        val detail = editTextDetail.text.toString().trim()
        val desiredPriceText = editTextDesiredPrice.text.toString().replace(",", "").trim()

        if (item.isEmpty() || quantityValue.isEmpty() || detail.isEmpty() || desiredPriceText.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val desiredPrice = desiredPriceText.toLongOrNull()
        if (desiredPrice == null || desiredPrice <= 0) {
            Toast.makeText(this, "유효한 희망 가격을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val unit = when {
            EA.isChecked -> "개"
            box.isChecked -> "박스"
            kg.isChecked -> "kg"
            else -> ""
        }

        val quantity = "$quantityValue $unit"

        val postId = database.push().key ?: ""

        val post = Post(
            postId = postId,
            userId = currentUserUid,
            item = item,
            desiredPrice = desiredPrice,
            quantity = quantity,
            detail = detail,
            timestamp = System.currentTimeMillis()
        )

        database.child(postId).setValue(post)
            .addOnSuccessListener {
                Toast.makeText(this, "구매 요청 글이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                // 메인 화면으로 이동
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("fragment", "post")
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "등록 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 키보드 숨기는 함수 (필요 시 사용)
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

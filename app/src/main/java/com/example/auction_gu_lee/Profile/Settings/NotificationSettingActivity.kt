package com.example.auction_gu_lee.Profile.Settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.auction_gu_lee.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationSettingActivity : AppCompatActivity() {

    private lateinit var switchChatNotification: Switch
    private lateinit var switchTransactionNotification: Switch
    private lateinit var etKeyword: EditText
    private lateinit var btnAddKeyword: Button
    private lateinit var keywordContainer: LinearLayout

    // Firebase 관련 객체
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    // 사용자 ID
    private var userId: String? = null
    private val keywords = mutableListOf<String>()

    // SharedPreferences 객체
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_setting)

        switchChatNotification = findViewById(R.id.switchChatNotification)
        switchTransactionNotification = findViewById(R.id.switchTransactionNotification)
        etKeyword = findViewById(R.id.etKeyword)
        btnAddKeyword = findViewById(R.id.btnAddKeyword)
        keywordContainer = findViewById(R.id.keywordContainer)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        userId = auth.currentUser?.uid

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("NotificationPreferences", Context.MODE_PRIVATE)

        // 알림 스위치 상태 불러오기
        loadNotificationSettings()

        // 스위치 상태 변경 시 SharedPreferences에 저장
        switchChatNotification.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("chatNotification", isChecked)
        }

        switchTransactionNotification.setOnCheckedChangeListener { _, isChecked ->
            saveNotificationSetting("transactionNotification", isChecked)
        }

        if (userId != null) {
            loadUserKeywords()
        }

        // 키워드 추가 버튼 클릭 시
        btnAddKeyword.setOnClickListener {
            val keyword = etKeyword.text.toString().trim()
            if (keyword.isNotEmpty() && !keywords.contains(keyword)) {
                addKeywordToFirebase(keyword)
                etKeyword.text.clear()
            }
        }
    }

    // SharedPreferences에서 알림 설정 로드
    private fun loadNotificationSettings() {
        // SharedPreferences로부터 채팅 및 거래 알림 상태 불러오기
        val chatNotificationEnabled = sharedPreferences.getBoolean("chatNotification", true)
        val transactionNotificationEnabled = sharedPreferences.getBoolean("transactionNotification", true)

        // 스위치 초기값 설정
        switchChatNotification.isChecked = chatNotificationEnabled
        switchTransactionNotification.isChecked = transactionNotificationEnabled
    }

    // SharedPreferences에 알림 설정 저장
    private fun saveNotificationSetting(key: String, value: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    // Firebase에서 사용자 키워드 로드
    private fun loadUserKeywords() {
        userId?.let {
            database.child("users").child(it).child("keywords").addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    keywords.clear()
                    for (keywordSnapshot in snapshot.children) {
                        val keyword = keywordSnapshot.value as String
                        keywords.add(keyword)
                        addKeywordRow(keyword)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@NotificationSettingActivity, "키워드를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    // 키워드 행 추가
    private fun addKeywordRow(keyword: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val textView = TextView(this)
        textView.text = keyword
        textView.textSize = 16f
        textView.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val deleteButton = Button(this)
        deleteButton.text = "x"
        deleteButton.textSize = 14f
        deleteButton.setOnClickListener {
            keywordContainer.removeView(row)
            keywords.remove(keyword)
            removeKeywordFromFirebase(keyword)
        }

        row.addView(textView)
        row.addView(deleteButton)

        keywordContainer.addView(row)
    }

    // Firebase에 키워드 추가
    private fun addKeywordToFirebase(keyword: String) {
        userId?.let {
            database.child("users").child(it).child("keywords").push().setValue(keyword)
                .addOnSuccessListener {
                    addKeywordRow(keyword)
                    Toast.makeText(this, "키워드가 추가되었습니다.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "키워드 추가에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Firebase에서 키워드 삭제
    private fun removeKeywordFromFirebase(keyword: String) {
        userId?.let {
            val userKeywordsRef = database.child("users").child(it).child("keywords")
            userKeywordsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (keywordSnapshot in snapshot.children) {
                        if (keywordSnapshot.value == keyword) {
                            keywordSnapshot.ref.removeValue()
                            Toast.makeText(this@NotificationSettingActivity, "키워드가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@NotificationSettingActivity, "키워드 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}

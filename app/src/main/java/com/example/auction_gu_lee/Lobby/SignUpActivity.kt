package com.example.auction_gu_lee

import com.example.auction_gu_lee.Database.AppDatabase
import com.example.auction_gu_lee.Database.User
import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val usernameEditText = findViewById<EditText>(R.id.et_username)
        val nameEditText = findViewById<EditText>(R.id.et_name)
        val emailEditText = findViewById<EditText>(R.id.et_email)
        val passwordEditText = findViewById<EditText>(R.id.et_password)
        val phoneEditText = findViewById<EditText>(R.id.et_phone)
        val signUpButton = findViewById<Button>(R.id.btn_signup)

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val phoneNumber = phoneEditText.text.toString().trim()

            if (username.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Room 데이터베이스 객체 가져오기
            val db = AppDatabase.getDatabase(this)
            val userDao = db.userDao()

            // 비동기 작업 시작 (코루틴 사용)
            lifecycleScope.launch {
                val existingUser = userDao.getUserByUsername(username)
                if (existingUser != null) {
                    // 아이디가 이미 존재할 경우
                    Toast.makeText(this@SignUpActivity, "이미 존재하는 아이디입니다.", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    // 아이디가 존재하지 않으면 회원가입 진행

                    val newUser = User(
                        username = username,
                        name = name,
                        email = email,
                        password = password,
                        phoneNumber = phoneNumber
                    )
                    userDao.insertUser(newUser)
                    Toast.makeText(this@SignUpActivity, "회원가입 성공", Toast.LENGTH_SHORT).show()

                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("user_Id", username)  // username은 로그인된 사용자 아이디
                    editor.apply()


                    val intent = Intent(this@SignUpActivity, LobbyActivty::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()

                }
            }
        }
    }
}
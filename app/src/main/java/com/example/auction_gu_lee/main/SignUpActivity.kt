package com.example.auction_gu_lee

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

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phoneNumber.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Room 데이터베이스 객체 가져오기
            val db = AppDatabase.getDatabase(this)
            val userDao = db.userDao()

            // 비동기 작업 시작 (코루틴 사용)
            lifecycleScope.launch {
                val newUser = User(
                    username = username,
                    name = name,
                    email = email,
                    password = password,
                    phoneNumber = phoneNumber
                )
                userDao.insertUser(newUser)
                Toast.makeText(this@SignUpActivity, "회원가입 성공", Toast.LENGTH_SHORT).show()

                // 로그인 화면으로 이동 또는 다른 처리
                  finish()// 현재 화면 종료
            }
        }
    }
}


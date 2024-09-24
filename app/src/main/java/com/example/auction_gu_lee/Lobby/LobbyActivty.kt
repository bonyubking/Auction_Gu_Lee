package com.example.auction_gu_lee

import com.example.auction_gu_lee.Database.AppDatabase
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LobbyActivty : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        val signUpButton = findViewById<Button>(R.id.btn_signup)

        // 회원가입 버튼 클릭 시 SignupActivity로 이동
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)}

        val usernameEditText = findViewById<EditText>(R.id.et_username)
        val passwordEditText = findViewById<EditText>(R.id.et_password)
        val loginButton = findViewById<Button>(R.id.btn_login)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = AppDatabase.getDatabase(this)
            val userDao = db.userDao()

            lifecycleScope.launch {
                val user = userDao.getUser(username, password)
                if (user != null) {
                    Toast.makeText(this@LobbyActivty, "로그인 성공", Toast.LENGTH_SHORT).show()

                    // SharedPreferences에 사용자 아이디 저장
                    val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("user_id", username)  // 로그인 성공 시 username을 저장
                    editor.apply()  // 저장 완료

                    val intent = Intent(this@LobbyActivty, MainActivity::class.java)
                    intent.putExtra("username", username)  // username 전달
                    startActivity(intent)


                    // TODO: 메인 화면으로 이동
                } else {
                    Toast.makeText(this@LobbyActivty, "이메일 또는 비밀번호가 잘못되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}


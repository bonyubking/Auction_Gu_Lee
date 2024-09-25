package com.example.auction_gu_lee

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LobbyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        // FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        val signUpButton = findViewById<Button>(R.id.btn_signup)
        val emailEditText = findViewById<EditText>(R.id.et_username) // username 대신 email 사용
        val passwordEditText = findViewById<EditText>(R.id.et_password)
        val loginButton = findViewById<Button>(R.id.btn_login)

        // 회원가입 버튼 클릭 시 SignUpActivity로 이동
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // 로그인 버튼 클릭 시 이메일과 비밀번호로 로그인
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginWithEmail(email, password)
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 로그인 성공 시 메인 화면으로 이동
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // 로그인 실패 시 에러 메시지 표시
                    Toast.makeText(this, "이메일 또는 비밀번호가 정확하지 않습니다", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

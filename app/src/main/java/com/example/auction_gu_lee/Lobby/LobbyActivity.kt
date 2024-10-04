package com.example.auction_gu_lee.Lobby

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import com.example.auction_gu_lee.R
import androidx.appcompat.app.AppCompatActivity
import com.example.auction_gu_lee.Main.MainActivity
import com.google.firebase.auth.FirebaseAuth

class LobbyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var autoLoginCheckBox: CheckBox
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        // FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        // UI 요소와 연결
        emailEditText = findViewById(R.id.et_username) // username 대신 email 사용
        passwordEditText = findViewById(R.id.et_password)
        loginButton = findViewById(R.id.btn_login)
        signUpButton = findViewById(R.id.btn_signup)
        autoLoginCheckBox = findViewById(R.id.checkbox_auto_login)  // 자동 로그인 체크박스

        // SharedPreferences 초기화
        val sharedPreferences = getSharedPreferences("autoLoginPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // 자동 로그인 설정 확인
        if (sharedPreferences.getBoolean("autoLogin", false)) {
            val savedEmail = sharedPreferences.getString("email", "")
            val savedPassword = sharedPreferences.getString("password", "")

            if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                // 저장된 이메일과 비밀번호로 자동 로그인
                loginWithEmail(savedEmail, savedPassword)
            }
        }

        // 회원가입 버튼 클릭 시 회원가입 화면으로 이동
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

                // 자동 로그인 체크박스가 체크되어 있으면 자동 로그인 정보 저장
                if (autoLoginCheckBox.isChecked) {
                    editor.putBoolean("autoLogin", true)
                    editor.putString("email", email)
                    editor.putString("password", password)
                    editor.apply()
                }
            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 이메일과 비밀번호로 Firebase 로그인
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
                    Toast.makeText(this, "이메일 또는 비밀번호가 정확하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

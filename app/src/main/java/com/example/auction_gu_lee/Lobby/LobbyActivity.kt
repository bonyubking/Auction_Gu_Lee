package com.example.auction_gu_lee.Lobby

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.auction_gu_lee.Main.MainActivity
import com.example.auction_gu_lee.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LobbyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var autoLoginCheckBox: CheckBox
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users")

        emailEditText = findViewById(R.id.et_username)
        passwordEditText = findViewById(R.id.et_password)
        loginButton = findViewById(R.id.btn_login)
        signUpButton = findViewById(R.id.btn_signup)

        // 공백 필터 설정
        val noWhiteSpaceFilter = InputFilter { source, _, _, _, _, _ ->
            if (source.contains(" ")) "" else null
        }

        passwordEditText.filters = arrayOf(noWhiteSpaceFilter)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("LobbyActivity", "User already logged in: ${currentUser.email}")
            // 이미 로그인된 경우 MainActivity로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }


        // **회원가입 버튼 클릭 리스너 설정**
        signUpButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // **로그인 버튼 클릭 리스너 설정**
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // **이메일과 비밀번호로 로그인 시도**
                Log.d("LobbyActivity", "Attempting to log in with email: $email")
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("LobbyActivity", "Login successful.")
                            val userUid = auth.currentUser?.uid
                            val userEmail = auth.currentUser?.email
                            if (userUid != null && userEmail != null) {
                                // **MainActivity로 이동**
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Log.e("LobbyActivity", "Login failed: ${task.exception?.message}")
                            Toast.makeText(this, "이메일 또는 비밀번호가 정확하지 않습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

            } else {
                Log.d("LobbyActivity", "Email or password is empty.")
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Firebase에 로그인 상태 업데이트 및 메인 화면으로 이동
    private fun updateLoginStatusAndNavigate(
        userUid: String,
        email: String,
        password: String,
        editor: SharedPreferences.Editor
    ) {
        Log.d("LobbyActivity", "Setting loggedin to true for user $userUid")
        database.child(userUid).child("loggedin").setValue(true).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("LobbyActivity", "loggedin set to true successfully.")

                // 로그인 성공 시 메인 화면으로 이동
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Log.e("LobbyActivity", "Failed to set loggedin to true: ${task.exception?.message}")
                Toast.makeText(this, "로그인 상태 업데이트 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }


}
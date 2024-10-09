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
        autoLoginCheckBox = findViewById(R.id.checkbox_auto_login)

        // 공백 필터 설정
        val noWhiteSpaceFilter = InputFilter { source, _, _, _, _, _ ->
            if (source.contains(" ")) "" else null
        }

        passwordEditText.filters = arrayOf(noWhiteSpaceFilter)

        val sharedPreferences = getSharedPreferences("autoLoginPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // **자동 로그인 설정 확인**
        val autoLoginEnabled = sharedPreferences.getBoolean("autoLogin", false)
        Log.d("LobbyActivity", "Auto-login enabled: $autoLoginEnabled")
        if (autoLoginEnabled) {
            val savedEmail = sharedPreferences.getString("email", "")
            val savedPassword = sharedPreferences.getString("password", "")
            Log.d("LobbyActivity", "Attempting auto-login with email: $savedEmail")
            if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                loginWithEmail(savedEmail, savedPassword, editor)
            } else {
                Log.d("LobbyActivity", "Saved email or password is empty.")
            }
        } else {
            Log.d("LobbyActivity", "Auto-login is not enabled.")
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
                            if (userUid != null) {
                                // **로그인 상태를 Firebase에 업데이트하고 메인 화면으로 이동**
                                updateLoginStatusAndNavigate(userUid, email, password, editor)
                            }
                        } else {
                            Log.e("LobbyActivity", "Login failed: ${task.exception?.message}")
                            Toast.makeText(this, "이메일 또는 비밀번호가 정확하지 않습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                // **자동 로그인 체크박스가 체크되어 있으면 자동 로그인 정보 저장**
                if (autoLoginCheckBox.isChecked) {
                    Log.d("LobbyActivity", "Auto-login checkbox is checked.")
                    editor.putBoolean("autoLogin", true)
                    editor.putString("email", email)
                    editor.putString("password", password)
                    editor.apply()
                } else {
                    Log.d("LobbyActivity", "Auto-login checkbox is not checked.")
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
                // 자동 로그인 체크박스가 체크되어 있으면 자동 로그인 정보 SharedPreferences에 저장
                if (autoLoginCheckBox.isChecked) {
                    Log.d("LobbyActivity", "Saving auto-login preferences.")
                    editor.putBoolean("autoLogin", true)
                    editor.putString("email", email)
                    editor.putString("password", password)
                    editor.apply()
                }

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

    private fun loginWithEmail(email: String, password: String, editor: SharedPreferences.Editor) {
        Log.d("LobbyActivity", "Attempting to auto-login with email: $email")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LobbyActivity", "Auto-login successful.")
                    val userUid = auth.currentUser?.uid
                    if (userUid != null) {
                        updateLoginStatusAndNavigate(userUid, email, password, editor)
                    }
                } else {
                    Log.e("LobbyActivity", "Auto-login failed: ${task.exception?.message}")
                    Toast.makeText(
                        this,
                        "자동 로그인 실패: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


}
package com.example.auction_gu_lee.Lobby

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
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

        val sharedPreferences = getSharedPreferences("autoLoginPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // 자동 로그인 설정 확인
        if (sharedPreferences.getBoolean("autoLogin", false)) {
            val savedEmail = sharedPreferences.getString("email", "")
            val savedPassword = sharedPreferences.getString("password", "")

            if (!savedEmail.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                // Realtime Database에서 loggedin 상태 확인
                val userUid = auth.currentUser?.uid
                if (userUid != null) {
                    database.child(userUid).child("loggedin")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val loggedin = snapshot.getValue(Boolean::class.java) ?: false
                                if (!loggedin) {
                                    // loggedin이 false인 경우에만 자동 로그인 시도
                                    loginWithEmail(savedEmail, savedPassword, editor)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@LobbyActivity,
                                    "로그인 상태를 확인할 수 없습니다: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
            }
        }

            // 나머지 코드 동일...




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
                // 이메일과 비밀번호로 로그인 시도하기 전에 중복 로그인 상태 확인
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userUid = auth.currentUser?.uid
                            if (userUid != null) {
                                // Firebase에서 중복 로그인 상태 확인
                                database.child(userUid).child("loggedin")
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val loggedin =
                                                snapshot.getValue(Boolean::class.java) ?: false
                                            if (loggedin) {
                                                // 중복 로그인 감지 시
                                                Toast.makeText(
                                                    this@LobbyActivity,
                                                    "다른 기기에서 이미 로그인 중입니다.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                auth.signOut()  // 중복 로그인 시도 차단
                                            } else {
                                                // 중복 로그인이 아니면 로그인 상태 true로 설정하고 메인 화면으로 이동
                                                updateLoginStatusAndNavigate(
                                                    userUid,
                                                    email,
                                                    password,
                                                    editor
                                                )
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(
                                                this@LobbyActivity,
                                                "로그인 상태를 확인할 수 없습니다: ${error.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    })
                            }
                        } else {
                            Toast.makeText(this, "이메일 또는 비밀번호가 정확하지 않습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

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

    // Firebase에 로그인 상태 업데이트 및 메인 화면으로 이동
    private fun updateLoginStatusAndNavigate(
        userUid: String,
        email: String,
        password: String,
        editor: SharedPreferences.Editor
    ) {
        // 로그인 상태를 true로 설정하여 다른 기기에서 중복 로그인이 발생하지 않도록 함
        database.child(userUid).child("loggedin").setValue(true)

        // 자동 로그인 체크박스가 체크되어 있으면 자동 로그인 정보 SharedPreferences에 저장
        if (autoLoginCheckBox.isChecked) {
            editor.putBoolean("autoLogin", true)
            editor.putString("email", email)
            editor.putString("password", password)
            editor.apply()
        }

        // 로그인 성공 시 메인 화면으로 이동
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loginWithEmail(email: String, password: String, editor: SharedPreferences.Editor) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userUid = auth.currentUser?.uid
                    if (userUid != null) {
                        // 로그인 성공 시 Firebase에 로그인 상태를 업데이트
                        updateLoginStatusAndNavigate(userUid, email, password, editor)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "자동 로그인 실패: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 이곳에서 `loggedin`을 변경하지 않습니다.
    }
}
package com.example.auction_gu_lee.Lobby

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.widget.Button
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

        // **이미 로그인된 사용자가 있을 경우 처리**
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
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // **이메일과 비밀번호로 로그인 시도**
                Log.d("LobbyActivity", "Attempting to log in with email: $email")
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("LobbyActivity", "Authentication successful.")
                            val userUid = auth.currentUser?.uid
                            if (userUid != null) {
                                checkAndUpdateLoggedInStatus(userUid)
                            } else {
                                Log.e("LobbyActivity", "User UID is null.")
                                Toast.makeText(this, "로그인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e(
                                "LobbyActivity",
                                "Authentication failed: ${task.exception?.message}"
                            )
                            Toast.makeText(this, "이메일 또는 비밀번호가 정확하지 않습니다.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
            } else {
                Log.d("LobbyActivity", "Email or password is empty.")
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 트랜잭션을 사용하여 'loggedin' 상태를 원자적으로 업데이트하고 로그인 가능 여부를 결정하는 함수
     */
    private fun checkAndUpdateLoggedInStatus(userUid: String) {
        val userLoggedInRef = database.child(userUid).child("loggedin")

        userLoggedInRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val isLoggedIn = currentData.getValue(Boolean::class.java) ?: false
                if (isLoggedIn) {
                    // 이미 로그인된 상태이므로 로그인 시도 취소
                    return Transaction.success(currentData)
                } else {
                    // 로그인 상태를 true로 설정
                    currentData.value = true
                    return Transaction.success(currentData)
                }
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("LobbyActivity", "Transaction failed: ${error.message}")
                    Toast.makeText(
                        this@LobbyActivity,
                        "로그인 상태 확인 중 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    auth.signOut()
                } else if (!committed) {
                    // 트랜잭션이 성공적으로 커밋되지 않았으므로 이미 로그인된 상태
                    Log.d("LobbyActivity", "User is already logged in elsewhere.")
                    Toast.makeText(this@LobbyActivity, "이미 접속 중인 ID입니다.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                } else {
                    // 트랜잭션이 성공적으로 커밋되었으므로 로그인 진행
                    Log.d("LobbyActivity", "'loggedin' set to true successfully via transaction.")
                    // MainActivity로 이동
                    val intent = Intent(this@LobbyActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        })
    }
}
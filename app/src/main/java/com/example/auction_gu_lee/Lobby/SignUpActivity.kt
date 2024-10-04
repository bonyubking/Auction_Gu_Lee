package com.example.auction_gu_lee.Lobby

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.auction_gu_lee.R
import com.example.auction_gu_lee.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        val usernameEditText = findViewById<EditText>(R.id.et_username)
        val nameEditText = findViewById<EditText>(R.id.et_name)
        val emailEditText = findViewById<EditText>(R.id.et_email)
        val passwordEditText = findViewById<EditText>(R.id.et_password)
        val phoneEditText = findViewById<EditText>(R.id.et_phone)
        val signUpButton = findViewById<Button>(R.id.btn_signup)

        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val phone = phoneEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signUpWithFirebase(email, password, username, name, phone)
            } else {
                Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signUpWithFirebase(email: String, password: String, username: String, name: String, phone: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // 회원가입 성공
                    val user = auth.currentUser
                    Toast.makeText(this, "회원가입 성공: ${user?.email}", Toast.LENGTH_SHORT).show()

                    // Firebase Realtime Database에 사용자 정보 저장
                    saveUserToFirebaseDatabase(user?.uid, username, name, email, phone)

                    // 회원가입 후 다음 화면으로 이동
                    val intent = Intent(this, LobbyActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    // 회원가입 실패
                    Toast.makeText(this, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToFirebaseDatabase(uid: String?, username: String, name: String, email: String, phone: String) {
        val database = FirebaseDatabase.getInstance().getReference("users")
        val user = User(uid, username, name, email, phone)

        database.child(uid ?: "").setValue(user)
            .addOnSuccessListener {
                Toast.makeText(this, "사용자 정보 저장 성공", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "사용자 정보 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}


package com.example.auction_gu_lee

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // EditText 및 버튼 연결
        val usernameEditText: EditText = findViewById(R.id.et_username)
        val passwordEditText: EditText = findViewById(R.id.et_password)
        val passwordConfirmEditText: EditText = findViewById(R.id.et_password_confirm) // 비밀번호 확인
        val nameEditText: EditText = findViewById(R.id.et_name)
        val phoneEditText: EditText = findViewById(R.id.et_phone)
        val emailEditText: EditText = findViewById(R.id.et_email)
        val signupButton: Button = findViewById(R.id.btn_signup)

        // 회원가입 버튼 클릭 이벤트
        signupButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val passwordConfirm = passwordConfirmEditText.text.toString()
            val name = nameEditText.text.toString()
            val phone = phoneEditText.text.toString()
            val email = emailEditText.text.toString()

            if (username.isEmpty() || password.isEmpty() || name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else if (password != passwordConfirm) {
                // 비밀번호와 비밀번호 확인이 다르면 메시지 출력
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 회원가입 성공 후 처리 (DBHelper가 없으므로 단순히 메시지 처리)
                Toast.makeText(this, "회원가입 완료! $username", Toast.LENGTH_SHORT).show()
                finish() // 회원가입 완료 후 현재 액티비티 종료
            }
        }
    }
}
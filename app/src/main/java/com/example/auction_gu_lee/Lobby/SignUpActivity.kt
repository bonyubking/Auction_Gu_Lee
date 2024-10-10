package com.example.auction_gu_lee.Lobby

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.util.Log
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

    // 뷰 요소 선언
    private lateinit var usernameEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var passwordConfirmEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var sendVerificationButton: Button
    private lateinit var signUpButton: Button

    // 이메일 인증 확인을 위한 핸들러
    private val handler = Handler(Looper.getMainLooper())
    private var emailCheckRunnable: Runnable? = null

    // 이메일 인증 여부를 추적하는 변수
    private var isVerificationEmailSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        // 뷰 초기화
        initializeViews()

        // 회원가입 버튼 초기에는 비활성화
        signUpButton.isEnabled = false

        // "이메일 인증" 버튼 클릭 리스너
        sendVerificationButton.setOnClickListener { sendVerificationEmail() }

        // "회원가입" 버튼 클릭 리스너
        signUpButton.setOnClickListener { completeSignUp() }
    }

    private fun initializeViews() {
        usernameEditText = findViewById(R.id.et_username)
        nameEditText = findViewById(R.id.et_name)
        emailEditText = findViewById(R.id.et_email)
        passwordEditText = findViewById(R.id.et_password)
        passwordConfirmEditText = findViewById(R.id.et_password_confirm)
        phoneEditText = findViewById(R.id.et_phone)
        sendVerificationButton = findViewById(R.id.btn_send_verification)
        signUpButton = findViewById(R.id.btn_signup)

        // 한글과 영어만 허용하는 필터 생성
        val nameFilter = InputFilter { source, _, _, _, _, _ ->
            if (source.matches(Regex("^[a-zA-Z가-힣]*$"))) null else ""
        }

        // 이름 EditText에 필터 적용
        nameEditText.filters = arrayOf(nameFilter)

        // 공백 입력 방지 필터 생성
        val noWhiteSpaceFilter = InputFilter { source, _, _, _, _, _ ->
            if (source.contains(" ")) "" else null
        }

        // 비밀번호와 비밀번호 확인 필드에 공백 필터 적용
        passwordEditText.filters = arrayOf(noWhiteSpaceFilter)
        passwordConfirmEditText.filters = arrayOf(noWhiteSpaceFilter)
    }

    private fun sendVerificationEmail() {
        val username = usernameEditText.text.toString().trim()
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val passwordConfirm = passwordConfirmEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        // 입력된 정보 유효성 확인
        if (username.isEmpty() || name.isEmpty() || email.isEmpty() ||
            password.isEmpty() || phone.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isVerificationEmailSent) {
            // 처음으로 이메일 인증 이메일 보내기
            createAccountAndSendVerification(email, password, username, name, phone)
            sendVerificationButton.text = "메일 재발송"
        } else {
            // 이미 이메일 인증 이메일이 발송된 경우 -> 다시 이메일 발송
            resendVerificationEmail(email, password)
        }
    }

    private fun createAccountAndSendVerification(email: String, password: String, username: String, name: String, phone: String) {
        // 계정 생성 및 이메일 인증 발송
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            Toast.makeText(this, "인증 이메일을 보냈습니다. 이메일을 확인하세요.", Toast.LENGTH_LONG).show()
                            isVerificationEmailSent = true // 이메일 인증 플래그 업데이트
                            auth.signOut()
                            checkEmailVerified(email, password)
                        } else {
                            Toast.makeText(this, "이메일 인증 발송 실패: ${emailTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    if (task.exception?.message?.contains("The email address is already in use") == true) {
                        Toast.makeText(this, "이미 존재하는 계정입니다. 로그인 후 이메일을 인증하세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "계정 생성 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun resendVerificationEmail(email: String, password: String) {
        // 이미 존재하는 계정으로 로그인 후 이메일 재발송
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { signInTask ->
                if (signInTask.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { emailTask ->
                        if (emailTask.isSuccessful) {
                            Toast.makeText(this, "인증 이메일을 다시 보냈습니다. 이메일을 확인하세요.", Toast.LENGTH_LONG).show()
                            auth.signOut()
                            checkEmailVerified(email, password)
                        } else {
                            Toast.makeText(this, "이메일 재발송 실패: ${emailTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "로그인 실패: ${signInTask.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkEmailVerified(email: String, password: String) {
        emailCheckRunnable = object : Runnable {
            override fun run() {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null && user.isEmailVerified) {
                                auth.signOut()
                                signUpButton.isEnabled = true
                                Toast.makeText(this@SignUpActivity, "이메일 인증이 완료되었습니다. 회원가입 버튼을 눌러 진행하세요.", Toast.LENGTH_SHORT).show()
                                handler.removeCallbacks(this)
                            } else {
                                auth.signOut()
                                handler.postDelayed(this, 3000)
                            }
                        } else {
                            handler.postDelayed(this, 3000)
                        }
                    }
            }
        }
        handler.post(emailCheckRunnable!!)
    }

    private fun completeSignUp() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val name = nameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        saveUserToFirebaseDatabase(user.uid, username, name, email, phone)
                    } else {
                        Toast.makeText(this, "이메일 인증이 완료되지 않았습니다.", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                    }
                } else {
                    Toast.makeText(this, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToFirebaseDatabase(uid: String?, username: String, name: String, email: String, phone: String) {
        val user = User(
            uid = uid,
            username = username,
            name = name,
            email = email,
            phone = phone
        )
        val database = FirebaseDatabase.getInstance().getReference("users")
        database.child(uid ?: "").setValue(user)
            .addOnSuccessListener {
                Toast.makeText(this, "회원가입이 완료되었습니다. 로그인 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LobbyActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "사용자 정보 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("SignUpActivity", "Error saving user data", e)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        emailCheckRunnable?.let { handler.removeCallbacks(it) }
    }
}

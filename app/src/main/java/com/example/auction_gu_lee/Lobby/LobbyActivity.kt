package com.example.auction_gu_lee.Lobby

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputFilter
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.auction_gu_lee.Main.MainActivity
import com.example.auction_gu_lee.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import com.kakao.sdk.user.UserApiClient
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import com.google.android.gms.auth.api.signin.GoogleSignInClient



class LobbyActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: Button
    private lateinit var googleSignInClient: GoogleSignInClient // 선언 추가


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        auth = FirebaseAuth.getInstance()

        // 구글 로그인 버튼 설정
        val googleLoginButton = findViewById<ImageButton>(R.id.btn_google_login)
        googleLoginButton.setOnClickListener {
            signInWithGoogle()
        }

        // Google 로그인 옵션 구성
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id)) // 웹 클라이언트 ID 사용
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso) // googleSignInClient 초기화

        database = FirebaseDatabase.getInstance().getReference("users")

        emailEditText = findViewById(R.id.et_email)
        passwordEditText = findViewById(R.id.et_password)
        loginButton = findViewById(R.id.btn_login)
        signUpButton = findViewById(R.id.btn_signup)

        // 해시키 출력 코드
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = String(Base64.encode(md.digest(), 0))
                Log.d("KakaoLogin", "KeyHash: $keyHash")
            }
        } catch (e: NoSuchAlgorithmException) {
            Log.e("KakaoLogin", "해시키를 얻는 중 오류 발생", e)
        } catch (e: Exception) {
            Log.e("KakaoLogin", "해시키를 얻는 중 오류 발생", e)
        }

        // 카카오 로그인 버튼 설정
        val kakaoLoginButton = findViewById<ImageButton>(R.id.btn_kakao_login)
        kakaoLoginButton.setOnClickListener {
            kakaoLogin()
        }

        // 비밀번호 입력칸에 공백 입력 방지 및 최대 길이 15자
        val noWhiteSpaceFilter = InputFilter { source, _, _, _, _, _ ->
            if (source.contains(" ")) "" else null
        }
        val maxLengthFilter = InputFilter.LengthFilter(15)
        passwordEditText.filters = arrayOf(noWhiteSpaceFilter, maxLengthFilter)

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
                return if (isLoggedIn) {
                    // 이미 로그인된 상태이므로 트랜잭션을 실패로 처리
                    Log.d("LobbyActivity", "User is already logged in. Canceling login.")
                    Transaction.abort()
                } else {
                    // 로그인 상태를 true로 설정
                    Log.d("LobbyActivity", "User is not logged in. Proceeding with login.")
                    currentData.value = true
                    Transaction.success(currentData)
                }
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("LobbyActivity", "Transaction failed: ${error.message}")
                    Toast.makeText(this@LobbyActivity, "로그인 상태 확인 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                } else if (!committed) {
                    // 트랜잭션이 커밋되지 않았으므로 이미 로그인된 상태
                    Log.d("LobbyActivity", "User is already logged in elsewhere.")
                    Toast.makeText(this@LobbyActivity, "이미 접속 중인 ID입니다.", Toast.LENGTH_SHORT).show()
                    auth.signOut()  // 로그아웃 처리
                } else {
                    // 트랜잭션이 성공적으로 커밋되었으므로 로그인 진행
                    Log.d("LobbyActivity", "'loggedin' set to true successfully via transaction.")
                    val intent = Intent(this@LobbyActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        })
    }


    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Handle Google Sign-In result
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w("LobbyActivity", "Google sign in failed", e)
                Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LobbyActivity", "signInWithCredential:success")
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Log.w("LobbyActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private fun kakaoLogin() {
        // 카카오톡 설치 여부 확인 후 로그인 시도
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    Log.e("KakaoLogin", "카카오톡 로그인 실패: ${error.message}")
                    // 오류 처리 코드 추가
                } else if (token != null) {
                    Log.d("KakaoLogin", "카카오톡 로그인 성공: ${token.accessToken}")
                    fetchKakaoUserInfo()
                }
            }
        } else {
            // 카카오 계정으로 로그인
            UserApiClient.instance.loginWithKakaoAccount(this) { token, error ->
                if (error != null) {
                    Log.e("KakaoLogin", "카카오 계정 로그인 실패: ${error.message}")
                    // 오류 처리 코드 추가
                } else if (token != null) {
                    Log.d("KakaoLogin", "카카오 계정 로그인 성공: ${token.accessToken}")
                    fetchKakaoUserInfo()
                }
            }
        }
    }

    private fun fetchKakaoUserInfo() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e("KakaoLogin", "사용자 정보 요청 실패: ${error.message}")
            } else if (user != null) {
                Log.d("KakaoLogin", "사용자 정보 요청 성공: ${user.kakaoAccount?.email}")

                // 필요한 사용자 정보 사용
                val email = user.kakaoAccount?.email ?: "unknown_email@kakao.com"
                // 다음 단계로 이동
                navigateToMainActivity()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
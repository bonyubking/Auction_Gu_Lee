package com.example.auction_gu_lee.Profile.Settings

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.auction_gu_lee.models.User
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.auction_gu_lee.R

class UserSettingActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvPassword: TextView

    private lateinit var btnChangeUsername: ImageButton
    private lateinit var btnChangePhone: ImageButton
    private lateinit var btnChangePassword: ImageButton

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_setting)

        // UI 요소 초기화
        tvName = findViewById(R.id.tv_value_name)
        tvUsername = findViewById(R.id.tv_value_username)
        tvEmail = findViewById(R.id.tv_value_email)
        tvPhone = findViewById(R.id.tv_value_phone)
        tvPassword = findViewById(R.id.tv_value_password)

        btnChangeUsername = findViewById(R.id.btn_change_username)
        btnChangePhone = findViewById(R.id.btn_change_phone)
        btnChangePassword = findViewById(R.id.btn_change_password)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users") // 대소문자 일관성 유지

        // 현재 사용자 정보 불러오기
        loadUserInfo()

        if (!isPasswordProvider()) {
            btnChangePassword.visibility = View.GONE
        } else {
            btnChangePassword.visibility = View.VISIBLE
        }

        // 변경 버튼 클릭 리스너 설정
        btnChangeUsername.setOnClickListener {
            showChangeUsernameDialog()
        }

        btnChangePhone.setOnClickListener {
            showChangePhoneDialog()
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            database.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        tvName.text = it.name
                        tvUsername.text = it.username
                        tvEmail.text = it.email
                        tvPhone.text = it.phone
                        tvPassword.text = "************" // 비밀번호는 보안상 표시하지 않음
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UserSettingActivity, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showChangeUsernameDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("닉네임 변경")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "새 닉네임을 입력하세요"

        // 공백 입력 방지 필터 추가
        val noSpaceFilter = InputFilter { source, start, end, dest, dstart, dend ->
            if (source.contains(" ")) {
                ""
            } else {
                null
            }
        }
        input.filters = arrayOf(noSpaceFilter)

        builder.setView(input)

        builder.setPositiveButton("변경") { dialog, _ ->
            val newUsername = input.text.toString().trim()
            if (newUsername.isNotEmpty()) {
                updateUsername(newUsername)
            } else {
                Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun isPasswordProvider(): Boolean {
        val user = auth.currentUser
        user?.let {
            val providers = it.providerData.map { providerInfo -> providerInfo.providerId }
            return providers.contains("password")
        }
        return false
    }

    private fun updateUsername(newUsername: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val updates = HashMap<String, Any>()
            updates["username"] = newUsername

            database.child(uid).updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        tvUsername.text = newUsername
                        Toast.makeText(this, "닉네임이 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "닉네임 변경 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChangePhoneDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("휴대폰 번호 변경")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_PHONE
        input.hint = "새 휴대폰 번호를 입력하세요"

        // 공백 입력 방지 필터 추가
        val noSpaceFilter = InputFilter { source, start, end, dest, dstart, dend ->
            if (source.contains(" ")) {
                ""
            } else {
                null
            }
        }
        input.filters = arrayOf(noSpaceFilter)

        builder.setView(input)

        builder.setPositiveButton("변경") { dialog, _ ->
            val newPhone = input.text.toString().trim()
            if (newPhone.isNotEmpty()) {
                if (isPhoneNumberValid(newPhone)) {
                    updatePhone(newPhone)
                } else {
                    Toast.makeText(this, "유효한 휴대폰 번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "휴대폰 번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun isPhoneNumberValid(phone: String): Boolean {
        // 간단한 휴대폰 번호 형식 검증 (예: 010-1234-5678)
        val phonePattern = Regex("^\\d{3}\\d{4}\\d{4}$")
        return phone.matches(phonePattern)
    }

    private fun updatePhone(newPhone: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val updates = HashMap<String, Any>()
            updates["phone"] = newPhone

            database.child(uid).updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        tvPhone.text = newPhone
                        Toast.makeText(this, "휴대폰 번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "휴대폰 번호 변경 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChangePasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("비밀번호 변경")

        // 레이아웃을 수직으로 배치하여 현재 비밀번호, 새 비밀번호, 비밀번호 확인을 입력받음
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputCurrent = EditText(this)
        inputCurrent.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        inputCurrent.hint = "현재 비밀번호"
        layout.addView(inputCurrent)

        val inputNew = EditText(this)
        inputNew.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        inputNew.hint = "새 비밀번호(최소 6자)"
        layout.addView(inputNew)

        val inputConfirm = EditText(this)
        inputConfirm.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        inputConfirm.hint = "비밀번호 확인(최소 6자)ㅔ"
        layout.addView(inputConfirm)

        // 공백 입력 방지 필터 추가
        val noSpaceFilter = InputFilter { source, start, end, dest, dstart, dend ->
            if (source.contains(" ")) {
                ""
            } else {
                null
            }
        }
        inputCurrent.filters = arrayOf(noSpaceFilter)
        inputNew.filters = arrayOf(noSpaceFilter)
        inputConfirm.filters = arrayOf(noSpaceFilter)

        builder.setView(layout)

        builder.setPositiveButton("변경") { dialog, _ ->
            val currentPassword = inputCurrent.text.toString().trim()
            val newPassword = inputNew.text.toString().trim()
            val confirmPassword = inputConfirm.text.toString().trim()

            when {
                currentPassword.isEmpty() -> {
                    Toast.makeText(this, "현재 비밀번호.", Toast.LENGTH_SHORT).show()
                }
                newPassword.isEmpty() -> {
                    Toast.makeText(this, "새 비밀번호(최소 6자).", Toast.LENGTH_SHORT).show()
                }
                confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "비밀번호 확인(최소 6자).", Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
                newPassword == currentPassword -> {
                    Toast.makeText(this, "이미 사용 중인 비밀번호입니다.", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // 비밀번호 변경 로직 실행
                    reauthenticateAndChangePassword(currentPassword, newPassword)
                }
            }

            dialog.dismiss()
        }

        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun reauthenticateAndChangePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential)
                .addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        // 비밀번호가 동일한지 이미 비교했으므로, 비밀번호 업데이트 진행
                        updatePassword(newPassword)
                    } else {
                        Toast.makeText(this, "현재 비밀번호가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePassword(newPassword: String) {
        val user = auth.currentUser
        if (user != null) {
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        tvPassword.text = "************" // 비밀번호는 보안상 표시하지 않음

                    } else {
                        Toast.makeText(this, "비밀번호 변경 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}

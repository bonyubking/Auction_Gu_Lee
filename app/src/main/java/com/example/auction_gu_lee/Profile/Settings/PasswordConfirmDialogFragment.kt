package com.example.auction_gu_lee.Profile.Settings

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class PasswordConfirmationDialogFragment(
    private val onPasswordConfirmed: () -> Unit
) : DialogFragment() {

    private lateinit var inputPassword: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("비밀번호 확인")

        // 레이아웃 생성
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // 비밀번호 입력 필드
        inputPassword = EditText(requireContext())
        inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        inputPassword.hint = "비밀번호를 입력하세요"
        layout.addView(inputPassword)

        builder.setView(layout)

        // 확인 버튼 (초기에는 null로 설정, 클릭 리스너는 onStart에서 설정)
        builder.setPositiveButton("확인", null)

        // 취소 버튼
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.cancel()
        }

        return builder.create()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as AlertDialog?
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val password = inputPassword.text.toString().trim()
            if (password.isNotEmpty()) {
                authenticateUser(password)
            } else {
                showError("비밀번호를 입력해주세요.")
            }
        }
    }

    private fun authenticateUser(password: String) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null && currentUser.email != null) {
            // 사용자 재인증
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, password)
            currentUser.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 인증 성공, 콜백 호출 및 다이얼로그 닫기
                        onPasswordConfirmed()
                        dismiss()
                    } else {
                        // 인증 실패, 오류 메시지 표시
                        showError("비밀번호가 올바르지 않습니다.")
                    }
                }
        } else {
            showError("사용자가 로그인되어 있지 않습니다.")
        }
    }

    private fun showError(message: String) {
        if (isAdded) { // 프래그먼트가 여전히 액티비티에 연결되어 있는지 확인
            AlertDialog.Builder(requireContext())
                .setTitle("인증 실패")
                .setMessage(message)
                .setPositiveButton("확인") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}

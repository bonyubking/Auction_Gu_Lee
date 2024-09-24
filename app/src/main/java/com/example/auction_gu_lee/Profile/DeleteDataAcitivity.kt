package com.example.auction_gu_lee.Profile

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.auction_gu_lee.R

class DeleteDataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showDeleteCacheDialog()
    }

    // 캐시 데이터 삭제 여부를 묻는 다이얼로그
    private fun showDeleteCacheDialog() {
        // AlertDialog Builder 사용
        val builder = AlertDialog.Builder(this)
        builder.setTitle("캐시 데이터 삭제")
        builder.setMessage("정말 캐시 데이터를 삭제하시겠습니까?")

        // '예' 버튼
        builder.setPositiveButton("예") { dialog, which ->
            // 캐시 삭제 로직 추가 (필요 시)
            Toast.makeText(this, "캐시 데이터가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // '아니오' 버튼
        builder.setNegativeButton("아니오") { dialog, which ->
            // 이전 화면으로 이동
            finish()  // 현재 액티비티를 종료하여 이전 화면으로 돌아감
        }

        // 다이얼로그 띄우기
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}

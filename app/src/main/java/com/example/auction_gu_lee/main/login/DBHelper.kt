package com.example.auction_gu_lee

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// DBHelper 클래스
class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UserDB.db" // 데이터베이스 이름
        private const val DATABASE_VERSION = 1        // 데이터베이스 버전

        // 테이블 및 컬럼 이름
        private const val TABLE_USERS = "users"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
    }

    // 테이블 생성 SQL 문
    private val CREATE_USERS_TABLE = ("CREATE TABLE " + TABLE_USERS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_USERNAME + " TEXT,"
            + COLUMN_PASSWORD + " TEXT" + ")")

    override fun onCreate(db: SQLiteDatabase?) {
        // 데이터베이스가 처음 만들어질 때 호출됨
        db?.execSQL(CREATE_USERS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // 데이터베이스가 업그레이드될 때 호출됨
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // 사용자 등록 함수
    fun addUser(username: String, password: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("username", username)
            put("password", password)
        }

        // 중복된 사용자명을 막기 위한 쿼리
        val cursor = db.query(
            "users", arrayOf("id"), "username = ?", arrayOf(username),
            null, null, null
        )

        return if (cursor.count > 0) {
            cursor.close()
            db.close()
            false // 이미 존재하는 사용자
        } else {
            // 사용자 정보 삽입
            db.insert("users", null, values)
            cursor.close()
            db.close()
            true // 사용자 추가 성공
        }
    }
    // 로그인 확인 함수
    fun checkUser(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor: Cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(username, password),
            null, null, null
        )
        val isUserValid = cursor.count > 0
        cursor.close()
        db.close()

        return isUserValid // 일치하는 레코드가 있으면 true 반환
    }
}


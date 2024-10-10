package com.example.auction_gu_lee

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.auction_gu_lee.Lobby.LobbyActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "새로운 토큰 생성: $token")
        // 현재 로그인된 사용자의 UID를 가져와 Firebase Database에 토큰 저장
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            val uid = user.uid
            val database = FirebaseDatabase.getInstance().getReference("users")
            database.child(uid).child("FCMToken").setValue(token)
                .addOnSuccessListener {
                    Log.d("FCM", "새로운 FCM 토큰이 성공적으로 저장되었습니다.")
                }
                .addOnFailureListener { e ->
                    Log.w("FCM", "새로운 FCM 토큰 저장에 실패했습니다.", e)
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 메시지 수신 처리
        remoteMessage.notification?.let {
            sendNotification(it.title, it.body)
        }

        // 데이터 메시지 처리 (필요 시)
        remoteMessage.data.isNotEmpty().let {
            // 데이터 처리 로직 추가 가능
        }
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, LobbyActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = "default_channel_id"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon_email) // 알림 아이콘 설정
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Android Oreo 이상에서 Notification Channel 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}

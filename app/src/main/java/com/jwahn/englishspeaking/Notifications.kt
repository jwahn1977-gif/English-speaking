package com.jwahn.englishspeaking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Notifications {
    const val CHANNEL_PLAYBACK = "news_playback"
    const val CHANNEL_REMINDER = "daily_reminder"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PLAYBACK, "뉴스 듣기", NotificationManager.IMPORTANCE_LOW).apply {
                description = "화면이 꺼져도 뉴스를 계속 들을 때 표시"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "복습 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "매일 정해진 시간에 복습할 단어와 패턴을 알려줌"
            }
        )
    }

    /** Android 13 이상에서 알림 권한이 필요한지 */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
}

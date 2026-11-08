package com.jwahn.englishspeaking.news

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.jwahn.englishspeaking.App
import com.jwahn.englishspeaking.MainActivity
import com.jwahn.englishspeaking.Notifications
import com.jwahn.englishspeaking.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 뉴스 듣기를 화면이 꺼져도 계속하게 해 주는 포그라운드 서비스.
 * 알림에서 재생/일시정지, 이전/다음 문장, 닫기를 할 수 있다.
 */
class NewsPlaybackService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_TOGGLE = "toggle"
        const val ACTION_NEXT = "next"
        const val ACTION_PREV = "prev"
        const val ACTION_CLOSE = "close"
        private const val NOTIFICATION_ID = 1001

        @Volatile var running = false
            private set

        fun start(context: Context) {
            if (running) return
            val i = Intent(context, NewsPlaybackService::class.java).setAction(ACTION_START)
            runCatching { ContextCompat.startForegroundService(context, i) }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private val player get() = (application as App).container.newsPlayer

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        Notifications.ensureChannels(this)
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, build(player.state.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        scope.launch {
            player.state.map { Triple(it.index, it.playing, it.lessonId) }.distinctUntilChanged().collect {
                val st = player.state.value
                if (!st.active) { stopSelf(); return@collect }
                updateWakeLock(st.playing)
                runCatching { NotificationManagerCompat.from(this@NewsPlaybackService).notify(NOTIFICATION_ID, build(st)) }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> player.toggle()
            ACTION_NEXT -> player.next()
            ACTION_PREV -> player.prev()
            ACTION_CLOSE -> { player.close(); stopSelf() }
        }
        return START_NOT_STICKY
    }

    private fun updateWakeLock(playing: Boolean) {
        if (playing) {
            if (wakeLock?.isHeld != true) {
                val pm = getSystemService(POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EnglishSpeaking:news").apply {
                    setReferenceCounted(false)
                    acquire(60 * 60 * 1000L) // 최대 1시간
                }
            }
        } else {
            wakeLock?.takeIf { it.isHeld }?.release()
        }
    }

    private fun action(action: String, code: Int): PendingIntent = PendingIntent.getService(
        this, code, Intent(this, NewsPlaybackService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun build(st: PlayerState): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, Notifications.CHANNEL_PLAYBACK)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(st.title.ifBlank { "뉴스 듣기" })
            .setContentText(if (st.active) "${st.index + 1}/${st.sentences.size} · ${st.current}" else "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(st.current))
            .setContentIntent(open)
            .setOngoing(st.playing)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "이전", action(ACTION_PREV, 1))
            .addAction(0, if (st.playing) "일시정지" else "재생", action(ACTION_TOGGLE, 2))
            .addAction(0, "다음", action(ACTION_NEXT, 3))
            .addAction(0, "닫기", action(ACTION_CLOSE, 4))
            .build()
    }

    override fun onDestroy() {
        running = false
        wakeLock?.takeIf { it.isHeld }?.release()
        scope.cancel()
        super.onDestroy()
    }
}

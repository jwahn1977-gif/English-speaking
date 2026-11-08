package com.jwahn.englishspeaking.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jwahn.englishspeaking.App
import com.jwahn.englishspeaking.MainActivity
import com.jwahn.englishspeaking.Notifications
import com.jwahn.englishspeaking.R
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** 매일 복습 알림 예약/취소 */
object Reminder {
    private const val WORK = "daily_review_reminder"
    const val NOTIFICATION_ID = 2001

    /** 지금부터 다음 hour:minute 까지 남은 시간 */
    fun delayUntil(hour: Int, minute: Int, now: LocalDateTime = LocalDateTime.now()): Duration {
        var target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }

    fun schedule(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) { wm.cancelUniqueWork(WORK); return }
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(hour, minute).toMinutes(), TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    /** 알림 문구. 보낼 필요가 없으면 null */
    fun message(dueWords: Int, duePatterns: Int, studiedToday: Boolean, streak: Int): String? {
        val parts = buildList {
            if (dueWords > 0) add("단어 ${dueWords}개")
            if (duePatterns > 0) add("패턴 ${duePatterns}개")
        }
        return when {
            parts.isNotEmpty() -> "오늘 복습할 ${parts.joinToString(", ")}가 기다리고 있어요." +
                if (streak > 0 && !studiedToday) " 🔥 ${streak}일 연속 기록을 이어가세요!" else ""
            !studiedToday -> "오늘은 아직 영어를 안 했어요. AI 튜터와 1분만 대화해 볼까요?"
            else -> null
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as App).container
        val now = System.currentTimeMillis()
        val dueWords = c.db.vocab().dueWordCount(now).first()
        val duePatterns = c.db.patterns().dueCount(now).first()
        val stats = c.db.stats().recent().first()
        val today = LocalDate.now().toString()
        val active = stats.filter { it.reviews + it.sentences + it.listened > 0 }.map { it.date }.toSet()
        val streak = com.jwahn.englishspeaking.domain.Streak.count(active)
        val msg = Reminder.message(dueWords, duePatterns, today in active, streak) ?: return Result.success()

        Notifications.ensureChannels(applicationContext)
        if (Notifications.needsPermission(applicationContext)) return Result.success()
        val open = PendingIntent.getActivity(
            applicationContext, 0, Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("말해요 영어 복습 시간 ⏰")
            .setContentText(msg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(Reminder.NOTIFICATION_ID, n) }
        return Result.success()
    }
}

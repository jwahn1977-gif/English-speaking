package com.jwahn.englishspeaking

import android.app.Application
import com.jwahn.englishspeaking.data.Repo
import com.jwahn.englishspeaking.data.db.AppDatabase
import com.jwahn.englishspeaking.data.llm.LlmClient
import com.jwahn.englishspeaking.data.llm.OpenRouterAuth
import com.jwahn.englishspeaking.data.settings.Profiles
import com.jwahn.englishspeaking.data.news.NewsFetcher
import com.jwahn.englishspeaking.data.settings.SettingsStore
import com.jwahn.englishspeaking.news.NewsPlaybackService
import com.jwahn.englishspeaking.news.NewsPlayer
import com.jwahn.englishspeaking.news.Speaker
import com.jwahn.englishspeaking.reminder.Reminder
import com.jwahn.englishspeaking.speech.Tts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** 간단한 수동 DI 컨테이너 */
class AppContainer(app: Application, val profiles: Profiles = Profiles(app), val profileId: String = profiles.currentId) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = AppDatabase.create(app, profileId)
    val repo = Repo(db)
    val settings = SettingsStore(app, profileId)
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain -> testInterceptor?.intercept(chain) ?: chain.proceed(chain.request()) }
        .build()
    val llm = LlmClient(settings, http)
    val news = NewsFetcher(http)
    val openRouterAuth = OpenRouterAuth(http)
    val tts = Tts(app)
    val newsPlayer = NewsPlayer(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
        speaker = Speaker { text, rate -> tts.speakAndWait(text, rate) },
        stopSpeech = { tts.stop() },
        onSentenceDone = { lessonId, index ->
            repo.recordActivity(listened = 1)
            db.news().updateLessonPosition(lessonId, index)
        },
    )

    companion object {
        /** 테스트에서 네트워크 응답을 흉내 내기 위한 훅 (앱에서는 사용하지 않음) */
        @Volatile var testInterceptor: okhttp3.Interceptor? = null
    }

    init {
        scope.launch { repo.seedIfNeeded() }
        settings.state.onEach { tts.rate = it.ttsRate; tts.voiceName = it.ttsVoice }.launchIn(scope)
        Notifications.ensureChannels(app)
        // 다른 곳(카드 학습, 튜터 등)에서 읽기를 시작하면 뉴스 재생은 일시정지
        tts.onUserSpeak = { if (newsPlayer.state.value.playing) newsPlayer.pause() }
        // 재생이 시작되면 화면이 꺼져도 계속 듣도록 포그라운드 서비스 시작
        newsPlayer.onPlayingChanged = { playing -> if (playing) NewsPlaybackService.start(app) }
        settings.current.let { runCatching { Reminder.schedule(app, it.reminderEnabled, it.reminderHour, it.reminderMinute) } }
    }

    /** 프로필 전환 등으로 이 컨테이너를 버릴 때 */
    fun close() {
        newsPlayer.close()
        tts.shutdown()
        scope.cancel()
        runCatching { db.close() }
    }
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** 가족 프로필 전환: 그 프로필의 학습 기록과 설정으로 앱을 다시 연다 */
    fun switchProfile(id: String) {
        val profiles = container.profiles
        if (id == container.profileId) return
        container.close()
        profiles.currentId = id
        container = AppContainer(this, profiles, id)
    }
}

package com.jwahn.englishspeaking

import android.app.Application
import com.jwahn.englishspeaking.data.Repo
import com.jwahn.englishspeaking.data.db.AppDatabase
import com.jwahn.englishspeaking.data.llm.LlmClient
import com.jwahn.englishspeaking.data.news.NewsFetcher
import com.jwahn.englishspeaking.data.settings.SettingsStore
import com.jwahn.englishspeaking.speech.Tts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** 간단한 수동 DI 컨테이너 */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = AppDatabase.create(app)
    val repo = Repo(db)
    val settings = SettingsStore(app)
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain -> testInterceptor?.intercept(chain) ?: chain.proceed(chain.request()) }
        .build()
    val llm = LlmClient(settings, http)
    val news = NewsFetcher(http)
    val tts = Tts(app)

    companion object {
        /** 테스트에서 네트워크 응답을 흉내 내기 위한 훅 (앱에서는 사용하지 않음) */
        @Volatile var testInterceptor: okhttp3.Interceptor? = null
    }

    init {
        scope.launch { repo.seedIfNeeded() }
        settings.state.onEach { tts.rate = it.ttsRate }.launchIn(scope)
    }
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

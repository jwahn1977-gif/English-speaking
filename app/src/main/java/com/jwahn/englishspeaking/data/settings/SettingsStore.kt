package com.jwahn.englishspeaking.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

enum class Provider(val label: String, val defaultModel: String, val keyHint: String) {
    /** 브라우저 로그인 한 번으로 연결(OAuth). Claude·GPT·Gemini 모델을 모두 고를 수 있다 */
    OPENROUTER("OpenRouter (웹 로그인)", "openrouter/auto", "sk-or-..."),
    CLAUDE("Claude (Anthropic)", "claude-sonnet-5-5", "sk-ant-..."),
    OPENAI("ChatGPT (OpenAI)", "gpt-4o-mini", "sk-..."),
    GEMINI("Gemini (Google)", "gemini-2.5-flash", "AIza..."),
}

/** 튜터 난이도. AUTO = 사용 어휘 수로 자동 결정 */
enum class TutorLevel(val label: String) {
    AUTO("자동(레벨 기준)"), KIDS("어린이"), BEGINNER("초급"), INTERMEDIATE("중급"), ADVANCED("고급")
}

/** 첫 실행에서 고르는 학습자 유형 */
enum class Learner(val label: String, val desc: String, val emoji: String) {
    KIDS("어린이", "그림과 소리로 쉬운 단어부터", "🧒"),
    STARTER("처음 배워요", "알파벳 소리와 인사말부터", "🌱"),
    BASIC("조금 해 봤어요", "짧은 문장은 알아요", "🌿"),
    INTERMEDIATE("대화는 돼요", "더 자연스럽게 말하고 싶어요", "🌳"),
    ADVANCED("자신 있어요", "뉴스·토론·업무 영어", "🏆"),
}

data class AppSettings(
    val defaultProvider: Provider = Provider.OPENROUTER,
    val keys: Map<Provider, String> = emptyMap(),
    val models: Map<Provider, String> = emptyMap(),
    val ttsRate: Float = 1.0f,
    val showKorean: Boolean = true,
    val autoSpeak: Boolean = true,
    val tutorLevel: TutorLevel = TutorLevel.AUTO,
    val dailyGoal: Int = 20,
    val nickname: String = "",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    // v0.3~
    val onboarded: Boolean = false,
    val learner: Learner = Learner.BASIC,
    val goal: String = "",
    val fontScale: Float = 1.0f,
    val showSuggestions: Boolean = true,
    val silentMode: Boolean = false,
    val dailyAiLimit: Int = 0, // 0 = 제한 없음
    val ttsVoice: String = "",
) {
    fun key(p: Provider) = keys[p].orEmpty()
    fun model(p: Provider) = models[p]?.takeIf { it.isNotBlank() } ?: p.defaultModel
    fun hasKey(p: Provider) = key(p).isNotBlank()
    val connectedProviders: List<Provider> get() = Provider.entries.filter { hasKey(it) }
    val anyConnected: Boolean get() = connectedProviders.isNotEmpty()
    /** 왕초보·어린이처럼 도움이 많이 필요한 학습자 */
    val isBeginner: Boolean get() = learner == Learner.KIDS || learner == Learner.STARTER || learner == Learner.BASIC
}

data class Usage(val todayCalls: Int, val todayTokens: Int, val monthCalls: Int, val monthTokens: Int)

/**
 * 설정 저장소.
 * - API 키는 모든 가족 프로필이 함께 쓰며 Android Keystore 기반으로 암호화 저장(백업 파일에도 들어가지 않음)
 * - 나머지 설정과 AI 사용량은 프로필별로 저장
 */
class SettingsStore(context: Context, val profileId: String = "default") {

    private val secure: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context, "secure_settings", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // 키스토어 손상 등 예외 상황: 일반 prefs로 대체 (앱이 죽지 않도록)
        context.getSharedPreferences("settings_fallback", Context.MODE_PRIVATE)
    }

    /** 기본 프로필은 v0.2까지의 저장 위치를 그대로 쓴다 */
    private val prefs: SharedPreferences =
        if (profileId == "default") secure else context.getSharedPreferences("profile_$profileId", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state
    val current: AppSettings get() = _state.value

    private inline fun <reified E : Enum<E>> enumOf(name: String?, default: E): E =
        runCatching { enumValueOf<E>(name ?: default.name) }.getOrDefault(default)

    private fun load(): AppSettings {
        val p = prefs
        // v0.2 사용자: 이미 키를 넣었거나 이름을 정했다면 첫 실행 안내를 건너뛴다
        val legacyUser = profileId == "default" && (p.contains("provider") || p.contains("nickname"))
        return AppSettings(
            defaultProvider = enumOf(p.getString("provider", null), Provider.OPENROUTER),
            keys = Provider.entries.associateWith { secure.getString("key_${it.name}", "") ?: "" },
            models = Provider.entries.associateWith { p.getString("model_${it.name}", "") ?: "" },
            ttsRate = p.getFloat("tts_rate", 1.0f),
            showKorean = p.getBoolean("show_ko", true),
            autoSpeak = p.getBoolean("auto_speak", true),
            tutorLevel = enumOf(p.getString("tutor_level", null), TutorLevel.AUTO),
            dailyGoal = p.getInt("daily_goal", 20),
            nickname = p.getString("nickname", "") ?: "",
            reminderEnabled = p.getBoolean("reminder_on", false),
            reminderHour = p.getInt("reminder_hour", 20),
            reminderMinute = p.getInt("reminder_minute", 0),
            onboarded = p.getBoolean("onboarded", legacyUser),
            learner = enumOf(p.getString("learner", null), Learner.BASIC),
            goal = p.getString("goal", "") ?: "",
            fontScale = p.getFloat("font_scale", 1.0f),
            showSuggestions = p.getBoolean("show_suggestions", true),
            silentMode = p.getBoolean("silent_mode", false),
            dailyAiLimit = p.getInt("daily_ai_limit", 0),
            ttsVoice = p.getString("tts_voice", "") ?: "",
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val s = transform(_state.value)
        secure.edit().apply {
            Provider.entries.forEach { putString("key_${it.name}", s.keys[it].orEmpty().trim()) }
        }.apply()
        prefs.edit().apply {
            putString("provider", s.defaultProvider.name)
            Provider.entries.forEach { putString("model_${it.name}", s.models[it].orEmpty().trim()) }
            putFloat("tts_rate", s.ttsRate)
            putBoolean("show_ko", s.showKorean)
            putBoolean("auto_speak", s.autoSpeak)
            putString("tutor_level", s.tutorLevel.name)
            putInt("daily_goal", s.dailyGoal)
            putString("nickname", s.nickname)
            putBoolean("reminder_on", s.reminderEnabled)
            putInt("reminder_hour", s.reminderHour)
            putInt("reminder_minute", s.reminderMinute)
            putBoolean("onboarded", s.onboarded)
            putString("learner", s.learner.name)
            putString("goal", s.goal)
            putFloat("font_scale", s.fontScale)
            putBoolean("show_suggestions", s.showSuggestions)
            putBoolean("silent_mode", s.silentMode)
            putInt("daily_ai_limit", s.dailyAiLimit)
            putString("tts_voice", s.ttsVoice)
        }.apply()
        _state.value = s
    }

    // ---------- AI 사용량 (프로필별) ----------

    private val _usage = MutableStateFlow(readUsage())
    val usage: StateFlow<Usage> = _usage

    private fun readUsage(): Usage {
        val today = LocalDate.now()
        val sameDay = prefs.getString("usage_day", "") == today.toString()
        val sameMonth = prefs.getString("usage_month", "") == today.toString().take(7)
        return Usage(
            todayCalls = if (sameDay) prefs.getInt("usage_day_calls", 0) else 0,
            todayTokens = if (sameDay) prefs.getInt("usage_day_tokens", 0) else 0,
            monthCalls = if (sameMonth) prefs.getInt("usage_month_calls", 0) else 0,
            monthTokens = if (sameMonth) prefs.getInt("usage_month_tokens", 0) else 0,
        )
    }

    /** 오늘 한도를 넘었는지 (넘었으면 true) */
    fun overDailyLimit(): Boolean {
        val limit = current.dailyAiLimit
        return limit > 0 && readUsage().todayCalls >= limit
    }

    @Synchronized
    fun recordUsage(tokens: Int) {
        val u = readUsage()
        val today = LocalDate.now().toString()
        val n = Usage(u.todayCalls + 1, u.todayTokens + tokens, u.monthCalls + 1, u.monthTokens + tokens)
        prefs.edit()
            .putString("usage_day", today).putString("usage_month", today.take(7))
            .putInt("usage_day_calls", n.todayCalls).putInt("usage_day_tokens", n.todayTokens)
            .putInt("usage_month_calls", n.monthCalls).putInt("usage_month_tokens", n.monthTokens)
            .apply()
        _usage.value = n
    }

    fun refreshUsage() { _usage.value = readUsage() }

    // ---------- 오늘의 공부 진행 표시 (프로필별, 날짜별) ----------

    private val _today = MutableStateFlow(readToday())
    val todayDone: StateFlow<Set<String>> = _today

    private fun readToday(): Set<String> =
        if (prefs.getString("today_date", "") == LocalDate.now().toString())
            prefs.getString("today_steps", "").orEmpty().split(",").filter { it.isNotBlank() }.toSet()
        else emptySet()

    fun markToday(step: String) {
        val set = readToday() + step
        prefs.edit().putString("today_date", LocalDate.now().toString()).putString("today_steps", set.joinToString(",")).apply()
        _today.value = set
    }

    fun refreshToday() { _today.value = readToday() }
}

/** AI에게 알려줄 학습자 수준 설명 */
fun AppSettings.levelPrompt(usedWords: Int): String = when (tutorLevel) {
    TutorLevel.KIDS -> "a young child (6-10 years old): very simple words, very short sentences, lots of praise"
    TutorLevel.BEGINNER -> "beginner (A1-A2): short simple sentences, everyday words"
    TutorLevel.INTERMEDIATE -> "intermediate (B1-B2): natural everyday English"
    TutorLevel.ADVANCED -> "advanced (C1): fully natural, nuanced English with idioms"
    TutorLevel.AUTO -> when (learner) {
        Learner.KIDS -> "a young child (6-10 years old): very simple words, very short sentences, lots of praise"
        Learner.STARTER -> if (usedWords < 300) "absolute beginner (Pre-A1): 3-6 word sentences, only the most common words, speak slowly"
            else com.jwahn.englishspeaking.domain.Levels.of(usedWords).prompt
        Learner.ADVANCED -> "advanced (C1): fully natural, nuanced English"
        else -> com.jwahn.englishspeaking.domain.Levels.of(usedWords).prompt
    }
}

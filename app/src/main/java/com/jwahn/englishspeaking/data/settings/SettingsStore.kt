package com.jwahn.englishspeaking.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Provider(val label: String, val defaultModel: String, val keyHint: String) {
    CLAUDE("Claude (Anthropic)", "claude-sonnet-5-5", "sk-ant-..."),
    OPENAI("ChatGPT (OpenAI)", "gpt-4o-mini", "sk-..."),
    GEMINI("Gemini (Google)", "gemini-2.5-flash", "AIza..."),
}

/** 튜터 난이도. AUTO = 사용 어휘 수로 자동 결정 */
enum class TutorLevel(val label: String) {
    AUTO("자동(레벨 기준)"), KIDS("어린이"), BEGINNER("초급"), INTERMEDIATE("중급"), ADVANCED("고급")
}

data class AppSettings(
    val defaultProvider: Provider = Provider.CLAUDE,
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
) {
    fun key(p: Provider) = keys[p].orEmpty()
    fun model(p: Provider) = models[p]?.takeIf { it.isNotBlank() } ?: p.defaultModel
    fun hasKey(p: Provider) = key(p).isNotBlank()
    val connectedProviders: List<Provider> get() = Provider.entries.filter { hasKey(it) }
    val anyConnected: Boolean get() = connectedProviders.isNotEmpty()
}

/**
 * API 키는 Android Keystore 기반 EncryptedSharedPreferences에 암호화 저장.
 * 키는 백업(JSON 내보내기)에 포함되지 않는다.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = try {
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

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state
    val current: AppSettings get() = _state.value

    private fun load(): AppSettings = AppSettings(
        defaultProvider = runCatching { Provider.valueOf(prefs.getString("provider", null) ?: "CLAUDE") }
            .getOrDefault(Provider.CLAUDE),
        keys = Provider.entries.associateWith { prefs.getString("key_${it.name}", "") ?: "" },
        models = Provider.entries.associateWith { prefs.getString("model_${it.name}", "") ?: "" },
        ttsRate = prefs.getFloat("tts_rate", 1.0f),
        showKorean = prefs.getBoolean("show_ko", true),
        autoSpeak = prefs.getBoolean("auto_speak", true),
        tutorLevel = runCatching { TutorLevel.valueOf(prefs.getString("tutor_level", null) ?: "AUTO") }
            .getOrDefault(TutorLevel.AUTO),
        dailyGoal = prefs.getInt("daily_goal", 20),
        nickname = prefs.getString("nickname", "") ?: "",
        reminderEnabled = prefs.getBoolean("reminder_on", false),
        reminderHour = prefs.getInt("reminder_hour", 20),
        reminderMinute = prefs.getInt("reminder_minute", 0),
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val s = transform(_state.value)
        prefs.edit().apply {
            putString("provider", s.defaultProvider.name)
            Provider.entries.forEach {
                putString("key_${it.name}", s.keys[it].orEmpty().trim())
                putString("model_${it.name}", s.models[it].orEmpty().trim())
            }
            putFloat("tts_rate", s.ttsRate)
            putBoolean("show_ko", s.showKorean)
            putBoolean("auto_speak", s.autoSpeak)
            putString("tutor_level", s.tutorLevel.name)
            putInt("daily_goal", s.dailyGoal)
            putString("nickname", s.nickname)
            putBoolean("reminder_on", s.reminderEnabled)
            putInt("reminder_hour", s.reminderHour)
            putInt("reminder_minute", s.reminderMinute)
        }.apply()
        _state.value = s
    }
}

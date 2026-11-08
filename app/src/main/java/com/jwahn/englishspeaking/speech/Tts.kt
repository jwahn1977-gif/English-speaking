package com.jwahn.englishspeaking.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID

/** 기기 내장 TTS 래퍼. 콜백은 메인 스레드로 전달된다. */
class Tts(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val callbacks = mutableMapOf<String, () -> Unit>()
    private val pending = mutableListOf<() -> Unit>()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    var rate: Float = 1.0f

    /** 사용자가 고른 목소리 이름 (비어 있으면 기기 기본 영어 목소리) */
    var voiceName: String = ""
        set(v) { field = v; main.post { applyVoice() } }

    data class VoiceOption(val name: String, val label: String)

    /** 기기에 있는 영어 목소리 목록 (품질 높은 순) */
    fun englishVoices(): List<VoiceOption> = runCatching {
        tts?.voices.orEmpty()
            .filter { it.locale.language == "en" && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }.thenBy { it.isNetworkConnectionRequired }.thenBy { it.name })
            .map { v ->
                val country = when (v.locale.country) { "US" -> "미국"; "GB" -> "영국"; "AU" -> "호주"; "IN" -> "인도"; "CA" -> "캐나다"; else -> v.locale.country }
                val quality = if (v.quality >= android.speech.tts.Voice.QUALITY_HIGH) "고음질" else "보통"
                VoiceOption(v.name, "$country · $quality" + (if (v.isNetworkConnectionRequired) " · 인터넷 필요" else "") + " · ${v.name.takeLast(12)}")
            }
    }.getOrDefault(emptyList())

    private fun applyVoice() {
        val t = tts ?: return
        if (!_ready.value) return
        val v = if (voiceName.isBlank()) bestVoice(t) else runCatching { t.voices?.firstOrNull { it.name == voiceName } }.getOrNull()
        if (v != null) t.voice = v else t.language = Locale.US
    }

    /** 따로 고르지 않았으면 기기에 있는 미국 영어 목소리 중 가장 자연스러운 것 */
    private fun bestVoice(t: TextToSpeech): android.speech.tts.Voice? = runCatching {
        t.voices.orEmpty()
            .filter { it.locale.language == "en" && it.locale.country == "US" && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .sortedWith(compareByDescending<android.speech.tts.Voice> { it.quality }.thenBy { it.isNetworkConnectionRequired }.thenBy { it.latency })
            .firstOrNull()
    }.getOrNull()

    /** AI 고음질 음성 (설정에서 켰고 키가 있을 때) */
    var cloud: CloudVoice? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var cloudJob: Job? = null
    private var player: MediaPlayer? = null

    /** 마지막 고음질 음성 오류 (설정 화면 안내용) */
    private val _cloudError = MutableStateFlow<String?>(null)
    val cloudError: StateFlow<String?> = _cloudError

    private fun playCloud(text: String, speed: Float, id: String, onDone: (() -> Unit)?, fallback: () -> Unit) {
        val engine = cloud ?: return fallback()
        cloudJob?.cancel()
        cloudJob = scope.launch {
            val file = runCatching { engine.fetch(text) }
                .onFailure { _cloudError.value = it.message }
                .getOrNull()
            if (currentId != id) return@launch
            if (file == null) { fallback(); return@launch }
            _cloudError.value = null
            releasePlayer()
            runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                    )
                    setDataSource(file.path)
                    prepare()
                    runCatching { playbackParams = PlaybackParams().setSpeed(speed.coerceIn(0.5f, 2f)).setPitch(1f) }
                    setOnCompletionListener { finish(id) }
                    setOnErrorListener { _, _, _ -> finish(id); true }
                    if (onDone != null) callbacks[id] = onDone
                    _speaking.value = true
                    start()
                    player = this
                }
            }.onFailure { _cloudError.value = "재생 실패: ${it.message}"; fallback() }
        }
    }

    private fun releasePlayer() {
        player?.let { p -> runCatching { if (p.isPlaying) p.stop() }; p.release() }
        player = null
    }

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val t = tts ?: return@TextToSpeech
                val r = t.setLanguage(Locale.US)
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) t.setLanguage(Locale.ENGLISH)
                t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) { _speaking.value = true }
                    override fun onDone(id: String?) = finish(id)
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) = finish(id)
                    override fun onError(id: String?, errorCode: Int) = finish(id)
                    override fun onStop(id: String?, interrupted: Boolean) { _speaking.value = false }
                })
                main.post {
                    _ready.value = true
                    applyVoice()
                    pending.forEach { it() }
                    pending.clear()
                }
            }
        }
    }

    private fun finish(id: String?) {
        main.post {
            _speaking.value = false
            id?.let { callbacks.remove(it)?.invoke() }
        }
    }

    @Volatile
    private var currentId: String? = null

    /** 뉴스 플레이어 외의 곳에서 읽기를 시작할 때 호출 (뉴스 재생을 멈추기 위해) */
    var onUserSpeak: (() -> Unit)? = null

    /** 영어 문장 읽기. onDone은 끝까지 읽었을 때만 호출. 반환값: 발화 id */
    fun speak(text: String, rateOverride: Float? = null, onDone: (() -> Unit)? = null): String =
        speakInternal(text, rateOverride, onDone, fromPlayer = false)

    private fun speakInternal(text: String, rateOverride: Float?, onDone: (() -> Unit)?, fromPlayer: Boolean): String {
        if (!fromPlayer) onUserSpeak?.invoke()
        val id = UUID.randomUUID().toString()
        currentId = id
        tts?.stop()
        releasePlayer()
        val deviceSpeak = {
            val t = tts
            if (t != null && currentId == id) {
                callbacks.clear()
                if (onDone != null) callbacks[id] = onDone
                t.setSpeechRate(rateOverride ?: rate)
                t.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            }
        }
        val action = deviceSpeak
        if (cloud?.enabled() == true) {
            callbacks.clear()
            playCloud(text, rateOverride ?: rate, id, onDone) { if (_ready.value) action() else pending += action }
        } else if (_ready.value) action() else pending += action
        return id
    }

    /** 끝까지 읽을 때까지 기다린다. 코루틴이 취소되면 (그 문장을 읽는 중일 때만) 읽기를 멈춘다. */
    suspend fun speakAndWait(text: String, rateOverride: Float? = null) {
        suspendCancellableCoroutine { cont ->
            val id = speakInternal(text, rateOverride, { if (cont.isActive) cont.resumeWith(Result.success(Unit)) }, fromPlayer = true)
            cont.invokeOnCancellation { main.post { if (currentId == id) stop() } }
        }
    }

    fun stop() {
        currentId = null
        callbacks.clear()
        cloudJob?.cancel()
        releasePlayer()
        tts?.stop()
        _speaking.value = false
    }

    fun shutdown() {
        releasePlayer()
        scope.cancel()
        tts?.shutdown()
        tts = null
    }
}

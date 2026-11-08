package com.jwahn.englishspeaking.speech

import android.content.Context
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
        val action = {
            val t = tts
            if (t != null && currentId == id) {
                callbacks.clear()
                if (onDone != null) callbacks[id] = onDone
                t.setSpeechRate(rateOverride ?: rate)
                t.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            }
        }
        if (_ready.value) action() else pending += action
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
        tts?.stop()
        _speaking.value = false
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}

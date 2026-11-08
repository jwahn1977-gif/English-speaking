package com.jwahn.englishspeaking.news

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 문장을 끝까지 읽고 돌아오는 음성 출력 (실제로는 TTS, 테스트에서는 가짜) */
fun interface Speaker {
    suspend fun speak(text: String, rate: Float)
}

data class PlayerState(
    val lessonId: Long = 0,
    val title: String = "",
    val sentences: List<String> = emptyList(),
    val index: Int = 0,
    val playing: Boolean = false,
    val rate: Float = 1.0f,
    val repeat: Int = 1,
) {
    val active: Boolean get() = lessonId != 0L && sentences.isNotEmpty()
    val current: String get() = sentences.getOrElse(index) { "" }
}

/**
 * 뉴스 듣기 플레이어. 화면과 분리되어 앱 전체에 하나만 존재하므로
 * 화면을 나가거나 폰 화면이 꺼져도(포그라운드 서비스와 함께) 계속 재생된다.
 */
class NewsPlayer(
    private val scope: CoroutineScope,
    private val speaker: Speaker,
    private val stopSpeech: () -> Unit,
    private val onSentenceDone: suspend (lessonId: Long, index: Int) -> Unit = { _, _ -> },
    private val gapMs: Long = 400,
    private val repeatGapMs: Long = 700,
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    /** 재생 시작/정지 때 서비스를 켜고 끄기 위한 콜백 */
    var onPlayingChanged: (Boolean) -> Unit = {}

    private var job: Job? = null

    fun load(lessonId: Long, title: String, sentences: List<String>, startIndex: Int, rate: Float) {
        if (_state.value.lessonId == lessonId && _state.value.sentences == sentences) return
        pause()
        _state.value = PlayerState(
            lessonId = lessonId, title = title, sentences = sentences,
            index = startIndex.coerceIn(0, (sentences.size - 1).coerceAtLeast(0)), rate = rate,
            repeat = _state.value.repeat,
        )
    }

    fun play() {
        val s = _state.value
        if (!s.active || job?.isActive == true) return
        setPlaying(true)
        job = scope.launch {
            while (true) {
                val st = _state.value
                val text = st.current
                repeat(st.repeat) { r ->
                    speaker.speak(text, st.rate)
                    if (r < st.repeat - 1) delay(repeatGapMs)
                }
                onSentenceDone(st.lessonId, st.index)
                delay(gapMs)
                val now = _state.value
                if (now.index >= now.sentences.lastIndex) {
                    _state.update { it.copy(playing = false) }
                    onPlayingChanged(false)
                    break
                }
                _state.update { it.copy(index = it.index + 1) }
            }
        }
    }

    fun pause() {
        job?.cancel()
        job = null
        stopSpeech()
        if (_state.value.playing) setPlaying(false)
    }

    fun toggle() = if (_state.value.playing) pause() else play()

    /** 특정 문장으로 이동. 재생 중이었다면 그 문장부터 다시 읽는다 */
    fun seek(index: Int, autoplay: Boolean = _state.value.playing) {
        val s = _state.value
        if (!s.active) return
        val wasPlaying = job?.isActive == true
        job?.cancel(); job = null
        stopSpeech()
        _state.update { it.copy(index = index.coerceIn(0, it.sentences.lastIndex)) }
        if (autoplay) play() else if (wasPlaying) setPlaying(false)
    }

    fun next() = seek(_state.value.index + 1)
    fun prev() = seek(_state.value.index - 1)

    fun setRate(rate: Float) = _state.update { it.copy(rate = rate) }
    fun setRepeat(n: Int) = _state.update { it.copy(repeat = n.coerceIn(1, 5)) }

    /** 완전히 종료 (알림의 '닫기') */
    fun close() {
        pause()
        _state.value = PlayerState(repeat = _state.value.repeat)
    }

    private fun setPlaying(p: Boolean) {
        _state.update { it.copy(playing = p) }
        onPlayingChanged(p)
    }
}

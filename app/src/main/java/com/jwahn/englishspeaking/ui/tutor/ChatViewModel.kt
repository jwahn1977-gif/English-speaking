package com.jwahn.englishspeaking.ui.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jwahn.englishspeaking.AppContainer
import com.jwahn.englishspeaking.data.PatternDraft
import com.jwahn.englishspeaking.data.WordDraft
import com.jwahn.englishspeaking.data.db.ConversationEntity
import com.jwahn.englishspeaking.data.db.MessageEntity
import com.jwahn.englishspeaking.data.db.ScenarioEntity
import com.jwahn.englishspeaking.data.llm.ChatMsg
import com.jwahn.englishspeaking.data.llm.Prompts
import com.jwahn.englishspeaking.data.settings.TutorLevel
import com.jwahn.englishspeaking.domain.JsonText
import com.jwahn.englishspeaking.domain.Levels
import com.jwahn.englishspeaking.domain.arr
import com.jwahn.englishspeaking.domain.ints
import com.jwahn.englishspeaking.domain.obj
import com.jwahn.englishspeaking.domain.objs
import com.jwahn.englishspeaking.domain.str
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class Correction(val original: String, val better: String, val why: String)

data class ChatItem(
    val id: Long,
    val role: String,
    val text: String,
    val ko: String = "",
    val correction: Correction? = null,
    val patterns: List<PatternDraft> = emptyList(),
    val words: List<WordDraft> = emptyList(),
)

data class HintOption(val en: String, val note: String)

data class Report(
    val summary: String,
    val good: List<String>,
    val improvements: List<Correction>,
    val patterns: List<PatternDraft>,
    val words: List<WordDraft>,
    val score: Int,
)

data class ChatState(
    val scenario: ScenarioEntity? = null,
    val items: List<ChatItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val missionsDone: Set<Int> = emptySet(),
    val voiceMode: Boolean = false,
    val hintLoading: Boolean = false,
    val hints: List<HintOption>? = null,
    val reportLoading: Boolean = false,
    val report: Report? = null,
)

class ChatViewModel(private val c: AppContainer, private val scenarioId: Long, private val resumeId: Long) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    /** 음성 모드에서 AI가 말을 끝내면 마이크를 자동으로 켜기 위한 신호 */
    private val _listen = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val listen: SharedFlow<Unit> = _listen

    private var conversationId = 0L
    private var lastFailed: String? = null
    private var tempId = -1L

    init {
        viewModelScope.launch {
            val s = c.db.tutor().scenario(scenarioId)
            _state.update { it.copy(scenario = s) }
            if (s == null) { _state.update { it.copy(error = "시나리오를 찾을 수 없어요.") }; return@launch }
            if (resumeId > 0 && c.db.tutor().conversation(resumeId) != null) {
                conversationId = resumeId
                val items = c.db.tutor().messages(resumeId).map { it.toItem() }
                _state.update { it.copy(items = items) }
            } else {
                conversationId = c.db.tutor().insertConversation(ConversationEntity(scenarioId = s.id, title = "${s.emoji} ${s.title}"))
                requestReply()
            }
        }
    }

    private fun MessageEntity.toItem(): ChatItem {
        if (role == "user") return ChatItem(id, role, text)
        val o = extrasJson?.let { JsonText.parseObject(it) }
        return if (o != null) parseReply(o, id) else ChatItem(id, role, text, ko)
    }

    private fun parseReply(o: JsonObject, id: Long): ChatItem {
        val corr = o.obj("correction")?.let { Correction(it.str("original"), it.str("better"), it.str("why_ko")) }
            ?.takeIf { it.better.isNotBlank() && !it.better.equals(it.original, ignoreCase = true) }
        return ChatItem(
            id = id, role = "assistant",
            text = o.str("reply").ifBlank { "…" }, ko = o.str("reply_ko"),
            correction = corr,
            patterns = o.objs("patterns").mapNotNull { PatternDraft.from(it, "AI 튜터") },
            words = o.objs("words").mapNotNull { WordDraft.from(it) },
        )
    }

    private suspend fun levelPrompt(): String = when (c.settings.current.tutorLevel) {
        TutorLevel.AUTO -> Levels.of(c.db.tutor().usedWordCountOnce()).prompt
        TutorLevel.KIDS -> "a young child (6-10 years old): very simple words, very short sentences, lots of praise"
        TutorLevel.BEGINNER -> "beginner (A2): short simple sentences, everyday words"
        TutorLevel.INTERMEDIATE -> "intermediate (B1-B2): natural everyday English"
        TutorLevel.ADVANCED -> "advanced (C1): fully natural, nuanced English with idioms"
    }

    private fun history(): List<ChatMsg> {
        val msgs = mutableListOf(ChatMsg("user", Prompts.TUTOR_OPENING))
        _state.value.items.forEach { msgs += ChatMsg(it.role, it.text) }
        return msgs
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || _state.value.loading) return
        c.tts.stop()
        viewModelScope.launch {
            val id = c.db.tutor().insertMessage(MessageEntity(conversationId = conversationId, role = "user", text = t))
            _state.update { it.copy(items = it.items + ChatItem(id, "user", t), error = null, hints = null) }
            c.repo.addUsedWords(t)
            c.repo.recordActivity(sentences = 1)
            requestReply()
        }
    }

    fun retry() {
        _state.update { it.copy(error = null) }
        requestReply()
    }

    private fun requestReply() {
        val scenario = _state.value.scenario ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val system = Prompts.tutorSystem(scenario, levelPrompt(), c.settings.current.nickname)
                c.llm.completeJson(system, history(), maxTokens = 1500)
            }.onSuccess { o ->
                val tmp = parseReply(o, tempId--)
                val id = c.db.tutor().insertMessage(
                    MessageEntity(conversationId = conversationId, role = "assistant", text = tmp.text, ko = tmp.ko, extrasJson = o.toString())
                )
                val done = o.ints("missions_done").toSet()
                _state.update { it.copy(items = it.items + tmp.copy(id = id), loading = false, missionsDone = it.missionsDone + done) }
                val st = _state.value
                if (st.voiceMode || c.settings.current.autoSpeak) {
                    c.tts.speak(tmp.text) { if (_state.value.voiceMode) _listen.tryEmit(Unit) }
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "오류") }
            }
        }
    }

    fun toggleVoice() {
        val v = !_state.value.voiceMode
        _state.update { it.copy(voiceMode = v) }
        if (!v) c.tts.stop()
    }

    fun askHint(korean: String) {
        if (korean.isBlank()) return
        _state.update { it.copy(hintLoading = true, hints = null, error = null) }
        viewModelScope.launch {
            val context = _state.value.items.takeLast(4).joinToString("\n") {
                (if (it.role == "user") "Learner: " else "Partner: ") + it.text
            }
            runCatching { c.llm.quickJson(Prompts.hint(korean, context), 800) }
                .onSuccess { o ->
                    _state.update { it.copy(hintLoading = false, hints = o.objs("options").map { h -> HintOption(h.str("en"), h.str("note_ko")) }) }
                }
                .onFailure { e -> _state.update { it.copy(hintLoading = false, error = e.message) } }
        }
    }

    fun clearHints() = _state.update { it.copy(hints = null) }

    fun finish() {
        val st = _state.value
        val s = st.scenario ?: return
        if (st.items.none { it.role == "user" }) {
            _state.update { it.copy(error = "한 마디 이상 말한 뒤에 리포트를 받을 수 있어요.") }
            return
        }
        c.tts.stop()
        _state.update { it.copy(reportLoading = true, error = null) }
        viewModelScope.launch {
            val transcript = st.items.joinToString("\n") { (if (it.role == "user") "Learner: " else "Partner: ") + it.text }
            runCatching { c.llm.quickJson(Prompts.report(transcript, s.missions), 3000) }
                .onSuccess { o ->
                    val r = Report(
                        summary = o.str("summary_ko"),
                        good = o.arr("good_ko").mapNotNull { (it as? JsonPrimitive)?.contentOrNull },
                        improvements = o.objs("improvements").map { Correction(it.str("original"), it.str("better"), it.str("why_ko")) },
                        patterns = o.objs("patterns").mapNotNull { PatternDraft.from(it, s.title) },
                        words = o.objs("words").mapNotNull { WordDraft.from(it) },
                        score = o.str("score").toIntOrNull() ?: 0,
                    )
                    c.db.tutor().conversation(conversationId)?.let { conv -> c.db.tutor().updateConversation(conv.copy(reportJson = o.toString())) }
                    _state.update { it.copy(reportLoading = false, report = r) }
                }
                .onFailure { e -> _state.update { it.copy(reportLoading = false, error = e.message) } }
        }
    }

    fun closeReport() = _state.update { it.copy(report = null) }

    override fun onCleared() {
        c.tts.stop()
        // 한 마디도 안 한 빈 대화는 지운다
        val c2 = c
        val id = conversationId
        if (_state.value.items.none { it.role == "user" } && id > 0) {
            c2.scope.launch { c2.db.tutor().deleteMessages(id); c2.db.tutor().deleteConversation(id) }
        }
    }
}

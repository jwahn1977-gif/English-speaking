package com.jwahn.englishspeaking.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jwahn.englishspeaking.data.PatternDraft
import com.jwahn.englishspeaking.data.WordDraft
import com.jwahn.englishspeaking.data.db.NewsLessonEntity
import com.jwahn.englishspeaking.domain.JsonText
import com.jwahn.englishspeaking.domain.objs
import com.jwahn.englishspeaking.domain.str
import com.jwahn.englishspeaking.speech.rememberSpeechInput
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.EmptyState
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.PatternSaveCard
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SpeakIcon
import com.jwahn.englishspeaking.ui.common.WordSaveCard
import com.jwahn.englishspeaking.ui.vocab.SpokenResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val NEWS_CHAPTER = "뉴스에서 저장한 단어"

private data class Sent(val en: String, val ko: String)
private data class QA(val q: String, val a: String)

private data class Lesson(
    val entity: NewsLessonEntity,
    val title: String,
    val titleKo: String,
    val summaryKo: String,
    val sentences: List<Sent>,
    val words: List<WordDraft>,
    val patterns: List<PatternDraft>,
    val questions: List<QA>,
)

private fun parse(e: NewsLessonEntity): Lesson? {
    val o = JsonText.parseObject(e.json) ?: return null
    return Lesson(
        e, o.str("title").ifBlank { e.title }, o.str("title_ko"), o.str("summary_ko"),
        o.objs("sentences").map { Sent(it.str("en"), it.str("ko")) }.filter { it.en.isNotBlank() },
        o.objs("words").mapNotNull { WordDraft.from(it) },
        o.objs("patterns").mapNotNull { PatternDraft.from(it, "뉴스") },
        o.objs("questions").map { QA(it.str("q"), it.str("a")) }.filter { it.q.isNotBlank() },
    )
}

@Composable
fun NewsLessonScreen(nav: Nav, id: Long) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val settings by c.settings.state.collectAsState()
    val lesson by produceState<Lesson?>(null, id) { value = c.db.news().lesson(id)?.let { parse(it) } }

    Screen("뉴스 듣기", onBack = nav::back) { mod ->
        val l = lesson ?: return@Screen
        if (l.sentences.isEmpty()) {
            EmptyState("😅", "문장이 없어요", "다른 기사를 골라 주세요.", mod); return@Screen
        }
        var index by remember { mutableIntStateOf(l.entity.lastPosition.coerceIn(0, l.sentences.lastIndex)) }
        var playing by remember { mutableStateOf(false) }
        var rate by remember { mutableFloatStateOf(settings.ttsRate) }
        var repeatCount by remember { mutableIntStateOf(1) }
        var showEn by remember { mutableStateOf(true) }
        var showKo by remember { mutableStateOf(settings.showKorean) }
        var tab by remember { mutableIntStateOf(0) }
        var spoken by remember { mutableStateOf<Pair<Int, String>?>(null) }
        var shadowIdx by remember { mutableIntStateOf(-1) }
        val speech = rememberSpeechInput { s -> spoken = shadowIdx to s; scope.launch { c.repo.recordActivity(sentences = 1) } }
        val listState = rememberLazyListState()

        // 재생 중에는 화면 꺼짐 방지
        val view = LocalView.current
        DisposableEffect(playing) {
            view.keepScreenOn = playing
            onDispose { view.keepScreenOn = false }
        }
        DisposableEffect(Unit) { onDispose { c.tts.stop() } }

        // 문장 단위 재생 루프: 한 문장을 repeat번 읽고 다음 문장으로
        LaunchedEffect(playing, index) {
            c.db.news().updateLessonPosition(id, index)
            if (tab == 0) listState.animateScrollToItem(index + 1)
            if (!playing) return@LaunchedEffect
            repeat(repeatCount) { r ->
                c.tts.speakAndWait(l.sentences[index].en, rate)
                if (r < repeatCount - 1) delay(700)
            }
            c.repo.recordActivity(listened = 1)
            delay(400)
            if (index < l.sentences.lastIndex) index++ else playing = false
        }

        Column(mod) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("문장 ${l.sentences.size}") })
                Tab(tab == 1, { tab = 1 }, text = { Text("단어 ${l.words.size}") })
                Tab(tab == 2, { tab = 2 }, text = { Text("패턴 ${l.patterns.size}") })
                Tab(tab == 3, { tab = 3 }, text = { Text("퀴즈 ${l.questions.size}") })
            }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = listState) {
                when (tab) {
                    0 -> {
                        item {
                            Column(Modifier.padding(vertical = 8.dp)) {
                                Text(l.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (l.titleKo.isNotBlank()) Text(l.titleKo, style = MaterialTheme.typography.bodyMedium)
                                if (l.summaryKo.isNotBlank()) Text(l.summaryKo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        itemsIndexed(l.sentences) { i, s ->
                            val current = i == index
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    .background(
                                        if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(10.dp),
                                    )
                                    .clickable { index = i; playing = true }
                                    .padding(10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp, top = 2.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            if (showEn || current && !playing) s.en else "🎧 · · ·",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                        if (showKo && s.ko.isNotBlank()) Text(s.ko, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { playing = false; shadowIdx = i; spoken = null; speech("en-US", s.en) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Mic, "따라 말하기")
                                    }
                                }
                                spoken?.takeIf { it.first == i }?.let { SpokenResult(s.en, it.second) }
                            }
                        }
                    }
                    1 -> {
                        if (l.words.isEmpty()) item { Text("AI를 연결하면 핵심 단어를 뽑아 줘요.", Modifier.padding(16.dp)) }
                        l.words.forEach { w -> item { WordSaveCard(w, NEWS_CHAPTER, "news", Modifier.padding(vertical = 4.dp)) } }
                    }
                    2 -> {
                        if (l.patterns.isEmpty()) item { Text("AI를 연결하면 기사 속 패턴을 뽑아 줘요.", Modifier.padding(16.dp)) }
                        l.patterns.forEach { p -> item { PatternSaveCard(p, "news", Modifier.padding(vertical = 4.dp)) } }
                    }
                    else -> {
                        if (l.questions.isEmpty()) item { Text("AI를 연결하면 이해도 확인 질문을 만들어 줘요.", Modifier.padding(16.dp)) }
                        l.questions.forEach { qa -> item { QuestionCard(qa) } }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            // 플레이어
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    LinearProgressIndicator(progress = { (index + 1) / l.sentences.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (index > 0) index-- }) { Icon(Icons.Default.SkipPrevious, "이전 문장") }
                        FilledIconButton(onClick = { if (playing) { playing = false; c.tts.stop() } else playing = true }, modifier = Modifier.size(56.dp)) {
                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "일시정지" else "재생", Modifier.size(32.dp))
                        }
                        IconButton(onClick = { if (index < l.sentences.lastIndex) index++ }) { Icon(Icons.Default.SkipNext, "다음 문장") }
                        SpeakIcon(l.sentences[index].en, rate = 0.7f)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.7f, 0.85f, 1.0f, 1.2f).forEach { r -> FilterChip(rate == r, { rate = r }, { Text("${r}x") }) }
                        listOf(1, 2, 3).forEach { n -> FilterChip(repeatCount == n, { repeatCount = n }, { Text("${n}회 반복") }) }
                        FilterChip(showEn, { showEn = !showEn }, { Text("영어 자막") })
                        FilterChip(showKo, { showKo = !showKo }, { Text("한국어") })
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(qa: QA) {
    var open by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { open = !open },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Q. ${qa.q}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                SpeakIcon(qa.q)
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Text(if (open) "A. ${qa.a}" else "눌러서 정답 보기", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

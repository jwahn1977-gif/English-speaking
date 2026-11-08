package com.jwahn.englishspeaking.ui.vocab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jwahn.englishspeaking.data.WordDraft
import com.jwahn.englishspeaking.data.db.ChapterStat
import com.jwahn.englishspeaking.data.db.WordEntity
import com.jwahn.englishspeaking.data.llm.Prompts
import com.jwahn.englishspeaking.domain.objs
import com.jwahn.englishspeaking.domain.str
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.ErrorCard
import com.jwahn.englishspeaking.ui.common.Loading
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.RingGauge
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SpeakIcon
import com.jwahn.englishspeaking.ui.theme.Amber
import com.jwahn.englishspeaking.ui.theme.Blue
import com.jwahn.englishspeaking.ui.theme.Green
import kotlinx.coroutines.launch

@Composable
fun VocabScreen(nav: Nav) {
    val c = LocalContainer.current
    val stats by c.db.vocab().chapterStats().collectAsState(initial = emptyList())
    val due by c.db.vocab().dueWordCount(System.currentTimeMillis()).collectAsState(initial = 0)

    Screen(
        "단어장",
        actions = {
            TextButton(onClick = nav::wordGen) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(4.dp)); Text("AI 생성") }
        },
    ) { mod ->
        LazyColumn(mod.padding(horizontal = 16.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { nav.study("due") }, modifier = Modifier.weight(1f)) { Text("통합 복습 ($due)") }
                    OutlinedButton(onClick = { nav.study("bookmark") }, modifier = Modifier.weight(1f)) { Text("⭐ 북마크 학습") }
                }
                Text(
                    "암기율 = 3단계 이상 외운 단어 비율 · 암기강도 = 평균 단계/5 · 진도율 = 한 번 이상 본 단어 비율",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val beginner = stats.filter { it.level == 0 }
            val others = stats.filter { it.level != 0 }
            if (beginner.isNotEmpty()) {
                item {
                    com.jwahn.englishspeaking.ui.common.SectionTitle("🌱 왕초보 과정 · AI 없이 시작")
                    Text("알파벳 소리 → 인사 → 기초 단어 → 생활 표현 순서로 해 보세요.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(beginner, key = { it.id }) { ChapterCard(it) { nav.chapter(it.id) } }
            }
            item { com.jwahn.englishspeaking.ui.common.SectionTitle("📚 주제별 단어장") }
            items(others, key = { it.id }) { ChapterCard(it) { nav.chapter(it.id) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ChapterCard(s: ChapterStat, onClick: () -> Unit) {
    // 고정 크기를 쓰지 않아 큰 글씨에서도 잘리지 않는다
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClickLabel = "단어장 열기", onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (s.progress > 0) Amber.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.emoji.isNotBlank()) { Text(s.emoji, fontSize = 28.sp); Spacer(Modifier.width(10.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(s.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${s.total}개" + (if (s.description.isNotBlank()) " · ${s.description}" else ""),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RingGauge(s.memorizeRate, "암기율", Blue, 56.dp)
                RingGauge(s.strength, "암기강도", Amber, 56.dp)
                RingGauge(s.progress, "진도율", Green, 56.dp)
            }
        }
    }
}

@Composable
fun ChapterScreen(nav: Nav, chapterId: Long) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val chapter by produceState<com.jwahn.englishspeaking.data.db.ChapterEntity?>(null, chapterId) { value = c.db.vocab().chapter(chapterId) }
    val words by c.db.vocab().wordsOfChapter(chapterId).collectAsState(initial = emptyList())
    var confirmDelete by remember { mutableStateOf(false) }
    var addOpen by remember { mutableStateOf(false) }

    Screen(
        chapter?.title ?: "챕터",
        onBack = nav::back,
        actions = {
            IconButton(onClick = { addOpen = true }) { Icon(Icons.Default.Add, "단어 추가") }
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "챕터 삭제") }
        },
    ) { mod ->
        LazyColumn(mod.padding(horizontal = 16.dp)) {
            item {
                chapter?.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { nav.study("chapter", chapterId) }, modifier = Modifier.weight(1f), enabled = words.isNotEmpty()) { Text("카드 학습") }
                    OutlinedButton(onClick = { nav.quiz(chapterId) }, modifier = Modifier.weight(1f), enabled = words.size >= 4) { Text("퀴즈") }
                }
            }
            items(words, key = { it.id }) { w -> WordRow(w) { scope.launch { c.db.vocab().updateWord(w.copy(bookmarked = !w.bookmarked)) } } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("챕터 삭제") },
        text = { Text("이 챕터와 단어 ${words.size}개를 모두 삭제할까요?") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { c.db.vocab().deleteWordsOfChapter(chapterId); c.db.vocab().deleteChapter(chapterId); nav.back() }
                confirmDelete = false
            }) { Text("삭제") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
    )

    if (addOpen) AddWordDialog(onDismiss = { addOpen = false }) { w ->
        scope.launch { c.db.vocab().insertWord(WordEntity(chapterId = chapterId, text = w.word, ipa = w.ipa, meaningKo = w.meaningKo, exampleEn = w.exampleEn, exampleKo = w.exampleKo)) }
        addOpen = false
    }
}

@Composable
private fun WordRow(w: WordEntity, onBookmark: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (w.emoji.isNotBlank()) Text(w.emoji + " ")
                    Text(w.text, fontWeight = FontWeight.Bold)
                    if (w.ipa.isNotBlank()) Text("  /${w.ipa}/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(w.meaningKo, style = MaterialTheme.typography.bodyMedium)
                Text(if (w.box == 0) "새 단어" else "${w.box}단계 · 맞음 ${w.correct} / 틀림 ${w.wrong}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            SpeakIcon(w.text)
            IconButton(onClick = onBookmark) {
                Icon(if (w.bookmarked) Icons.Default.Star else Icons.Default.StarBorder, "북마크", tint = Amber)
            }
        }
    }
}

@Composable
private fun AddWordDialog(onDismiss: () -> Unit, onAdd: (WordDraft) -> Unit) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var word by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var ipa by remember { mutableStateOf("") }
    var exEn by remember { mutableStateOf("") }
    var exKo by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("단어 추가") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(word, { word = it }, label = { Text("영어 단어") }, singleLine = true)
                TextButton(enabled = word.isNotBlank() && !loading, onClick = {
                    loading = true; err = null
                    scope.launch {
                        runCatching { c.llm.quickJson(Prompts.wordSet("only the word \"$word\" (explain this exact word)", 1, "any"), 800) }
                            .onSuccess { o ->
                                o.objs("words").firstOrNull()?.let { w ->
                                    meaning = w.str("meaning_ko"); ipa = w.str("ipa").trim('/'); exEn = w.str("example_en"); exKo = w.str("example_ko")
                                }
                            }.onFailure { err = it.message }
                        loading = false
                    }
                }) { Text(if (loading) "AI가 채우는 중…" else "✨ AI로 뜻·예문 채우기") }
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(meaning, { meaning = it }, label = { Text("뜻") })
                OutlinedTextField(ipa, { ipa = it }, label = { Text("발음기호 (선택)") }, singleLine = true)
                OutlinedTextField(exEn, { exEn = it }, label = { Text("예문 (선택)") })
                OutlinedTextField(exKo, { exKo = it }, label = { Text("예문 해석 (선택)") })
            }
        },
        confirmButton = {
            TextButton(enabled = word.isNotBlank() && meaning.isNotBlank(), onClick = { onAdd(WordDraft(word.trim(), ipa, meaning, exEn, exKo)) }) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
fun WordGenScreen(nav: Nav) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var topic by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf(15) }
    var level by rememberSaveable { mutableStateOf("중급") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var words by remember { mutableStateOf<List<WordDraft>>(emptyList()) }
    val levelMap = mapOf("어린이" to "kids (very easy)", "초급" to "beginner (A2)", "중급" to "intermediate (B1-B2)", "고급" to "advanced (C1)")

    Screen("AI로 단어장 만들기", onBack = nav::back) { mod ->
        LazyColumn(mod.padding(horizontal = 16.dp)) {
            item {
                OutlinedTextField(
                    topic, { topic = it }, label = { Text("주제 (예: 해외여행 호텔, 아이 학교생활, 회의 영어)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text("단어 수", Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 15, 20, 30).forEach { n -> FilterChip(count == n, { count = n }, { Text("$n") }) }
                }
                Text("난이도", Modifier.padding(top = 8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levelMap.keys.forEach { l -> FilterChip(level == l, { level = l }, { Text(l) }) }
                }
                Button(
                    enabled = topic.isNotBlank() && !loading,
                    onClick = {
                        loading = true; error = null; words = emptyList()
                        scope.launch {
                            runCatching { c.llm.quickJson(Prompts.wordSet(topic, count, levelMap.getValue(level)), 6000) }
                                .onSuccess { o ->
                                    title = o.str("title_ko").ifBlank { topic }
                                    desc = o.str("description_ko")
                                    words = o.objs("words").mapNotNull { WordDraft.from(it) }
                                    if (words.isEmpty()) error = "단어를 만들지 못했어요. 다시 시도해 주세요."
                                }
                                .onFailure { error = it.message }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                ) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text("단어 만들기") }
                val st by c.settings.state.collectAsState()
                if (st.subscriptionApp.isNotBlank()) {
                    com.jwahn.englishspeaking.ui.handoff.SubscriptionGenerate(
                        nav = nav,
                        prompt = { if (topic.isBlank()) null else Prompts.wordSet(topic, count, levelMap.getValue(level)) },
                        onImport = { im -> words = im.words; title = im.title.ifBlank { topic }; error = null },
                    )
                }
                if (loading) Loading()
                error?.let { ErrorCard(it, onSettings = nav::settings) }
                if (words.isNotEmpty()) {
                    OutlinedTextField(title, { title = it }, label = { Text("챕터 이름") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            scope.launch {
                                val id = c.repo.createChapter(title.ifBlank { topic }, desc, "ai", words)
                                nav.back(); nav.chapter(id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    ) { Text("단어장에 저장 (${words.size}개)") }
                }
            }
            items(words) { w ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${w.word}  /${w.ipa}/", fontWeight = FontWeight.Bold)
                            Text(w.meaningKo)
                            Text(w.exampleEn, style = MaterialTheme.typography.bodySmall)
                        }
                        SpeakIcon(w.word)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

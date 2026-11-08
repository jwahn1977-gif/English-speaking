package com.jwahn.englishspeaking.ui.pattern

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.jwahn.englishspeaking.data.PatternDraft
import com.jwahn.englishspeaking.data.db.PatternEntity
import com.jwahn.englishspeaking.data.db.PatternExampleEntity
import com.jwahn.englishspeaking.data.llm.Prompts
import com.jwahn.englishspeaking.domain.Srs
import com.jwahn.englishspeaking.domain.objs
import com.jwahn.englishspeaking.domain.str
import com.jwahn.englishspeaking.speech.rememberSpeechInput
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.EmptyState
import com.jwahn.englishspeaking.ui.common.ErrorCard
import com.jwahn.englishspeaking.ui.common.Loading
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.PatternText
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SectionTitle
import com.jwahn.englishspeaking.ui.common.SpeakIcon
import com.jwahn.englishspeaking.ui.theme.Green
import com.jwahn.englishspeaking.ui.theme.Red
import com.jwahn.englishspeaking.ui.vocab.SpokenResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

private val sourceLabel = mapOf("builtin" to "기본", "ai" to "AI 생성", "tutor" to "AI 튜터", "news" to "뉴스", "import" to "가져옴")

@Composable
fun PatternsScreen(nav: Nav) {
    val c = LocalContainer.current
    val all by c.db.patterns().all().collectAsState(initial = emptyList())
    val due by c.db.patterns().dueCount(System.currentTimeMillis()).collectAsState(initial = 0)
    var query by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf("전체") }
    val cats = listOf("전체") + all.map { it.category }.distinct().sorted()
    val list = all.filter { (cat == "전체" || it.category == cat) && (query.isBlank() || it.template.contains(query, true) || it.meaningKo.contains(query)) }

    Screen(
        "패턴 (${all.size})",
        actions = { TextButton(onClick = nav::patternGen) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(4.dp)); Text("AI 생성") } },
    ) { mod ->
        LazyColumn(mod.padding(horizontal = 16.dp)) {
            item {
                Text("AI 튜터·뉴스·생성에서 저장한 문장은 재사용할 수 있는 '패턴'으로 모여요. 패턴 하나로 단어만 바꿔 여러 문장을 말해 보세요.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = nav::patternReview, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("패턴 말하기 복습 (복습할 패턴 $due)") }
                OutlinedTextField(query, { query = it }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("패턴·뜻 검색") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cats.forEach { k -> FilterChip(cat == k, { cat = k }, { Text(k) }) }
                }
            }
            if (list.isEmpty()) item { EmptyState("🧩", "패턴이 없어요", "AI 튜터와 대화하거나 'AI 생성'으로 만들어 보세요.") }
            items(list, key = { it.id }) { p ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClickLabel = "패턴 연습하기") { nav.pattern(p.id) }) {
                    Column(Modifier.padding(14.dp)) {
                        PatternText(p.template)
                        Text(p.meaningKo, style = MaterialTheme.typography.bodyMedium)
                        Text("${p.category} · ${sourceLabel[p.source] ?: p.source} · " + if (p.box == 0) "새 패턴" else "${p.box}단계",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun PatternDetailScreen(nav: Nav, id: Long) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val pattern by produceState<PatternEntity?>(null, id) { value = c.db.patterns().byId(id) }
    val examples by c.db.patterns().examples(id).collectAsState(initial = emptyList())
    var spoken by remember { mutableStateOf<Pair<String, String>?>(null) } // target to spoken
    var target by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mine by rememberSaveable { mutableStateOf("") }
    var check by remember { mutableStateOf<Triple<Boolean, String, String>?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val speech = rememberSpeechInput { s -> spoken = target to s }
    val mineSpeech = rememberSpeechInput { s -> mine = s }

    Screen("패턴", onBack = nav::back, actions = { IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, "삭제") } }) { mod ->
        val p = pattern ?: return@Screen
        LazyColumn(mod.padding(horizontal = 16.dp)) {
            item {
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        PatternText(p.template, MaterialTheme.typography.headlineSmall)
                        Text(p.meaningKo, style = MaterialTheme.typography.titleMedium)
                        Text(p.category, style = MaterialTheme.typography.labelSmall)
                    }
                }
                SectionTitle("예문 – 듣고 따라 말해 보세요")
            }
            items(examples, key = { it.id }) { e ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Column(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.en, fontWeight = FontWeight.SemiBold)
                                if (e.ko.isNotBlank()) Text(e.ko, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            SpeakIcon(e.en)
                            IconButton(onClick = { target = e.en; speech("en-US", e.en) }) { Icon(Icons.Default.Mic, "따라 말하기") }
                        }
                        spoken?.takeIf { it.first == e.en }?.let { SpokenResult(it.first, it.second) }
                    }
                }
            }
            item {
                OutlinedButton(
                    enabled = !loading,
                    onClick = {
                        loading = true; error = null
                        scope.launch {
                            runCatching { c.llm.quickJson(Prompts.moreExamples(p.template, p.meaningKo, examples.map { it.en }), 1500) }
                                .onSuccess { o ->
                                    c.repo.savePattern(PatternDraft(p.template, p.meaningKo, o.objs("examples").map { it.str("en") to it.str("ko") }), p.source)
                                }
                                .onFailure { error = it.message }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text("AI로 예문 5개 더 만들기") }

                SectionTitle("내 문장 만들기")
                Text("이 패턴으로 나만의 문장을 말하거나 써 보세요. AI가 확인하고, 맞으면 예문에 추가돼요.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    mine, { mine = it; check = null }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    placeholder = { Text(p.template.replace("~", "…")) },
                    trailingIcon = { IconButton(onClick = { mineSpeech("en-US", p.template) }) { Icon(Icons.Default.Mic, "말하기") } },
                )
                Button(
                    enabled = mine.isNotBlank() && !loading,
                    onClick = {
                        loading = true; error = null
                        scope.launch {
                            runCatching { c.llm.quickJson(Prompts.checkPatternSentence(p.template, mine), 600) }
                                .onSuccess { o ->
                                    val ok = (o["ok"] as? JsonPrimitive)?.booleanOrNull ?: false
                                    val better = o.str("better").ifBlank { mine }
                                    check = Triple(ok, better, o.str("comment_ko"))
                                    c.repo.savePattern(PatternDraft(p.template, p.meaningKo, listOf(better to "")), p.source)
                                    c.repo.recordActivity(sentences = 1)
                                }
                                .onFailure { error = it.message }
                            loading = false
                        }
                    },
                    modifier = Modifier.padding(vertical = 6.dp),
                ) { Text("AI에게 확인받기") }
                check?.let { (ok, better, comment) ->
                    Card(colors = CardDefaults.cardColors(containerColor = if (ok) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if (ok) "👍 좋아요!" else "✏️ 이렇게 고쳐 볼까요?", fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(better, Modifier.weight(1f)); SpeakIcon(better) }
                            if (comment.isNotBlank()) Text(comment, style = MaterialTheme.typography.bodySmall)
                            Text("예문에 추가했어요.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (loading) Loading()
                error?.let { ErrorCard(it, onSettings = nav::settings) }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("패턴 삭제") }, text = { Text("이 패턴과 예문을 삭제할까요?") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch { c.db.patterns().deleteExamples(id); c.db.patterns().delete(id); nav.back() }
                confirmDelete = false
            }) { Text("삭제") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
    )
}

@Composable
fun PatternGenScreen(nav: Nav) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var topic by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableIntStateOf(5) }
    var level by rememberSaveable { mutableStateOf("중급") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf<List<PatternDraft>>(emptyList()) }
    var savedAll by remember { mutableStateOf(false) }
    val levelMap = mapOf("어린이" to "kids (very easy)", "초급" to "beginner (A2)", "중급" to "intermediate (B1-B2)", "고급" to "advanced (C1)")

    Screen("AI로 패턴 만들기", onBack = nav::back) { mod ->
        LazyColumn(mod.padding(horizontal = 16.dp)) {
            item {
                OutlinedTextField(topic, { topic = it }, label = { Text("상황 (예: 식당 불만 말하기, 회의에서 의견 내기)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(3, 5, 8, 10).forEach { n -> FilterChip(count == n, { count = n }, { Text("${n}개") }) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    levelMap.keys.forEach { l -> FilterChip(level == l, { level = l }, { Text(l) }) }
                }
                Button(
                    enabled = topic.isNotBlank() && !loading,
                    onClick = {
                        loading = true; error = null; drafts = emptyList(); savedAll = false
                        scope.launch {
                            runCatching { c.llm.quickJson(Prompts.patternSet(topic, count, levelMap.getValue(level)), 5000) }
                                .onSuccess { o -> drafts = o.objs("patterns").mapNotNull { PatternDraft.from(it, topic.take(12)) } }
                                .onFailure { error = it.message }
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                ) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text("패턴 만들기") }
                if (loading) Loading()
                error?.let { ErrorCard(it, onSettings = nav::settings) }
                if (drafts.isNotEmpty()) Button(
                    enabled = !savedAll,
                    onClick = { scope.launch { drafts.forEach { c.repo.savePattern(it, "ai") }; savedAll = true } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (savedAll) "모두 저장됨 ✅" else "전체 저장 (${drafts.size}개)") }
            }
            items(drafts) { d ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        PatternText(d.pattern)
                        Text(d.meaningKo)
                        d.examples.forEach { (en, ko) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(en, style = MaterialTheme.typography.bodyMedium)
                                    Text(ko, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                SpeakIcon(en)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private data class PatternCard(val p: PatternEntity, val examples: List<PatternExampleEntity>)

/** 한국어 뜻을 보고 영어로 말해 본 뒤 정답 확인 → SRS */
@Composable
fun PatternReviewScreen(nav: Nav) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var cards by remember { mutableStateOf<List<PatternCard>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var spoken by remember { mutableStateOf<String?>(null) }
    val speech = rememberSpeechInput { spoken = it }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val list = c.db.patterns().due(now, 30) + c.db.patterns().unstudied(5)
        cards = list.distinctBy { it.id }.map { PatternCard(it, c.db.patterns().examples(it.id).first()) }
    }

    Screen("패턴 말하기 복습", onBack = nav::back) { mod ->
        val list = cards ?: return@Screen
        if (list.isEmpty() || index >= list.size) {
            EmptyState("🎉", if (list.isEmpty()) "복습할 패턴이 없어요" else "복습 완료!", "새 패턴을 저장하면 여기서 연습할 수 있어요.", mod) {
                Button(onClick = nav::back) { Text("완료") }
            }
            return@Screen
        }
        val card = list[index]
        val ex = remember(card.p.id) { card.examples.filter { it.ko.isNotBlank() }.randomOrNull() ?: card.examples.firstOrNull() }
        Column(mod.verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(progress = { index / list.size.toFloat() }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text(card.p.meaningKo, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (ex != null && ex.ko.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("이 문장을 영어로 말해 보세요:", style = MaterialTheme.typography.labelMedium)
                Text(ex.ko, fontSize = 20.sp, textAlign = TextAlign.Center)
            }
            Button(onClick = { spoken = null; speech("en-US", ex?.ko ?: card.p.meaningKo) }, modifier = Modifier.padding(top = 16.dp)) {
                Icon(Icons.Default.Mic, null); Spacer(Modifier.width(6.dp)); Text("영어로 말하기")
            }
            spoken?.let { s -> if (ex != null) SpokenResult(ex.en, s) else Text("들린 말: $s") }
            Spacer(Modifier.height(16.dp))
            if (!revealed) OutlinedButton(onClick = { revealed = true; ex?.let { c.tts.speak(it.en) } }) { Text("정답 보기") }
            else {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        PatternText(card.p.template, MaterialTheme.typography.titleLarge)
                        ex?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) { Text(it.en, Modifier.weight(1f)); SpeakIcon(it.en) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                    fun answer(ok: Boolean) {
                        val r = Srs.answer(card.p.box, ok)
                        scope.launch { c.db.patterns().update(card.p.copy(box = r.box, nextDue = r.nextDue)); c.repo.recordActivity(reviews = 1) }
                        revealed = false; spoken = null; index++
                    }
                    Button(onClick = { answer(false) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("어려워요") }
                    Button(onClick = { answer(true) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("말할 수 있어요") }
                }
            }
        }
    }
}

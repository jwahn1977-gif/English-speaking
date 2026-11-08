package com.jwahn.englishspeaking.ui.vocab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jwahn.englishspeaking.data.db.WordEntity
import com.jwahn.englishspeaking.domain.SpeechScore
import com.jwahn.englishspeaking.domain.Srs
import com.jwahn.englishspeaking.speech.rememberSpeechInput
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.EmptyState
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SpeakIcon
import com.jwahn.englishspeaking.ui.common.scoreColor
import com.jwahn.englishspeaking.ui.theme.Amber
import com.jwahn.englishspeaking.ui.theme.Green
import com.jwahn.englishspeaking.ui.theme.Red
import kotlinx.coroutines.launch

/** 말하기 결과: 단어별 맞춤 표시 + 점수 */
@Composable
fun SpokenResult(target: String, spoken: String) {
    val score = SpeechScore.score(target, spoken)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("${score}점", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = scoreColor(score))
        Text(
            buildAnnotatedString {
                SpeechScore.wordHits(target, spoken).forEach { (w, ok) ->
                    withStyle(SpanStyle(color = if (ok) Green else Red, fontWeight = FontWeight.SemiBold)) { append(w) }
                    append(" ")
                }
            },
            textAlign = TextAlign.Center,
        )
        Text("들린 말: $spoken", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** 카드 학습. mode = chapter / due / bookmark */
@Composable
fun StudyScreen(nav: Nav, mode: String, chapterId: Long) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val settings by c.settings.state.collectAsState()
    var words by remember { mutableStateOf<List<WordEntity>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var spoken by remember { mutableStateOf<String?>(null) }
    var spokenTarget by remember { mutableStateOf("") }
    var known by remember { mutableIntStateOf(0) }
    var unknown by remember { mutableIntStateOf(0) }

    LaunchedEffect(mode, chapterId) {
        val now = System.currentTimeMillis()
        words = when (mode) {
            "due" -> c.db.vocab().dueWords(now)
            "bookmark" -> c.db.vocab().bookmarkedWords()
            else -> c.db.vocab().wordsOfChapterOnce(chapterId)
                .sortedWith(compareBy({ if (it.box == 0) 0 else 1 }, { it.nextDue }))
        }
    }

    val speech = rememberSpeechInput { text -> spoken = text }

    val title = when (mode) { "due" -> "통합 복습"; "bookmark" -> "북마크 학습"; else -> "카드 학습" }
    Screen(title, onBack = nav::back) { mod ->
        val list = words
        when {
            list == null -> {}
            list.isEmpty() -> EmptyState(
                "🎉", if (mode == "due") "오늘 복습할 단어가 없어요" else "단어가 없어요",
                if (mode == "due") "챕터에서 새 단어를 학습하면 복습 일정이 자동으로 잡혀요." else "단어를 추가하거나 북마크해 보세요.",
                mod,
            ) { Button(onClick = nav::back) { Text("돌아가기") } }
            index >= list.size -> EmptyState(
                "🏁", "학습 완료!", "알아요 $known · 몰라요 $unknown\n틀린 단어는 내일 다시 나와요.", mod,
            ) { Button(onClick = nav::back) { Text("완료") } }
            else -> {
                val w = list[index]
                LaunchedEffect(w.id) { c.tts.speak(w.text) }
                Column(mod.verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(progress = { index / list.size.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text("${index + 1} / ${list.size}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp))
                    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (w.box == 0) "NEW" else "${w.box}단계", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = {
                                    val nw = w.copy(bookmarked = !w.bookmarked)
                                    scope.launch { c.db.vocab().updateWord(nw) }
                                    words = list.toMutableList().also { it[index] = nw }
                                }) { Icon(if (w.bookmarked) Icons.Default.Star else Icons.Default.StarBorder, "북마크", tint = Amber) }
                            }
                            Text(w.text, fontSize = 40.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            if (w.ipa.isNotBlank()) Text("/${w.ipa}/", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 12.dp)) {
                                FilledTonalButton(onClick = { c.tts.speak(w.text) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null); Spacer(Modifier.width(4.dp)); Text("듣기") }
                                FilledTonalButton(onClick = { spokenTarget = w.text; spoken = null; speech("en-US", w.text) }) {
                                    Icon(Icons.Default.Mic, null); Spacer(Modifier.width(4.dp)); Text("따라 말하기")
                                }
                            }
                            spoken?.let { s ->
                                SpokenResult(spokenTarget, s)
                                LaunchedEffect(s) {
                                    val sc = SpeechScore.score(spokenTarget, s)
                                    if (sc > w.bestScore) {
                                        val nw = w.copy(bestScore = sc)
                                        c.db.vocab().updateWord(nw)
                                        words = list.toMutableList().also { it[index] = nw }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            if (revealed) {
                                Text(w.meaningKo, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                if (w.exampleEn.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(w.exampleEn, Modifier.weight(1f), textAlign = TextAlign.Center)
                                        SpeakIcon(w.exampleEn)
                                        IconButton(onClick = { spokenTarget = w.exampleEn; spoken = null; speech("en-US", w.exampleEn) }) { Icon(Icons.Default.Mic, "예문 말하기") }
                                    }
                                    if (settings.showKorean && w.exampleKo.isNotBlank())
                                        Text(w.exampleKo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            } else {
                                OutlinedButton(onClick = { revealed = true }) { Text("뜻 보기") }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        fun answer(ok: Boolean) {
                            val r = Srs.answer(w.box, ok)
                            val updated = w.copy(
                                box = r.box, nextDue = r.nextDue,
                                correct = w.correct + if (ok) 1 else 0, wrong = w.wrong + if (ok) 0 else 1,
                            )
                            scope.launch { c.db.vocab().updateWord(updated); c.repo.recordActivity(reviews = 1) }
                            if (ok) known++ else unknown++
                            revealed = false; spoken = null; index++
                        }
                        Button(onClick = { answer(false) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("몰라요") }
                        Button(onClick = { answer(true) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("알아요") }
                    }
                }
            }
        }
    }
}

private data class QuizQ(val word: WordEntity, val listening: Boolean, val choices: List<String>, val answer: String)

@Composable
fun QuizScreen(nav: Nav, chapterId: Long) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var qs by remember { mutableStateOf<List<QuizQ>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<String?>(null) }
    var correct by remember { mutableIntStateOf(0) }

    LaunchedEffect(chapterId) {
        val words = c.db.vocab().wordsOfChapterOnce(chapterId).shuffled().take(20)
        val pool = (c.db.vocab().randomWords(40) + words).distinctBy { it.id }
        qs = words.mapIndexed { i, w ->
            val listening = i % 2 == 1
            val others = pool.filter { it.id != w.id }.shuffled().take(3)
            if (listening) QuizQ(w, true, (others.map { it.text } + w.text).shuffled(), w.text)
            else QuizQ(w, false, (others.map { it.meaningKo } + w.meaningKo).shuffled(), w.meaningKo)
        }
    }

    Screen("퀴즈", onBack = nav::back) { mod ->
        val list = qs ?: return@Screen
        if (index >= list.size) {
            EmptyState("🏆", "퀴즈 끝!", "${list.size}문제 중 ${correct}개 정답", mod) { Button(onClick = nav::back) { Text("완료") } }
            return@Screen
        }
        val q = list[index]
        LaunchedEffect(index) { if (q.listening) c.tts.speak(q.word.text) }
        Column(mod.verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(progress = { index / list.size.toFloat() }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            if (q.listening) {
                Text("🎧 들리는 단어를 고르세요", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { c.tts.speak(q.word.text) }) { Icon(Icons.AutoMirrored.Filled.VolumeUp, "다시 듣기", Modifier.width(48.dp).height(48.dp)) }
            } else {
                Text("뜻을 고르세요", style = MaterialTheme.typography.titleMedium)
                Text(q.word.text, fontSize = 36.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            }
            Spacer(Modifier.height(16.dp))
            q.choices.forEach { ch ->
                val color = when {
                    picked == null -> MaterialTheme.colorScheme.primary
                    ch == q.answer -> Green
                    ch == picked -> Red
                    else -> Color.Gray
                }
                Button(
                    onClick = {
                        if (picked != null) return@Button
                        picked = ch
                        val ok = ch == q.answer
                        if (ok) correct++
                        val r = Srs.answer(q.word.box, ok)
                        scope.launch {
                            c.db.vocab().updateWord(q.word.copy(box = r.box, nextDue = r.nextDue,
                                correct = q.word.correct + if (ok) 1 else 0, wrong = q.word.wrong + if (ok) 0 else 1))
                            c.repo.recordActivity(reviews = 1)
                        }
                        if (!q.listening) c.tts.speak(q.word.text)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) { Text(ch, Modifier.padding(6.dp)) }
            }
            if (picked != null) {
                Spacer(Modifier.height(12.dp))
                Text("${q.word.text} = ${q.word.meaningKo}", fontWeight = FontWeight.SemiBold)
                Button(onClick = { picked = null; index++ }, modifier = Modifier.padding(top = 8.dp)) { Text("다음") }
            }
        }
    }
}

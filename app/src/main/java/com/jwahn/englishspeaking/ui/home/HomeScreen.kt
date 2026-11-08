package com.jwahn.englishspeaking.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jwahn.englishspeaking.AppContainer
import com.jwahn.englishspeaking.domain.Levels
import com.jwahn.englishspeaking.domain.Streak
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SectionTitle
import com.jwahn.englishspeaking.ui.common.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeState(
    val dueWords: Int = 0,
    val duePatterns: Int = 0,
    val newPatterns: Int = 0,
    val usedWords: Int = 0,
    val streak: Int = 0,
    val todayReviews: Int = 0,
    val todaySentences: Int = 0,
    val todayListened: Int = 0,
    val totalWords: Int = 0,
    val memorized: Int = 0,
)

class HomeViewModel(c: AppContainer) : ViewModel() {
    private val now = System.currentTimeMillis()
    private val counts = combine(
        c.db.vocab().dueWordCount(now),
        c.db.patterns().dueCount(now),
        c.db.patterns().unstudiedCount(),
        c.db.tutor().usedWordCount(),
    ) { a, b, n, u -> listOf(a, b, n, u) }
    private val words = combine(c.db.vocab().wordCount(), c.db.vocab().memorizedCount()) { t, m -> t to m }

    val state = combine(counts, words, c.db.stats().recent()) { cnt, w, stats ->
        val today = stats.firstOrNull { it.date == LocalDate.now().toString() }
        HomeState(
            dueWords = cnt[0], duePatterns = cnt[1], newPatterns = cnt[2], usedWords = cnt[3],
            streak = Streak.count(stats.filter { it.reviews + it.sentences + it.listened > 0 }.map { it.date }.toSet()),
            todayReviews = today?.reviews ?: 0, todaySentences = today?.sentences ?: 0, todayListened = today?.listened ?: 0,
            totalWords = w.first, memorized = w.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
}

@Composable
fun HomeScreen(nav: Nav) {
    val vm = appViewModel { HomeViewModel(it) }
    val s by vm.state.collectAsState()
    val settings by LocalContainer.current.settings.state.collectAsState()
    val level = Levels.of(s.usedWords)
    val next = Levels.next(level)

    Screen(
        title = if (settings.nickname.isNotBlank()) "${settings.nickname}님, 안녕하세요 👋" else "오늘도 영어 한 마디 👋",
        actions = { IconButton(onClick = nav::settings) { Icon(Icons.Default.Settings, "설정") } },
    ) { mod ->
        Column(mod.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            com.jwahn.englishspeaking.ui.news.NowPlayingBar(onOpen = { nav.newsLesson(it) })
            if (!settings.anyConnected) {
                Card(
                    Modifier.fillMaxWidth().padding(top = 8.dp).clickable { nav.settings() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("🔌 AI를 연결해 주세요", fontWeight = FontWeight.Bold)
                        Text(
                            "Claude, ChatGPT, Gemini 중 하나의 API 키를 넣으면 AI 튜터, 단어·패턴 생성, 뉴스 번역을 쓸 수 있어요. (단어장·복습은 AI 없이도 돼요)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = nav::settings) { Text("AI 연결 설정") }
                    }
                }
            }

            // 레벨 + 스트릭
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(level.emoji, fontSize = 40.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Lv.${level.no} ${level.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (next != null) "대화에서 쓴 단어 ${s.usedWords}개 · 다음 레벨까지 ${next.need - s.usedWords}개"
                                else "대화에서 쓴 단어 ${s.usedWords}개 · 최고 레벨!",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥 ${s.streak}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("연속", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { Levels.progress(s.usedWords) }, modifier = Modifier.fillMaxWidth())
                }
            }

            SectionTitle("오늘의 복습")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReviewTile("📚 단어", s.dueWords, "복습하기", Modifier.weight(1f)) { nav.study("due") }
                ReviewTile("🧩 패턴", s.duePatterns + minOf(s.newPatterns, 5), "연습하기", Modifier.weight(1f)) { nav.patternReview() }
            }

            SectionTitle("오늘 한 일")
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Stat("복습", s.todayReviews, "/${settings.dailyGoal}")
                    Stat("말한 문장", s.todaySentences, "")
                    Stat("들은 문장", s.todayListened, "")
                    Stat("외운 단어", s.memorized, "/${s.totalWords}")
                }
            }

            SectionTitle("바로 시작")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAction("🗣️", "AI 튜터와 대화", "상황별 롤플레이·비즈니스·OPIc 연습") { nav.tab("tutor") }
                QuickAction("🎧", "뉴스 듣기", "실제 영어 뉴스를 문장별로 듣고 따라 말하기") { nav.tab("news") }
                QuickAction("✨", "AI로 단어장 만들기", "원하는 주제의 단어 세트를 바로 생성") { nav.wordGen() }
                QuickAction("🧩", "AI로 패턴 만들기", "상황에 맞는 말하기 패턴을 생성해서 저장") { nav.patternGen() }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReviewTile(title: String, count: Int, action: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text("${count}개", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = onClick, enabled = count > 0) { Text(if (count > 0) action else "완료 ✅") }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, suffix: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value$suffix", fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun QuickAction(emoji: String, title: String, sub: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

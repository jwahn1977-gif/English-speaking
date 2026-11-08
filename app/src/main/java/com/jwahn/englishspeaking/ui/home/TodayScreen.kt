package com.jwahn.englishspeaking.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jwahn.englishspeaking.AppContainer
import com.jwahn.englishspeaking.data.settings.AppSettings
import com.jwahn.englishspeaking.data.settings.Learner
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.Screen

/** 오늘의 공부 10분: 정해진 순서대로 하나씩 */
data class TodayPlan(
    val dueWords: Int,
    val newChapterId: Long?,
    val newChapterTitle: String,
    val patternCount: Int,
    val speakScenarioId: Long?,
    val speakTitle: String,
    val phrasesChapterId: Long?,
)

/** 학습자 유형과 목표에 맞는 오늘 계획 */
suspend fun buildTodayPlan(c: AppContainer, s: AppSettings): TodayPlan {
    val now = System.currentTimeMillis()
    val maxLevel = when (s.learner) {
        Learner.KIDS, Learner.STARTER -> 0
        Learner.BASIC -> 1
        else -> 2
    }
    val chapter = c.db.vocab().nextChapterToLearn(maxLevel) ?: c.db.vocab().nextChapterToLearn(2)
    val due = c.db.vocab().dueWords(now).size
    val patterns = c.db.patterns().due(now, 30).size + minOf(c.db.patterns().unstudied(5).size, 3)
    val category = when {
        s.learner == Learner.KIDS || s.goal == "kids" -> "KIDS"
        s.goal == "work" -> "BUSINESS"
        s.goal == "test" -> "OPIC"
        s.learner == Learner.ADVANCED -> "ADVANCED"
        s.goal == "daily" -> "DAILY"
        else -> "ROLEPLAY"
    }
    val scenarios = c.db.tutor().allScenarios()
    // 날마다 다른 상황으로 돌아가며
    val pool = scenarios.filter { it.category == category }.ifEmpty { scenarios }
    val pick = pool.getOrNull((java.time.LocalDate.now().toEpochDay() % pool.size.coerceAtLeast(1)).toInt())
    val phrases = c.db.vocab().allChapters().firstOrNull { it.source == "beginner" && it.title.startsWith("생활 표현") }
    return TodayPlan(due, chapter?.id, chapter?.let { "${it.emoji} ${it.title}" }.orEmpty(), patterns,
        pick?.id, pick?.let { "${it.emoji} ${it.title}" }.orEmpty(), phrases?.id)
}

@Composable
fun TodayScreen(nav: Nav) {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    val done by c.settings.todayDone.collectAsState()
    LaunchedEffect(Unit) { c.settings.refreshToday() }
    val plan by produceState<TodayPlan?>(null, done) { value = buildTodayPlan(c, s) }

    Screen("오늘의 공부 10분", onBack = nav::back) { mod ->
        val p = plan ?: return@Screen
        val speakWithAi = (s.anyConnected || s.subscriptionApp.isNotBlank()) && p.speakScenarioId != null
        data class Step(val id: String, val emoji: String, val title: String, val sub: String, val skip: Boolean, val go: () -> Unit)
        val steps = listOf(
            Step("review", "🔁", "복습하기", if (p.dueWords > 0) "잊기 전에 다시 보기 ${p.dueWords}개" else "오늘 복습할 단어가 없어요",
                p.dueWords == 0) { nav.study("due") },
            Step("new", "🆕", "새 단어 5개", p.newChapterTitle.ifBlank { "모든 단어를 배웠어요! AI로 새 단어장을 만들어 보세요" },
                p.newChapterId == null) { p.newChapterId?.let { nav.study("new", it) } },
            Step("pattern", "🧩", "패턴 말하기", if (p.patternCount > 0) "한국어를 보고 영어로 말하기" else "연습할 패턴이 없어요",
                p.patternCount == 0) { nav.patternReview() },
            if (speakWithAi) Step("speak", "🗣️", "AI와 3번 주고받기", p.speakTitle, false) { if (s.subscriptionOnly) nav.handoffTutor(p.speakScenarioId!!) else nav.chat(p.speakScenarioId!!) }
            else Step("speak", "💬", "생활 표현 따라 말하기", "AI 없이 통째로 외우는 문장", p.phrasesChapterId == null) {
                p.phrasesChapterId?.let { nav.study("new", it) }
            },
        )
        val finished = steps.count { it.id in done || it.skip }
        Column(mod.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("순서대로 하나씩 눌러 보세요. 끝나면 이 화면으로 돌아와요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { finished / steps.size.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text("$finished / ${steps.size} 완료", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(8.dp))
            steps.forEachIndexed { i, st ->
                val isDone = st.id in done
                val label = "${i + 1}. ${st.title}"
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        .clickable(enabled = !st.skip, onClickLabel = "$label 시작") { c.settings.markToday(st.id); st.go() },
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isDone -> MaterialTheme.colorScheme.tertiaryContainer
                            st.skip -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                    ),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(st.emoji, fontSize = 30.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label, fontWeight = FontWeight.Bold)
                            Text(st.sub, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            when { isDone -> "✓ 완료"; st.skip -> "건너뜀"; else -> "시작 ›" },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (finished == steps.size) {
                Spacer(Modifier.height(16.dp))
                Text("🎉 오늘 공부 끝! 내일도 만나요.", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Button(onClick = nav::back, modifier = Modifier.padding(top = 8.dp)) { Text("홈으로") }
            }
        }
    }
}

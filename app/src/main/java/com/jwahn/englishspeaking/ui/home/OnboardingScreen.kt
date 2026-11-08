package com.jwahn.englishspeaking.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jwahn.englishspeaking.data.settings.Learner
import com.jwahn.englishspeaking.data.settings.TutorLevel
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.settings.OpenRouterLoginCard
import com.jwahn.englishspeaking.ui.settings.PRIVACY_NOTE

private val goals = listOf(
    "travel" to "✈️ 여행", "daily" to "💬 일상 대화", "work" to "💼 업무", "test" to "📝 시험(OPIc)",
    "kids" to "🎒 아이 공부", "news" to "📰 뉴스 듣기",
)

/** 첫 실행 안내: 누가 쓰는지, 목표, 글자 크기·속도, AI 연결(선택) */
@Composable
fun OnboardingScreen(onDone: () -> Unit, onLogin: ((com.jwahn.englishspeaking.ui.handoff.SubApp) -> Unit)? = null) {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var learner by rememberSaveable { mutableStateOf(Learner.STARTER) }
    var name by rememberSaveable { mutableStateOf(s.nickname) }
    var goal by rememberSaveable { mutableStateOf("daily") }
    var font by rememberSaveable { mutableFloatStateOf(1.0f) }
    var rate by rememberSaveable { mutableFloatStateOf(0.9f) }

    fun finish() {
        c.settings.update {
            it.copy(
                onboarded = true, learner = learner, nickname = name.trim(), goal = goal,
                fontScale = font, ttsRate = rate,
                tutorLevel = if (learner == Learner.KIDS) TutorLevel.KIDS else TutorLevel.AUTO,
                showSuggestions = learner != Learner.ADVANCED,
                dailyAiLimit = if (learner == Learner.KIDS) 30 else it.dailyAiLimit,
            )
        }
        onDone()
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 32.dp),
    ) {
        LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        when (step) {
            0 -> {
                Text("반가워요! 👋", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("누가 쓰나요? 고른 대로 쉬운 것부터 시작해요. 나중에 언제든 바꿀 수 있어요.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { name = it }, label = { Text("이름 (선택)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Learner.entries.forEach { l ->
                    ChoiceCard(l.emoji, l.label, l.desc, selected = learner == l) { learner = l }
                }
            }
            1 -> {
                Text("무엇을 위해 배우나요?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("가장 가까운 것 하나를 골라 주세요. 추천 대화와 단어에 반영돼요.")
                Spacer(Modifier.height(12.dp))
                goals.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                        row.forEach { (k, label) ->
                            FilterChip(goal == k, { goal = k }, { Text(label, Modifier.padding(8.dp)) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            2 -> {
                Text("보기 편하게 맞춰요", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("글자 크기", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1.0f to "보통", 1.2f to "크게", 1.4f to "아주 크게").forEach { (v, label) ->
                        FilterChip(font == v, { font = v; c.settings.update { it.copy(fontScale = v) } }, { Text(label) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("영어 읽어 주는 속도", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.7f to "아주 천천히", 0.9f to "천천히", 1.0f to "보통").forEach { (v, label) ->
                        FilterChip(rate == v, { rate = v; c.tts.speak("Hello! Nice to meet you.", v) }, { Text(label) })
                    }
                }
                OutlinedButton(onClick = { c.tts.speak("Hello! Nice to meet you.", rate) }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("🔊 들어 보기")
                }
            }
            else -> {
                Text("AI 선생님 연결 (선택)", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("AI를 연결하면 대화 연습, 단어·패턴 만들기, 뉴스 번역을 쓸 수 있어요. 왕초보 과정, 단어장, 복습은 연결하지 않아도 돼요.")
                com.jwahn.englishspeaking.ui.settings.SubscriptionLoginCard(onLogin)
                OpenRouterLoginCard()
                Text(PRIVACY_NOTE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("회사별 키로 직접 연결하려면 나중에 설정 > AI 연결에서 할 수 있어요.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text("이전") }
            Spacer(Modifier.weight(1f))
            if (step < 3) Button(onClick = { step++ }, modifier = Modifier.heightIn(min = 52.dp)) { Text("다음") }
            else Button(onClick = ::finish, modifier = Modifier.heightIn(min = 52.dp)) {
                Text(if (s.anyConnected || s.subscriptionApp.isNotBlank()) "시작하기" else "AI 없이 먼저 시작하기")
            }
        }
    }
}

@Composable
private fun ChoiceCard(emoji: String, title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 30.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall)
            }
            if (selected) Text("✓", fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

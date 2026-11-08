package com.jwahn.englishspeaking.ui.handoff

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jwahn.englishspeaking.data.PatternDraft
import com.jwahn.englishspeaking.data.WordDraft
import com.jwahn.englishspeaking.data.db.ScenarioEntity
import com.jwahn.englishspeaking.data.settings.Learner
import com.jwahn.englishspeaking.data.settings.TutorLevel
import com.jwahn.englishspeaking.data.settings.levelPrompt
import com.jwahn.englishspeaking.domain.JsonText
import com.jwahn.englishspeaking.domain.objs
import com.jwahn.englishspeaking.domain.str
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.PatternSaveCard
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SectionTitle
import com.jwahn.englishspeaking.ui.common.WordSaveCard
import kotlinx.coroutines.launch

/** 복사해 온 결과(JSON)에서 패턴·단어를 꺼낸다 */
data class Imported(val title: String, val patterns: List<PatternDraft>, val words: List<WordDraft>)

fun parseImported(text: String, defaultCategory: String): Imported? {
    val o = JsonText.parseObject(text) ?: return null
    val patterns = o.objs("patterns").mapNotNull { PatternDraft.from(it, defaultCategory) }
    val words = o.objs("words").mapNotNull { WordDraft.from(it) }
    if (patterns.isEmpty() && words.isEmpty()) return null
    return Imported(o.str("title_ko"), patterns, words)
}

/** 어떤 공식 앱으로 보낼지 고르기 */
@Composable
fun SubAppPicker(selected: SubApp, onSelect: (SubApp) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SubApp.entries.forEach { a -> FilterChip(selected == a, { onSelect(a) }, { Text(a.label) }) }
    }
}

/** 구독 계정으로 AI 튜터 대화: 지시문을 넘기고 결과를 가져온다 */
@Composable
fun HandoffTutorScreen(nav: Nav, scenarioId: Long) {
    val c = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by c.settings.state.collectAsState()
    val scenario by produceState<ScenarioEntity?>(null, scenarioId) { value = c.db.tutor().scenario(scenarioId) }
    var app by remember { mutableStateOf(SubApp.of(s.subscriptionApp) ?: SubApp.CLAUDE) }
    var imported by remember { mutableStateOf<Imported?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Screen(scenario?.let { "${it.emoji} ${it.title}" } ?: "구독 앱으로 대화", onBack = nav::back) { mod ->
        val sc = scenario ?: return@Screen
        LazyColumn(mod.padding(horizontal = 16.dp)) {
            item {
                Text("내 ${app.label} 구독 계정으로 대화해요. 앱이 수업 지시문을 만들어 넘겨 줍니다.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SubAppPicker(app) { app = it; c.settings.update { st -> st.copy(subscriptionApp = it.id) } }
                StepCard("1", "${app.label}에서 대화 시작",
                    "버튼을 누르면 ${app.label}가 열려요. " +
                        if (app.prefill) "입력칸에 지시문이 들어가 있으면 보내기만 누르세요." else "입력칸을 길게 눌러 붙여넣고 보내세요.")
                Button(
                    onClick = {
                        scope.launch {
                            val kids = sc.category == "KIDS" || s.learner == Learner.KIDS || s.tutorLevel == TutorLevel.KIDS
                            val prompt = HandoffPrompts.tutor(sc, s.levelPrompt(c.db.tutor().usedWordCountOnce()), kids)
                            openInSubApp(context, app, prompt)
                            c.repo.recordActivity(sentences = 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("${app.label}에서 대화 시작하기") }
                StepCard("2", "영어로 대화하기",
                    "음성 입력(🎤)을 쓰면 말하기 연습이 돼요. 막히면 한국어로 써도 고쳐 줘요. 끝나면 END 라고 보내세요.")
                StepCard("3", "결과 가져오기",
                    "${app.label}의 마지막 답(피드백과 코드 블록)을 길게 눌러 복사한 뒤, 아래 버튼을 누르세요. 패턴과 단어가 복습에 들어가요.")
                OutlinedButton(
                    onClick = {
                        val r = parseImported(readClipboard(context), sc.title)
                        imported = r
                        message = if (r == null) "복사한 내용에서 결과를 찾지 못했어요. 마지막 답 전체를 복사했는지 확인해 주세요." else null
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("📋 복사한 결과 가져오기") }
                message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
                imported?.let { im ->
                    Button(
                        onClick = {
                            scope.launch {
                                im.patterns.forEach { c.repo.savePattern(it, "tutor") }
                                im.words.forEach { c.repo.saveWord(it, "AI 튜터에서 저장한 단어", "tutor") }
                                message = "✅ 패턴 ${im.patterns.size}개, 단어 ${im.words.size}개를 저장했어요."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) { Text("모두 저장") }
                    if (im.patterns.isNotEmpty()) SectionTitle("🧩 패턴")
                }
            }
            imported?.let { im ->
                items(im.patterns) { PatternSaveCard(it, "tutor", Modifier.padding(vertical = 4.dp)) }
                if (im.words.isNotEmpty()) item { SectionTitle("📚 단어") }
                items(im.words) { WordSaveCard(it, "AI 튜터에서 저장한 단어", "tutor", Modifier.padding(vertical = 4.dp)) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StepCard(no: String, title: String, body: String) {
    Card(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("$no. $title", fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * 단어장·패턴 생성 화면에 붙는 '구독 앱으로 만들기' 영역.
 * prompt: 만들 지시문, onImport: 복사한 결과를 받아 화면에 채운다
 */
@Composable
fun SubscriptionGenerate(prompt: () -> String?, onImport: (Imported) -> Unit) {
    val c = LocalContainer.current
    val context = LocalContext.current
    val s by c.settings.state.collectAsState()
    var app by remember { mutableStateOf(SubApp.of(s.subscriptionApp) ?: SubApp.CLAUDE) }
    var message by remember { mutableStateOf<String?>(null) }
    Card(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("구독 계정으로 만들기", fontWeight = FontWeight.Bold)
            Text("① 버튼으로 지시문을 보내고 ② 나온 답 전체를 복사해서 ③ 가져오기를 누르세요.",
                style = MaterialTheme.typography.bodySmall)
            SubAppPicker(app) { app = it; c.settings.update { st -> st.copy(subscriptionApp = it.id) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                Button(onClick = {
                    val p = prompt()
                    if (p == null) message = "주제를 먼저 적어 주세요." else openInSubApp(context, app, p)
                }) { Text("${app.label}로 보내기") }
                OutlinedButton(onClick = {
                    val r = parseImported(readClipboard(context), "구독 앱")
                    if (r == null) message = "복사한 내용에서 결과를 찾지 못했어요." else { message = null; onImport(r) }
                }) { Text("📋 결과 가져오기") }
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

package com.jwahn.englishspeaking.ui.tutor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.CorrectionCard
import com.jwahn.englishspeaking.ui.common.ErrorCard
import com.jwahn.englishspeaking.ui.common.Loading
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.PatternSaveCard
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SectionTitle
import com.jwahn.englishspeaking.ui.common.SpeakIcon
import com.jwahn.englishspeaking.ui.common.WordSaveCard
import com.jwahn.englishspeaking.ui.common.appViewModel
import com.jwahn.englishspeaking.ui.common.scoreColor
import com.jwahn.englishspeaking.speech.rememberSpeechInput

private const val TUTOR_CHAPTER = "AI 튜터에서 저장한 단어"

@Composable
fun ChatScreen(nav: Nav, scenarioId: Long, conversationId: Long) {
    val vm = appViewModel(key = "chat-$scenarioId-$conversationId") { ChatViewModel(it, scenarioId, conversationId) }
    val st by vm.state.collectAsState()
    val settings by LocalContainer.current.settings.state.collectAsState()
    val tts = LocalContainer.current.tts
    var input by rememberSaveable { mutableStateOf("") }
    var hintOpen by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf("") }

    val speech = rememberSpeechInput { text ->
        if (st.voiceMode) vm.send(text) else input = if (input.isBlank()) text else "$input $text"
    }
    val hintSpeech = rememberSpeechInput { text -> hintText = text }

    LaunchedEffect(Unit) { vm.listen.collect { speech("en-US", "영어로 말해 보세요") } }

    val listState = rememberLazyListState()
    LaunchedEffect(st.items.size, st.loading) {
        val count = st.items.size + (if (st.loading) 1 else 0) + 1
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    val report = st.report
    if (report != null) {
        ReportView(report, st.scenario?.title.orEmpty(), onClose = { vm.closeReport() }, onDone = nav::back)
        return
    }

    Screen(
        title = st.scenario?.let { "${it.emoji} ${it.title}" } ?: "AI 튜터",
        onBack = nav::back,
        actions = {
            IconButton(onClick = vm::toggleVoice) {
                Icon(if (st.voiceMode) Icons.Default.Keyboard else Icons.Default.RecordVoiceOver,
                    if (st.voiceMode) "채팅 모드" else "음성 모드")
            }
            TextButton(onClick = vm::finish, enabled = !st.reportLoading) {
                Icon(Icons.Default.Flag, null); Text("종료·리포트")
            }
        },
    ) { mod ->
        Column(mod.imePadding()) {
            // 미션
            val missions = st.scenario?.missions?.lines()?.filter { it.isNotBlank() }.orEmpty()
            if (missions.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    missions.forEachIndexed { i, m ->
                        AssistChip(onClick = {}, label = { Text((if (i in st.missionsDone) "✅ " else "⬜ ") + m) })
                    }
                }
            }
            Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    if (st.voiceMode) "🎙️ 음성 모드: AI가 말을 마치면 자동으로 마이크가 켜져요" else "⌨️ 채팅 모드: 입력하거나 🎤로 말해 보세요. 💡는 한국어로 힌트 받기",
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(6.dp),
                )
            }

            LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp), state = listState) {
                items(st.items, key = { it.id }) { item ->
                    if (item.role == "user") UserBubble(item.text)
                    else AssistantBubble(item, settings.showKorean)
                }
                if (st.loading) item { Text("상대가 입력 중…", Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item {
                    st.error?.let { ErrorCard(it, onRetry = vm::retry, onSettings = nav::settings) }
                    if (st.reportLoading) Loading("대화를 분석해서 리포트를 만드는 중…")
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (st.voiceMode) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), contentAlignment = Alignment.Center) {
                    LargeFloatingActionButton(onClick = { tts.stop(); speech("en-US", "영어로 말해 보세요") }, shape = CircleShape) {
                        Icon(Icons.Default.Mic, "말하기", Modifier.size(36.dp))
                    }
                    IconButton(onClick = { hintOpen = true }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.Lightbulb, "힌트")
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { hintOpen = true }) { Icon(Icons.Default.Lightbulb, "힌트") }
                    OutlinedTextField(
                        value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                        placeholder = { Text("영어로 답해 보세요 (한국어도 OK)") }, maxLines = 4,
                    )
                    IconButton(onClick = { speech("en-US", null) }) { Icon(Icons.Default.Mic, "음성 입력") }
                    FilledIconButton(
                        onClick = { vm.send(input); input = "" }, enabled = input.isNotBlank() && !st.loading,
                        colors = IconButtonDefaults.filledIconButtonColors(),
                    ) { Icon(Icons.AutoMirrored.Filled.Send, "보내기") }
                }
            }
        }
    }

    if (hintOpen) {
        AlertDialog(
            onDismissRequest = { hintOpen = false; vm.clearHints() },
            title = { Text("💡 이렇게 말하고 싶어요") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        hintText, { hintText = it }, label = { Text("한국어로 적거나 말하기") },
                        trailingIcon = { IconButton(onClick = { hintSpeech("ko-KR", "한국어로 말해 보세요") }) { Icon(Icons.Default.Mic, null) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = { vm.askHint(hintText) }, enabled = hintText.isNotBlank() && !st.hintLoading, modifier = Modifier.padding(top = 8.dp)) {
                        Text("영어 표현 보기")
                    }
                    if (st.hintLoading) Loading("표현 찾는 중…")
                    st.hints?.forEach { h ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            if (st.voiceMode) vm.send(h.en) else input = h.en
                            hintOpen = false; hintText = ""; vm.clearHints()
                        }) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(h.en, fontWeight = FontWeight.SemiBold)
                                    if (h.note.isNotBlank()) Text(h.note, style = MaterialTheme.typography.bodySmall)
                                }
                                SpeakIcon(h.en)
                            }
                        }
                    }
                    if (st.hints != null) Text("문장을 누르면 입력창에 들어가요.", style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = { TextButton(onClick = { hintOpen = false; vm.clearHints() }) { Text("닫기") } },
        )
    }
}

@Composable
private fun UserBubble(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.CenterEnd) {
        Text(
            text,
            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                .background(MaterialTheme.colorScheme.primary).padding(12.dp),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun AssistantBubble(item: ChatItem, showKoDefault: Boolean) {
    var showKo by remember(item.id) { mutableStateOf(showKoDefault) }
    var extras by remember(item.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        item.correction?.let { CorrectionCard(it.original, it.better, it.why, Modifier.padding(bottom = 6.dp)) }
        Row(verticalAlignment = Alignment.Top) {
            Column(
                Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).clickable { showKo = !showKo }.padding(12.dp),
            ) {
                Text(item.text, style = MaterialTheme.typography.bodyLarge)
                if (showKo && item.ko.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.ko, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SpeakIcon(item.text)
        }
        if (item.patterns.isNotEmpty() || item.words.isNotEmpty()) {
            TextButton(onClick = { extras = !extras }) {
                Text((if (extras) "▲ " else "▼ ") + "🧩 패턴 ${item.patterns.size} · 📚 단어 ${item.words.size} 저장하기", fontSize = 13.sp)
            }
            if (extras) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.patterns.forEach { PatternSaveCard(it, "tutor") }
                item.words.forEach { WordSaveCard(it, TUTOR_CHAPTER, "tutor") }
            }
        }
    }
}

@Composable
private fun ReportView(r: Report, title: String, onClose: () -> Unit, onDone: () -> Unit) {
    Screen("대화 리포트", onBack = onClose) { mod ->
        LazyColumn(mod.padding(horizontal = 16.dp)) {
            item {
                Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        Text("${r.score}점", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = scoreColor(r.score))
                        Text("의사소통 점수", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(r.summary)
                    }
                }
                if (r.good.isNotEmpty()) {
                    SectionTitle("👍 잘한 점")
                    r.good.forEach { Text("• $it", Modifier.padding(vertical = 2.dp)) }
                }
                if (r.improvements.isNotEmpty()) SectionTitle("✏️ 이렇게 말하면 더 좋아요")
            }
            items(r.improvements) { CorrectionCard(it.original, it.better, it.why, Modifier.padding(vertical = 4.dp)) }
            item { if (r.patterns.isNotEmpty()) SectionTitle("🧩 이 상황의 핵심 패턴 (저장하면 복습에 나와요)") }
            items(r.patterns) { PatternSaveCard(it, "tutor", Modifier.padding(vertical = 4.dp)) }
            item { if (r.words.isNotEmpty()) SectionTitle("📚 핵심 단어") }
            items(r.words) { WordSaveCard(it, TUTOR_CHAPTER, "tutor", Modifier.padding(vertical = 4.dp)) }
            item {
                Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onClose) { Text("대화로 돌아가기") }
                    Button(onClick = onDone) { Text("끝내기") }
                }
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}

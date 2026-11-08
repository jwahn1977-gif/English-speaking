package com.jwahn.englishspeaking.ui.settings

import android.Manifest
import android.widget.Toast
import com.jwahn.englishspeaking.Notifications
import com.jwahn.englishspeaking.reminder.Reminder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jwahn.englishspeaking.data.settings.Provider
import com.jwahn.englishspeaking.data.settings.TutorLevel
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val keyLinks = mapOf(
    Provider.CLAUDE to "https://console.anthropic.com/settings/keys",
    Provider.OPENAI to "https://platform.openai.com/api-keys",
    Provider.GEMINI to "https://aistudio.google.com/apikey",
)

@Composable
fun SettingsScreen(nav: Nav) {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val msg = runCatching {
                val text = c.repo.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
                }
                "백업 파일을 저장했어요."
            }.getOrElse { "백업 실패: ${it.message}" }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val msg = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }.orEmpty()
                }
                c.repo.importJson(text)
            }.getOrElse { "가져오기 실패: 올바른 백업 파일인지 확인하세요. (${it.message})" }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Screen("설정", onBack = nav::back) { mod ->
        Column(mod.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("내 정보")
            var nick by remember { mutableStateOf(s.nickname) }
            OutlinedTextField(
                value = nick, onValueChange = { nick = it; c.settings.update { st -> st.copy(nickname = it) } },
                label = { Text("이름(닉네임) – AI 튜터가 불러줘요") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("가족 프로필")
            ProfileSection()

            SectionTitle("AI 연결")
            Text(PRIVACY_NOTE, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FreeGeminiCard()
            SubscriptionLoginCard(onLogin = { nav.webChat(it) })
            Text("앱 안에서 바로 대화·교정·자동 저장까지 하려면 아래 'API 연결'을 쓰세요.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OpenRouterLoginCard()
            if (s.hasKey(Provider.OPENROUTER)) ProviderCard(Provider.OPENROUTER, "")
            var advanced by remember { mutableStateOf(s.connectedProviders.any { it != Provider.OPENROUTER }) }
            TextButton(onClick = { advanced = !advanced }) {
                Text((if (advanced) "▲ " else "▼ ") + "회사별 키로 직접 연결 (Claude · ChatGPT · Gemini)")
            }
            if (advanced) {
                Text(
                    "구독(Pro, Plus) 계정은 다른 앱에 연결할 수 없어서 각 회사의 API 키가 필요해요. '웹에서 로그인하고 키 받기'를 누르고, 만든 키를 복사해서 돌아오면 자동으로 채워져요. 키는 이 폰에만 암호화되어 저장되고, 백업 파일에도 들어가지 않아요.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Provider.entries.filter { it != Provider.OPENROUTER }.forEach { p -> ProviderCard(p, keyLinks.getValue(p)) }
            }
            Spacer(Modifier.height(8.dp))
            Text("기본으로 쓸 AI", fontWeight = FontWeight.SemiBold)
            Provider.entries.filter { s.hasKey(it) || it == s.defaultProvider }.forEach { p ->
                Row(
                    Modifier.fillMaxWidth().clickable { c.settings.update { it.copy(defaultProvider = p) } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.defaultProvider == p, onClick = { c.settings.update { it.copy(defaultProvider = p) } })
                    Text(p.label + if (s.hasKey(p)) "  ✅" else "")
                }
            }
            SectionTitle("AI 사용량")
            UsageSection()

            SectionTitle("보기 · 듣기")
            DisplaySection()

            SectionTitle("학습 설정")
            Text("읽기 속도: ${"%.1f".format(s.ttsRate)}배")
            Slider(
                value = s.ttsRate, onValueChange = { v -> c.settings.update { it.copy(ttsRate = (v * 10).toInt() / 10f) } },
                valueRange = 0.5f..1.5f, steps = 9,
            )
            OutlinedButton(onClick = { c.tts.speak("Hello! This is how fast I will read English sentences.") }) { Text("속도 들어보기") }
            SwitchRow("한국어 해석 보이기", s.showKorean) { v -> c.settings.update { it.copy(showKorean = v) } }
            SwitchRow("AI 튜터 답변 자동으로 읽기", s.autoSpeak) { v -> c.settings.update { it.copy(autoSpeak = v) } }
            Spacer(Modifier.height(8.dp))
            Text("AI 튜터 난이도", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TutorLevel.entries.forEach { l ->
                    FilterChip(selected = s.tutorLevel == l, onClick = { c.settings.update { it.copy(tutorLevel = l) } }, label = { Text(l.label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("하루 복습 목표: ${s.dailyGoal}개")
            Slider(
                value = s.dailyGoal.toFloat(), onValueChange = { v -> c.settings.update { it.copy(dailyGoal = v.toInt()) } },
                valueRange = 10f..100f, steps = 8,
            )

            SectionTitle("복습 알림")
            ReminderSection()

            SectionTitle("백업 · 가족과 공유")
            Text(
                "단어장, 패턴, 내가 만든 시나리오, 뉴스 피드를 파일로 저장해요. 다른 가족 폰에서 '가져오기'를 하면 그대로 합쳐집니다.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                Button(onClick = { exportLauncher.launch("english-backup-${LocalDate.now()}.json") }) { Text("내보내기") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) { Text("가져오기") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ProviderCard(p: Provider, link: String) {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var key by remember { mutableStateOf(s.key(p)) }
    var model by remember { mutableStateOf(s.models[p].orEmpty()) }
    var show by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var menu by remember { mutableStateOf(false) }
    var waitingKey by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }
    ClipboardKeyWatcher(p, waitingKey && key.isBlank()) { k ->
        key = k; waitingKey = false
        c.settings.update { it.copy(keys = it.keys + (p to k)) }
    }

    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (p == Provider.OPENROUTER) "OpenRouter 모델 고르기" else p.label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            if (p != Provider.OPENROUTER && key.isBlank()) WebKeyButton(link) { waitingKey = true }
            if (p != Provider.OPENROUTER) OutlinedTextField(
                value = key,
                onValueChange = { v -> key = v.trim(); c.settings.update { it.copy(keys = it.keys + (p to v.trim())) } },
                label = { Text("API 키") }, placeholder = { Text(p.keyHint) }, singleLine = true,
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { show = !show }) {
                        Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, "보기")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Column {
                OutlinedTextField(
                    value = model,
                    onValueChange = { v -> model = v; c.settings.update { it.copy(models = it.models + (p to v.trim())) } },
                    label = { Text("모델 (비우면 기본값: ${p.defaultModel})") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (p == Provider.OPENROUTER && models.isNotEmpty()) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("" to "전체", "anthropic/" to "Claude", "openai/" to "GPT", "google/" to "Gemini", ":free" to "무료").forEach { (k, label) ->
                            FilterChip(filter == k, { filter = k; menu = true }, { Text(label) })
                        }
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    models.filter { filter.isBlank() || it.contains(filter) }.take(80).forEach { m ->
                        DropdownMenuItem(text = { Text(m) }, onClick = {
                            model = m; menu = false
                            c.settings.update { it.copy(models = it.models + (p to m)) }
                        })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Button(
                    enabled = key.isNotBlank() && !testing,
                    onClick = {
                        testing = true; result = null
                        scope.launch {
                            result = runCatching {
                                models = c.llm.listModels(p, key)
                                if (models.isNotEmpty()) menu = true
                                "✅ 연결 성공! 사용 가능한 모델 ${models.size}개 (목록에서 선택할 수 있어요)"
                            }.getOrElse { "❌ ${it.message}" }
                            testing = false
                        }
                    },
                ) { Text("연결 테스트 · 모델 불러오기") }
                if (testing) { Spacer(Modifier.width(8.dp)); CircularProgressIndicator(Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp) }
            }
            result?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
            if (models.isNotEmpty()) TextButton(onClick = { menu = true }) { Text("모델 목록 다시 보기") }
        }
    }
    HorizontalDivider(Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.surface)
}

@Composable
private fun ReminderSection() {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    val context = LocalContext.current
    fun apply(enabled: Boolean, hour: Int, minute: Int) {
        c.settings.update { it.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute) }
        Reminder.schedule(context, enabled, hour, minute)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) apply(true, s.reminderHour, s.reminderMinute)
        else Toast.makeText(context, "알림 권한이 없어서 복습 알림을 켤 수 없어요.", Toast.LENGTH_LONG).show()
    }
    SwitchRow("매일 복습 알림 받기", s.reminderEnabled) { on ->
        if (on && Notifications.needsPermission(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else apply(on, s.reminderHour, s.reminderMinute)
    }
    if (s.reminderEnabled) {
        Text("알림 시간: %02d:%02d".format(s.reminderHour, s.reminderMinute))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(7, 8, 12, 18, 20, 21, 22).forEach { h ->
                FilterChip(s.reminderHour == h, { apply(true, h, s.reminderMinute) }, { Text("${h}시") })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 15, 30, 45).forEach { m ->
                FilterChip(s.reminderMinute == m, { apply(true, s.reminderHour, m) }, { Text("%02d분".format(m)) })
            }
        }
        Text("복습할 단어·패턴이 있거나 오늘 아직 공부를 안 했을 때만 알려줘요.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileSection() {
    val c = LocalContainer.current
    val profiles by c.profiles.list.collectAsState()
    val activity = LocalContext.current as? android.app.Activity
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🧒") }
    var removeTarget by remember { mutableStateOf<com.jwahn.englishspeaking.data.settings.Profile?>(null) }
    Text(
        "한 폰을 가족이 함께 쓸 때 사람마다 학습 기록·레벨·설정을 따로 저장해요. AI 연결은 함께 씁니다.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    profiles.forEach { p ->
        val current = p.id == c.profileId
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clickable(onClickLabel = "${p.name} 프로필로 바꾸기", enabled = !current) {
                    activity?.let { com.jwahn.englishspeaking.MainActivity.switchProfile(it, p.id) }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = current, onClick = null)
            Text("${p.emoji} ${p.name}" + if (current) " (지금 사용 중)" else "", Modifier.weight(1f))
            if (p.id != "default") TextButton(onClick = { removeTarget = p }) { Text("삭제") }
        }
    }
    if (adding) {
        OutlinedTextField(name, { name = it }, label = { Text("이름 (예: 민준)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("🧒", "👧", "👦", "👩", "👨", "👵", "👴", "🐶", "🐱", "🦁").forEach { e ->
                FilterChip(emoji == e, { emoji = e }, { Text(e) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = name.isNotBlank(), onClick = {
                val p = c.profiles.add(name, emoji)
                adding = false; name = ""
                activity?.let { com.jwahn.englishspeaking.MainActivity.switchProfile(it, p.id) }
            }) { Text("만들고 바꾸기") }
            TextButton(onClick = { adding = false }) { Text("취소") }
        }
    } else {
        OutlinedButton(onClick = { adding = true }) { Text("+ 가족 프로필 추가") }
    }
    removeTarget?.let { t ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("프로필 삭제") },
            text = { Text("'${t.name}'의 학습 기록과 설정이 모두 지워져요. 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    val ctx = activity ?: return@TextButton
                    if (t.id == c.profileId) com.jwahn.englishspeaking.MainActivity.switchProfile(ctx, "default")
                    c.profiles.remove(ctx, t.id); removeTarget = null
                }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun UsageSection() {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    val u by c.settings.usage.collectAsState()
    LaunchedEffect(Unit) { c.settings.refreshUsage() }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("오늘: AI 사용 ${u.todayCalls}회 · 약 ${"%,d".format(u.todayTokens)} 토큰")
            Text("이번 달: ${u.monthCalls}회 · 약 ${"%,d".format(u.monthTokens)} 토큰", style = MaterialTheme.typography.bodySmall)
            Text(
                "토큰은 AI가 읽고 쓴 글자 양이에요. 요금은 고른 AI와 모델에 따라 다르며, 정확한 금액은 각 회사 사이트에서 확인하세요.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("하루 AI 사용 한도: " + if (s.dailyAiLimit == 0) "제한 없음" else "${s.dailyAiLimit}회", fontWeight = FontWeight.SemiBold)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0, 20, 50, 100, 200).forEach { n ->
            FilterChip(s.dailyAiLimit == n, { c.settings.update { it.copy(dailyAiLimit = n) } }, { Text(if (n == 0) "없음" else "${n}회") })
        }
    }
    Text("아이 프로필에 한도를 두면 요금 걱정 없이 쓰게 할 수 있어요. 대화 한 번 주고받기가 1회예요.",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DisplaySection() {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    Text("글자 크기", fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(1.0f to "보통", 1.2f to "크게", 1.4f to "아주 크게").forEach { (v, label) ->
            FilterChip(s.fontScale == v, { c.settings.update { it.copy(fontScale = v) } }, { Text(label) })
        }
    }
    Spacer(Modifier.height(8.dp))
    VoiceEngineSection()
    Spacer(Modifier.height(8.dp))
    Text("휴대폰 음성 고르기", fontWeight = FontWeight.SemiBold)
    var voices by remember { mutableStateOf(c.tts.englishVoices()) }
    var open by remember { mutableStateOf(false) }
    val currentLabel = voices.firstOrNull { it.name == s.ttsVoice }?.label ?: "기기 기본 목소리"
    Column {
        OutlinedButton(onClick = { voices = c.tts.englishVoices(); open = true }) { Text(currentLabel) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("기기 기본 목소리") }, onClick = {
                c.settings.update { it.copy(ttsVoice = "") }; open = false
            })
            voices.take(40).forEach { v ->
                DropdownMenuItem(text = { Text(v.label) }, onClick = {
                    c.settings.update { it.copy(ttsVoice = v.name) }; open = false
                    c.tts.speak("Hello! I will read English for you.")
                })
            }
        }
    }
    Text("목소리가 적으면 휴대폰 설정 > 텍스트 음성 변환에서 Google 음성 데이터(영어)를 받으면 더 자연스러운 목소리가 생겨요.",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    SwitchRow("대화할 때 예시 답변 보여주기", s.showSuggestions) { v -> c.settings.update { it.copy(showSuggestions = v) } }
    SwitchRow("소리 없이 공부하기 (듣기 문제를 글자로)", s.silentMode) { v -> c.settings.update { it.copy(silentMode = v) } }
}

@Composable
private fun VoiceEngineSection() {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    val err by c.tts.cloudError.collectAsState()
    val engine = com.jwahn.englishspeaking.speech.VoiceEngine.of(s.voiceEngine)
    var cacheMb by remember { mutableStateOf(0L) }
    LaunchedEffect(s.voiceEngine) { cacheMb = (c.tts.cloud?.cacheSizeBytes() ?: 0L) / (1024 * 1024) }

    Text("읽어 주는 목소리", fontWeight = FontWeight.SemiBold)
    Text(
        "AI 고음질 음성은 사람처럼 자연스러워요. 연결해 둔 OpenAI 또는 Gemini 키를 쓰고, 한 번 들은 문장은 폰에 저장돼서 다시 들을 땐 요금·인터넷 없이 바로 나와요.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        com.jwahn.englishspeaking.speech.VoiceEngine.entries.forEach { e ->
            val available = e.needs == null || s.hasKey(e.needs)
            FilterChip(
                selected = engine == e,
                onClick = { c.settings.update { it.copy(voiceEngine = e.name, cloudVoice = e.defaultVoice) } },
                label = { Text(e.label + if (!available) " (키 필요)" else "") },
                enabled = available,
            )
        }
    }
    if (engine.voices.isNotEmpty()) {
        val current = s.cloudVoice.ifBlank { engine.defaultVoice }
        engine.voices.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (id, label) ->
                    FilterChip(current == id, {
                        c.settings.update { it.copy(cloudVoice = id) }
                        c.tts.speak("Hi! I'm your English helper. Let's practice together.")
                    }, { Text(label) })
                }
            }
        }
    }
    if (engine != com.jwahn.englishspeaking.speech.VoiceEngine.DEVICE && engine.needs != null && !s.hasKey(engine.needs)) {
        Text("이 음성을 쓰려면 AI 연결 > 회사별 키로 직접 연결에서 ${engine.needs.label} 키를 넣어 주세요. (구독 계정과 OpenRouter로는 음성을 만들 수 없어요)",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { c.tts.speak("Hello! This is how I will read English for you. Can you hear me clearly?") }) { Text("🔊 들어 보기") }
        if (engine != com.jwahn.englishspeaking.speech.VoiceEngine.DEVICE) {
            TextButton(onClick = { c.tts.cloud?.clearCache(); cacheMb = 0 }) { Text("저장된 음성 지우기 (${cacheMb}MB)") }
        }
    }
    err?.let {
        Text("고음질 음성을 못 만들어서 휴대폰 음성으로 읽었어요: $it",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

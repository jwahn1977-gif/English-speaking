package com.jwahn.englishspeaking.ui.settings

import android.widget.Toast
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

            SectionTitle("AI 연결")
            Text(
                "각 회사의 API 키를 입력하면 바로 사용할 수 있어요. 키는 이 폰 안에만 암호화되어 저장되고, 백업 파일에도 들어가지 않아요. 가족이 각자 폰에 설치해서 같은 키를 넣어도 됩니다.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text("기본으로 쓸 AI", fontWeight = FontWeight.SemiBold)
            Provider.entries.forEach { p ->
                Row(
                    Modifier.fillMaxWidth().clickable { c.settings.update { it.copy(defaultProvider = p) } },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.defaultProvider == p, onClick = { c.settings.update { it.copy(defaultProvider = p) } })
                    Text(p.label + if (s.hasKey(p)) "  ✅" else "")
                }
            }
            Provider.entries.forEach { p -> ProviderCard(p, keyLinks.getValue(p)) }

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

    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { uri.openUri(link) }) { Text("키 발급받기") }
            }
            OutlinedTextField(
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
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    models.take(60).forEach { m ->
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

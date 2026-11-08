package com.jwahn.englishspeaking.ui.settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jwahn.englishspeaking.data.settings.Provider
import com.jwahn.englishspeaking.ui.common.LocalContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 대화 내용이 AI 회사로 전송된다는 안내 */
const val PRIVACY_NOTE = "AI 기능(튜터, 생성, 번역)을 쓰면 입력한 내용이 연결한 AI 회사로 전송돼요. 이름·주소 같은 개인정보는 말하지 마세요."

fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { Toast.makeText(context, "브라우저를 열 수 없어요.", Toast.LENGTH_LONG).show() }
}

/**
 * 가장 쉬운 연결: OpenRouter 웹 로그인.
 * 로그인하면 키가 자동으로 저장되고, Claude·GPT·Gemini 모델을 모두 쓸 수 있다.
 */
@Composable
fun OpenRouterLoginCard(onConnected: () -> Unit = {}) {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val connected = s.hasKey(Provider.OPENROUTER)

    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("API 연결: 웹 로그인 (앱 안에서 바로 대화)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "브라우저에서 OpenRouter에 로그인(구글 계정 가능)하면 자동으로 연결돼요. 키를 복사할 필요가 없고, " +
                    "하나의 계정으로 Claude·ChatGPT·Gemini 모델을 모두 고를 수 있어요. 사용한 만큼 충전 금액에서 빠지며, 이름에 ':free'가 붙은 무료 모델도 있어요.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            if (connected) {
                Text("✅ 연결됨", fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = { openUrl(context, "https://openrouter.ai/settings/credits") }) { Text("충전·사용 내역") }
                    TextButton(onClick = {
                        c.settings.update { it.copy(keys = it.keys + (Provider.OPENROUTER to "")) }
                    }) { Text("연결 해제") }
                }
            } else if (job?.isActive == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("브라우저에서 로그인을 마치면 자동으로 돌아와요…", Modifier.weight(1f))
                }
                TextButton(onClick = { job?.cancel(); job = null }) { Text("취소") }
            } else {
                Button(
                    onClick = {
                        message = null
                        job = scope.launch {
                            runCatching { c.openRouterAuth.login(openBrowser = { url -> openUrl(context, url) }) }
                                .onSuccess { key ->
                                    c.settings.update {
                                        it.copy(keys = it.keys + (Provider.OPENROUTER to key), defaultProvider = Provider.OPENROUTER)
                                    }
                                    message = "✅ 연결됐어요!"
                                    onConnected()
                                }
                                .onFailure { e ->
                                    if (e !is kotlinx.coroutines.CancellationException) message = "❌ ${e.message ?: "로그인 실패"}"
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Login, null)
                    Spacer(Modifier.width(6.dp))
                    Text("웹 로그인으로 연결하기")
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        }
    }
}

/** 복사된 문자열이 해당 AI 회사 키처럼 보이는지 */
fun looksLikeKey(p: Provider, text: String): Boolean {
    val t = text.trim()
    if (t.length < 20 || t.contains(' ')) return false
    return when (p) {
        Provider.OPENROUTER -> t.startsWith("sk-or-")
        Provider.CLAUDE -> t.startsWith("sk-ant-")
        Provider.OPENAI -> t.startsWith("sk-") && !t.startsWith("sk-ant-") && !t.startsWith("sk-or-")
        Provider.GEMINI -> t.startsWith("AIza")
    }
}

/**
 * "웹에서 키 받기"로 브라우저에 다녀오면, 복사해 둔 키를 자동으로 채운다.
 * waiting=true일 때만 동작한다.
 */
@Composable
fun ClipboardKeyWatcher(p: Provider, waiting: Boolean, onKey: (String) -> Unit) {
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!waiting) return@LifecycleEventEffect
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = runCatching { cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() }.getOrNull().orEmpty()
        if (looksLikeKey(p, text)) {
            onKey(text.trim())
            Toast.makeText(context, "복사한 ${p.label} 키를 넣었어요.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun WebKeyButton(link: String, onOpened: () -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = { onOpened(); openUrl(context, link) }) { Text("웹에서 로그인하고 키 받기") }
}

/**
 * 구독 계정(Claude Pro, ChatGPT Plus, Gemini)으로 쓰기.
 * 공식 앱/사이트에 로그인해 두면, 앱이 수업 지시문을 그쪽으로 넘기고 결과를 복사해 가져온다.
 */
@Composable
fun SubscriptionLoginCard(onLogin: ((com.jwahn.englishspeaking.ui.handoff.SubApp) -> Unit)? = null) {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    val context = LocalContext.current
    val current = com.jwahn.englishspeaking.ui.handoff.SubApp.of(s.subscriptionApp)
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("구독 계정으로 로그인 (Claude · ChatGPT · Gemini)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "이미 쓰는 Claude Pro, ChatGPT Plus, Gemini 계정으로 공부해요. 아래 버튼을 누르면 Chrome에서 로그인 화면이 열려요. 한 번 로그인하면 계속 유지돼요.\n" +
                    "AI 튜터와 단어·패턴 만들기를 누르면 Chrome에서 그 서비스와 대화가 시작되고, 수업 지시문이 자동으로 들어가요. 추가 요금이 없어요.\n" +
                    "※ 각 회사가 구독 계정을 다른 앱의 AI 엔진으로 쓰는 것을 허용하지 않아, 대화는 그 서비스 화면에서 하고 결과는 '결과 가져오기'로 저장해요.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            com.jwahn.englishspeaking.ui.handoff.SubApp.entries.forEach { app ->
                val using = current == app
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            c.settings.update { it.copy(subscriptionApp = app.id) }
                            if (onLogin != null) onLogin(app) else openUrl(context, app.loginUrl)
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("${app.label} 로그인") }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            using && s.isLoginConfirmed(app.id) -> "🔐 로그인됨"
                            using -> "선택됨\n(로그인 필요)"
                            else -> ""
                        },
                        Modifier.width(88.dp), style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(
                "로그인과 대화는 Chrome 창에서 열려요. Chrome에서 한 번 로그인하면 계속 유지되고, 구글 계정 로그인도 돼요.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { com.jwahn.englishspeaking.ui.handoff.openInChrome(context, com.jwahn.englishspeaking.ui.handoff.LoginHelp.GOOGLE_RECOVERY) }) {
                Text("구글 계정 비밀번호 찾기")
            }
            TextButton(onClick = { com.jwahn.englishspeaking.ui.handoff.openInChrome(context, com.jwahn.englishspeaking.ui.handoff.LoginHelp.OPENAI_RESET) }) {
                Text("ChatGPT 비밀번호 재설정")
            }
            if (current != null) TextButton(onClick = { c.settings.update { it.copy(subscriptionApp = "") } }) { Text("구독 계정 사용 해제") }
        }
    }
}


/**
 * 가장 쉬운 '앱 안에서 바로' 연결: Gemini 무료 API 키.
 * Chrome에서 구글 로그인 → 키 만들기 → 복사 → 앱으로 돌아오면 자동 입력 후 실제로 연결을 확인한다.
 */
@Composable
fun FreeGeminiCard() {
    val c = LocalContainer.current
    val s by c.settings.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var waiting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val connected = s.hasKey(Provider.GEMINI)

    fun verify(key: String) {
        checking = true; status = null
        scope.launch {
            status = runCatching { c.llm.listModels(Provider.GEMINI, key) }
                .fold(
                    { "✅ 연결 확인 완료! 이제 AI 튜터·단어 만들기·뉴스 번역이 앱 안에서 바로 동작해요." },
                    { "❌ 키 확인 실패: ${it.message}" },
                )
            checking = false
        }
    }

    ClipboardKeyWatcher(Provider.GEMINI, waiting && !connected) { key ->
        waiting = false
        c.settings.update { it.copy(keys = it.keys + (Provider.GEMINI to key), defaultProvider = Provider.GEMINI) }
        verify(key)
    }

    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("⭐ 추천: 무료로 앱 안에서 바로 쓰기 (Gemini)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                "구글 계정으로 무료 키를 받으면 AI 튜터 대화·교정·예시 답변·자동 저장·고음질 음성까지 모두 이 앱 안에서 돼요. 다른 앱으로 넘어가지 않아요. (무료 사용량 안에서는 요금이 없어요)",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            if (connected) {
                Text(if (checking) "연결 확인 중…" else status ?: "🔐 Gemini 연결됨", fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = { verify(s.key(Provider.GEMINI)) }) { Text("연결 다시 확인") }
                    TextButton(onClick = { c.settings.update { it.copy(keys = it.keys + (Provider.GEMINI to "")) }; status = null }) { Text("연결 해제") }
                }
            } else {
                Text("① 아래 버튼 → Chrome에서 구글 로그인\n② 'API 키 만들기(Create API key)' → 만들어진 키 옆 복사 버튼\n③ 이 앱으로 돌아오면 자동으로 연결돼요",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    waiting = true
                    com.jwahn.englishspeaking.ui.handoff.openInChrome(context, "https://aistudio.google.com/apikey")
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Chrome에서 무료 키 받기") }
                if (waiting) Text("키를 복사한 뒤 이 앱으로 돌아오세요…", style = MaterialTheme.typography.bodySmall)
            }
            status?.takeIf { it.startsWith("❌") }?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

package com.jwahn.englishspeaking.ui.handoff

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.PatternSaveCard
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.WordSaveCard
import kotlinx.coroutines.launch

/** 앱 안 화면으로 열 공식 서비스와 지시문 (화면 이동 사이에 잠깐 보관) */
object WebChatRequest {
    var prompt: String? = null
    /** 결과를 가져왔을 때 넘겨줄 곳 (단어장·패턴 만들기 화면). 없으면 바로 저장 */
    var onImport: ((Imported) -> Unit)? = null
}

/**
 * Claude · ChatGPT · Gemini 공식 웹을 앱 안에서 연다.
 * - 한 번 로그인하면 쿠키가 앱에 남아 계속 로그인 상태
 * - 지시문은 입력칸에 미리 채우고(Claude, ChatGPT) 복사도 해 둔다
 * - 대화 내용을 이 앱이 읽거나 조작하지 않는다. 결과는 사용자가 복사해서 가져온다.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebChatScreen(nav: Nav, appId: String) {
    val c = LocalContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = SubApp.of(appId) ?: SubApp.CLAUDE
    val prompt = remember { WebChatRequest.prompt.also { WebChatRequest.prompt = null } }
    val onImport = remember { WebChatRequest.onImport.also { WebChatRequest.onImport = null } }
    val startUrl = remember { if (prompt != null) app.chatUrl(prompt) else app.loginUrl }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var imported by remember { mutableStateOf<Imported?>(null) }
    var pendingMic by remember { mutableStateOf<PermissionRequest?>(null) }
    val appSettings by c.settings.state.collectAsState()
    // 구글 로그인이 필요한 경우 Chrome 창으로 (Gemini는 구글 계정 전용이라 항상)
    val external = appSettings.opensExternally(app.id)
    var googleBlocked by remember { mutableStateOf(false) }
    var helpOpen by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(external) {
        if (external) {
            prompt?.let {
                copyText(context, it)
                Toast.makeText(context, "수업 지시문이 들어가요. 빨간 경고가 보여도 보내기(↑)를 누르세요.", Toast.LENGTH_LONG).show()
            }
            openInChrome(context, startUrl)
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingMic?.let { if (granted) it.grant(it.resources) else it.deny() }
        pendingMic = null
    }

    LaunchedOnce(prompt) { if (prompt != null) copyText(context, prompt) }

    BackHandler(enabled = canGoBack) { webView?.goBack() }
    DisposableEffect(Unit) { onDispose { CookieManager.getInstance().flush() } }

    if (googleBlocked) androidx.compose.material3.AlertDialog(
        onDismissRequest = { googleBlocked = false },
        title = { Text("구글 로그인은 Chrome에서 해요") },
        text = { Text("구글은 보안 때문에 다른 앱 안의 화면에서 구글 계정 로그인을 막아요. 비밀번호가 맞아도 '안전하지 않은 브라우저' 오류가 나요.\n\nChrome 창으로 열면 정상적으로 로그인되고, 다음부터는 자동으로 Chrome에서 열려요.") },
        confirmButton = {
            Button(onClick = {
                googleBlocked = false
                c.settings.update { it.withExternal(app.id, true) }
            }) { Text("Chrome으로 열기") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { googleBlocked = false; openInChrome(context, LoginHelp.GOOGLE_RECOVERY) }) { Text("비밀번호 찾기") }
                TextButton(onClick = { googleBlocked = false }) { Text("이메일로 로그인") }
            }
        },
    )

    if (helpOpen) androidx.compose.material3.AlertDialog(
        onDismissRequest = { helpOpen = false },
        title = { Text("${app.label} 로그인 도움말") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(LoginHelp.note(app))
                Text("구글 계정 비밀번호를 잊었다면 '구글 비밀번호 찾기'를 누르세요. 휴대폰 번호나 복구 이메일로 본인 확인 후 새 비밀번호를 만들 수 있어요.",
                    style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                TextButton(onClick = { helpOpen = false; openInChrome(context, LoginHelp.GOOGLE_RECOVERY) }) { Text("구글 비밀번호 찾기") }
                if (app == SubApp.CHATGPT) TextButton(onClick = { helpOpen = false; openInChrome(context, LoginHelp.OPENAI_RESET) }) { Text("ChatGPT 비밀번호 재설정") }
                if (!external) TextButton(onClick = { helpOpen = false; c.settings.update { it.withExternal(app.id, true) } }) { Text("Chrome으로 열기 (구글 로그인)") }
                TextButton(onClick = { helpOpen = false }) { Text("닫기") }
            }
        },
    )

    Screen(
        title = app.label,
        onBack = nav::back,
        actions = {
            TextButton(onClick = {
                val r = parseImported(readClipboard(context), app.label)
                if (r == null) Toast.makeText(context, "복사한 내용에서 결과를 찾지 못했어요. END를 보낸 뒤 마지막 답의 복사 버튼을 눌러 주세요.", Toast.LENGTH_LONG).show()
                else if (onImport != null) { onImport(r); nav.back() }
                else imported = r
            }) { Text("📋 결과 가져오기") }
            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "더 보기") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (prompt != null) DropdownMenuItem(text = { Text("지시문 다시 복사") }, onClick = {
                    copyText(context, prompt); menu = false
                    Toast.makeText(context, "복사했어요. 입력칸을 길게 눌러 붙여넣기 하세요.", Toast.LENGTH_SHORT).show()
                })
                DropdownMenuItem(text = { Text("새로고침") }, onClick = { webView?.reload(); menu = false })
                DropdownMenuItem(text = { Text("${app.label} 앱이나 브라우저로 열기") }, onClick = {
                    menu = false
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webView?.url ?: startUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                })
            }
        },
    ) { mod ->
        Column(mod) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(
                        when {
                            external -> "Chrome 창에서 ${app.label}가 열려요. " +
                                (if (prompt != null) "지시문이 입력칸에 들어가 있어요(안 보이면 길게 눌러 붙여넣기). 대화를 마치면 END → 마지막 답 복사 → 이 화면으로 돌아와 위의 '결과 가져오기'." else "구글 계정으로 로그인하면 Chrome에 계속 남아요.")
                            prompt != null -> "수업 지시문이 입력칸에 들어가 있어요(안 보이면 길게 눌러 붙여넣기). 영어로 대화하고, 끝나면 END → 마지막 답 복사 → 위의 '결과 가져오기'."
                            else -> "처음 한 번 로그인하면 다음부터는 바로 열려요."
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { helpOpen = true }) { Text("로그인이 안 돼요? · 비밀번호 찾기") }
                }
            }
            imported?.let { im ->
                Surface(tonalElevation = 2.dp) {
                    Column(Modifier.padding(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("가져온 결과: 패턴 ${im.patterns.size} · 단어 ${im.words.size}", fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f).padding(top = 12.dp))
                            Button(onClick = {
                                scope.launch {
                                    im.patterns.forEach { c.repo.savePattern(it, "tutor") }
                                    im.words.forEach { c.repo.saveWord(it, "AI 튜터에서 저장한 단어", "tutor") }
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        Toast.makeText(context, "✅ 저장했어요. 복습에 나와요.", Toast.LENGTH_SHORT).show()
                                        imported = null
                                    }
                                }
                            }) { Text("모두 저장") }
                            TextButton(onClick = { imported = null }) { Text("닫기") }
                        }
                        LazyColumn(Modifier.heightIn(max = 260.dp)) {
                            items(im.patterns) { PatternSaveCard(it, "tutor", Modifier.padding(vertical = 3.dp)) }
                            items(im.words) { WordSaveCard(it, "AI 튜터에서 저장한 단어", "tutor", Modifier.padding(vertical = 3.dp)) }
                        }
                    }
                }
            }
            if (external) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🌐 ${app.label}는 Chrome 창에서 열려 있어요", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Chrome에서 로그인하면 다음부터는 바로 열려요(구글 계정 로그인 가능). 대화가 끝나면 마지막 답을 복사하고 이 화면으로 돌아와 위의 '결과 가져오기'를 누르세요.",
                        style = MaterialTheme.typography.bodyMedium)
                    if (prompt != null) Text(
                        "💡 Claude 앱에서 빨간 '주의하세요' 경고가 보여도 괜찮아요. 이 앱이 넣은 수업 지시문이라 안전해요. 주황색 ↑(보내기)를 누르면 수업이 시작돼요.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                    )
                    Button(onClick = { prompt?.let { copyText(context, it) }; openInChrome(context, startUrl) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text("Chrome에서 ${app.label} 다시 열기")
                    }
                    if (app != SubApp.GEMINI) TextButton(onClick = { c.settings.update { it.withExternal(app.id, false) } }) {
                        Text("앱 안 화면으로 쓰기 (이메일 로그인만 가능)")
                    }
                }
                return@Column
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        // 일부 사이트가 앱 내장 브라우저 표시(wv)를 막아서 일반 모바일 브라우저처럼 표시
                        settings.userAgentString = settings.userAgentString.replace("; wv", "")
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                canGoBack = view?.canGoBack() == true
                            }

                            // 구글 로그인 페이지로 가려 하면 막히기 전에 안내 (구글은 앱 안 화면 로그인을 차단)
                            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                if (isGoogleSignIn(request?.url?.toString())) { googleBlocked = true; return true }
                                return false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            // 사이트의 음성 입력(마이크) 허용
                            override fun onPermissionRequest(request: PermissionRequest) {
                                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE !in request.resources) { request.deny(); return }
                                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                                } else {
                                    pendingMic = request
                                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        loadUrl(startUrl)
                        webView = this
                    }
                },
            )
        }
    }
}


@Composable
private fun LaunchedOnce(key: Any?, block: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) { block() }
}

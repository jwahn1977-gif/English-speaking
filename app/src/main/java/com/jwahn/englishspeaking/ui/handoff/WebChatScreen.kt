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

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingMic?.let { if (granted) it.grant(it.resources) else it.deny() }
        pendingMic = null
    }

    LaunchedOnce(prompt) { if (prompt != null) copyText(context, prompt) }

    BackHandler(enabled = canGoBack) { webView?.goBack() }
    DisposableEffect(Unit) { onDispose { CookieManager.getInstance().flush() } }

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
                Text(
                    if (prompt != null)
                        "수업 지시문이 입력칸에 들어가 있어요(안 보이면 길게 눌러 붙여넣기). 영어로 대화하고, 끝나면 END → 마지막 답 복사 → 위의 '결과 가져오기'."
                    else "처음 한 번 로그인하면 다음부터는 바로 열려요. 구글 로그인이 막히면 이메일로 로그인하거나, ⋮ 메뉴에서 앱으로 여세요.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                )
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

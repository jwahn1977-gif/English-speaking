package com.jwahn.englishspeaking.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.jwahn.englishspeaking.AppContainer

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("no container") }

/** AppContainer를 받아 ViewModel을 만드는 헬퍼 */
@Composable
inline fun <reified T : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> T): T {
    val c = LocalContainer.current
    return viewModel(key = key, factory = viewModelFactory { initializer { create(c) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = fab,
    ) { pad -> content(Modifier.padding(pad).fillMaxSize()) }
}

@Composable
fun SpeakIcon(text: String, modifier: Modifier = Modifier, rate: Float? = null, tint: Color = MaterialTheme.colorScheme.primary) {
    val tts = LocalContainer.current.tts
    IconButton(onClick = { tts.speak(text, rate) }, modifier = modifier) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "듣기", tint = tint)
    }
}

@Composable
fun Loading(message: String = "AI가 만드는 중…", modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorCard(message: String, onRetry: (() -> Unit)? = null, onSettings: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("문제가 생겼어요", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodySmall)
            Row {
                if (onRetry != null) TextButton(onClick = onRetry) { Text("다시 시도") }
                if (onSettings != null) TextButton(onClick = onSettings) { Text("AI 연결 설정") }
            }
        }
    }
}

@Composable
fun RingGauge(percent: Int, label: String, color: Color, size: Dp = 64.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.size(size),
                color = color,
                strokeWidth = 6.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
            Text("$percent%", fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 패턴의 ~ 부분을 강조 */
@Composable
fun PatternText(template: String, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium) {
    val color = MaterialTheme.colorScheme.secondary
    Text(
        buildAnnotatedString {
            template.split("~").forEachIndexed { i, part ->
                if (i > 0) withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append("~") }
                append(part)
            }
        },
        style = style, fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun EmptyState(emoji: String, title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(body, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        action()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

fun scoreColor(score: Int): Color = when {
    score >= 85 -> Color(0xFF22A55B)
    score >= 60 -> Color(0xFFE09A00)
    else -> Color(0xFFE5484D)
}

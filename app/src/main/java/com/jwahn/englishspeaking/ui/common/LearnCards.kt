package com.jwahn.englishspeaking.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jwahn.englishspeaking.data.PatternDraft
import com.jwahn.englishspeaking.data.WordDraft
import kotlinx.coroutines.launch

/** 교정 카드: 원래 문장 → 더 자연스러운 문장 */
@Composable
fun CorrectionCard(original: String, better: String, why: String, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("✏️ 더 자연스럽게", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (original.isNotBlank()) Text(original, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(better, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                SpeakIcon(better)
            }
            if (why.isNotBlank()) Text(why, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 저장 버튼이 붙은 패턴 카드 */
@Composable
fun PatternSaveCard(p: PatternDraft, source: String, modifier: Modifier = Modifier) {
    val repo = LocalContainer.current.repo
    val scope = rememberCoroutineScope()
    var saved by remember(p.pattern) { mutableStateOf(false) }
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🧩 패턴", style = MaterialTheme.typography.labelSmall)
                PatternText(p.pattern, MaterialTheme.typography.titleSmall)
                if (p.meaningKo.isNotBlank()) Text(p.meaningKo, style = MaterialTheme.typography.bodySmall)
                p.examples.firstOrNull()?.let { (en, ko) ->
                    Spacer(Modifier.height(4.dp))
                    Text(en, style = MaterialTheme.typography.bodyMedium)
                    if (ko.isNotBlank()) Text(ko, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            SaveButton(saved) {
                scope.launch { repo.savePattern(p, source); saved = true }
            }
        }
    }
}

/** 저장 버튼이 붙은 단어 카드 */
@Composable
fun WordSaveCard(w: WordDraft, chapterTitle: String, source: String, modifier: Modifier = Modifier) {
    val repo = LocalContainer.current.repo
    val scope = rememberCoroutineScope()
    var saved by remember(w.word) { mutableStateOf(false) }
    Card(modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(w.word, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (w.ipa.isNotBlank()) Text("  /${w.ipa}/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(w.meaningKo, style = MaterialTheme.typography.bodyMedium)
                if (w.exampleEn.isNotBlank()) Text(w.exampleEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SpeakIcon(w.word)
            SaveButton(saved) {
                scope.launch { repo.saveWord(w, chapterTitle, source); saved = true }
            }
        }
    }
}

@Composable
private fun SaveButton(saved: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = !saved) {
        Icon(if (saved) Icons.Default.Check else Icons.Default.StarBorder, null)
        Spacer(Modifier.width(4.dp))
        Text(if (saved) "저장됨" else "저장")
    }
}

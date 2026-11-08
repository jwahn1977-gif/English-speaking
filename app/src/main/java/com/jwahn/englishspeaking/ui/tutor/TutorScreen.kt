package com.jwahn.englishspeaking.ui.tutor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState as rss
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jwahn.englishspeaking.data.db.ScenarioEntity
import com.jwahn.englishspeaking.data.llm.Prompts
import com.jwahn.englishspeaking.domain.arr
import com.jwahn.englishspeaking.domain.str
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.ErrorCard
import com.jwahn.englishspeaking.ui.common.Loading
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SectionTitle
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val categories = linkedMapOf(
    "ROLEPLAY" to "롤플레이",
    "DAILY" to "일상 대화",
    "BUSINESS" to "비즈니스",
    "OPIC" to "OPIc",
    "ADVANCED" to "고급",
    "KIDS" to "어린이",
    "CUSTOM" to "나만의",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TutorScreen(nav: Nav) {
    val c = LocalContainer.current
    val scenarios by c.db.tutor().scenarios().collectAsState(initial = emptyList())
    val recent by c.db.tutor().recentConversations(15).collectAsState(initial = emptyList())
    var cat by rememberSaveable { mutableStateOf("ROLEPLAY") }
    var deleteTarget by remember { mutableStateOf<ScenarioEntity?>(null) }
    val scope = rememberCoroutineScope()

    Screen(
        "AI 튜터",
        fab = {
            ExtendedFloatingActionButton(onClick = nav::newScenario, icon = { Icon(Icons.Default.Add, "나만의 시나리오 만들기") }, text = { Text("나만의 시나리오") })
        },
    ) { mod ->
            LazyColumn(mod.padding(horizontal = 16.dp)) {
                item {
                    Text("상황을 골라 AI와 영어로 대화해 보세요. 틀린 표현은 고쳐 주고, 좋은 패턴은 저장할 수 있어요.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { (k, v) -> FilterChip(selected = cat == k, onClick = { cat = k }, label = { Text(v) }) }
                    }
                }
                val list = scenarios.filter { it.category == cat || (cat == "CUSTOM" && !it.builtin) }
                if (list.isEmpty()) item {
                    Text("아직 없어요. 오른쪽 아래 '나만의 시나리오'로 원하는 상황을 만들어 보세요.", modifier = Modifier.padding(16.dp))
                }
                items(list, key = { it.id }) { s ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .combinedClickable(onClick = { nav.chat(s.id) }, onLongClick = { if (!s.builtin) deleteTarget = s }),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.emoji, fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, fontWeight = FontWeight.Bold)
                                Text(s.missions.lines().filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                            }
                        }
                    }
                }
                if (recent.isNotEmpty()) {
                    item { SectionTitle("최근 대화 (눌러서 이어하기)") }
                    items(recent, key = { "c${it.id}" }) { conv ->
                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { nav.chat(conv.scenarioId, conv.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Row(Modifier.padding(12.dp)) {
                                Text(conv.title, Modifier.weight(1f))
                                Text(SimpleDateFormat("M/d HH:mm", Locale.KOREA).format(Date(conv.startedAt)), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
    }

    deleteTarget?.let { t ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("시나리오 삭제") },
            text = { Text("'${t.title}'를 삭제할까요?") },
            confirmButton = { TextButton(onClick = { scope.launch { c.db.tutor().deleteScenario(t.id) }; deleteTarget = null }) { Text("삭제") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("취소") } },
        )
    }
}

@Composable
fun NewScenarioScreen(nav: Nav) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var desc by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf("💬") }
    var persona by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf("") }
    var missions by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Screen("나만의 시나리오", onBack = nav::back) { mod ->
        Column(mod.verticalScroll(rss()).padding(16.dp)) {
            Text("① 연습하고 싶은 상황을 한국어로 적고 AI에게 맡기거나, ② 아래 칸을 직접 채우세요.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                desc, { desc = it }, label = { Text("상황 설명 (예: 아이 학교 담임 선생님과 상담)") },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            Button(
                enabled = desc.isNotBlank() && !loading,
                onClick = {
                    loading = true; error = null
                    scope.launch {
                        runCatching { c.llm.quickJson(Prompts.customScenario(desc), 1200) }
                            .onSuccess { o ->
                                title = o.str("title"); emoji = o.str("emoji").ifBlank { "💬" }
                                persona = o.str("persona"); goal = o.str("goal")
                                missions = o.arr("missions").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.joinToString("\n")
                            }
                            .onFailure { error = it.message }
                        loading = false
                    }
                },
                modifier = Modifier.padding(vertical = 8.dp),
            ) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(6.dp)); Text("AI로 시나리오 만들기") }
            if (loading) Loading()
            error?.let { ErrorCard(it, onSettings = nav::settings) }

            OutlinedTextField(title, { title = it }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(emoji, { emoji = it.take(4) }, label = { Text("이모지") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(persona, { persona = it }, label = { Text("AI 역할 (영어 권장, 예: a friendly landlord named Tom)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(goal, { goal = it }, label = { Text("대화 목표") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(missions, { missions = it }, label = { Text("미션 (한 줄에 하나)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = title.isNotBlank() && persona.isNotBlank(),
                onClick = {
                    scope.launch {
                        val id = c.db.tutor().insertScenario(
                            ScenarioEntity(category = "CUSTOM", title = title, emoji = emoji, persona = persona,
                                goal = goal.ifBlank { "Have a natural conversation about: $title" }, missions = missions, builtin = false)
                        )
                        nav.back()
                        nav.chat(id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("저장하고 대화 시작") }
        }
    }
}

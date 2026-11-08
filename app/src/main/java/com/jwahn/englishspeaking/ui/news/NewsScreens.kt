package com.jwahn.englishspeaking.ui.news

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jwahn.englishspeaking.AppContainer
import com.jwahn.englishspeaking.data.db.NewsFeedEntity
import com.jwahn.englishspeaking.data.db.NewsLessonEntity
import com.jwahn.englishspeaking.data.llm.Prompts
import com.jwahn.englishspeaking.data.news.NewsItem
import com.jwahn.englishspeaking.data.settings.levelPrompt
import com.jwahn.englishspeaking.domain.JsonText
import com.jwahn.englishspeaking.domain.Levels
import com.jwahn.englishspeaking.domain.Sentences
import com.jwahn.englishspeaking.domain.str
import com.jwahn.englishspeaking.ui.Nav
import com.jwahn.englishspeaking.ui.common.EmptyState
import com.jwahn.englishspeaking.ui.common.ErrorCard
import com.jwahn.englishspeaking.ui.common.Loading
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.common.Screen
import com.jwahn.englishspeaking.ui.common.SectionTitle
import com.jwahn.englishspeaking.ui.common.appViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NewsUi(
    val selected: Long = 0, // 0 = 전체
    val items: List<NewsItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class NewsViewModel(private val c: AppContainer) : ViewModel() {
    val state = MutableStateFlow(NewsUi())
    private val cache = mutableMapOf<Long, List<NewsItem>>()

    init { load(0) }

    fun load(feedId: Long, force: Boolean = false) {
        state.update { it.copy(selected = feedId, error = null) }
        if (!force) cache[feedId]?.let { cached -> state.update { it.copy(items = cached) }; return }
        state.update { it.copy(loading = true, items = emptyList()) }
        viewModelScope.launch {
            val feeds = c.db.news().feedsOnce().filter { it.enabled && (feedId == 0L || it.id == feedId) }
            val results = feeds.map { f -> async { runCatching { c.news.fetchFeed(f.name, f.url) } } }.awaitAll()
            val items = results.flatMap { it.getOrDefault(emptyList()) }
                .let { list -> if (feedId == 0L) interleave(feeds.map { f -> list.filter { it.feed == f.name } }) else list }
                .distinctBy { it.link } // 여러 피드에 같은 기사가 있을 수 있음
            val err = results.firstNotNullOfOrNull { it.exceptionOrNull() }?.message
            cache[feedId] = items
            state.update { it.copy(loading = false, items = items, error = if (items.isEmpty()) (err ?: "뉴스를 불러오지 못했어요.") else null) }
        }
    }

    private fun interleave(lists: List<List<NewsItem>>): List<NewsItem> {
        val out = mutableListOf<NewsItem>()
        val max = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until max) lists.forEach { l -> l.getOrNull(i)?.let { out += it } }
        return out
    }
}

@Composable
fun NewsScreen(nav: Nav) {
    val c = LocalContainer.current
    val vm = appViewModel { NewsViewModel(it) }
    val st by vm.state.collectAsState()
    val feeds by c.db.news().feeds().collectAsState(initial = emptyList())
    val lessons by c.db.news().recentLessons().collectAsState(initial = emptyList())
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Screen(
        "뉴스 듣기",
        actions = {
            IconButton(onClick = nav::newsPaste) { Icon(Icons.Default.ContentPaste, "직접 붙여넣기") }
            IconButton(onClick = nav::feeds) { Icon(Icons.Default.RssFeed, "뉴스 피드 관리") }
            IconButton(onClick = { vm.load(st.selected, force = true) }) { Icon(Icons.Default.Refresh, "새로고침") }
        },
    ) { mod ->
        Column(mod) {
            NowPlayingBar(onOpen = { nav.newsLesson(it) })
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("최신 뉴스") })
                Tab(tab == 1, { tab = 1 }, text = { Text("들은 기사 (${lessons.size})") })
            }
            if (tab == 0) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(st.selected == 0L, { vm.load(0) }, { Text("전체") })
                    feeds.filter { it.enabled }.forEach { f -> FilterChip(st.selected == f.id, { vm.load(f.id) }, { Text(f.name) }) }
                }
                LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                    item {
                        Text("기사를 누르면 문장별로 나눠서 읽어 주고, 해석·핵심 단어·패턴을 함께 보여줘요.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (st.loading) Loading("뉴스 불러오는 중…")
                        st.error?.let { ErrorCard(it, onRetry = { vm.load(st.selected, true) }) }
                    }
                    items(st.items, key = { it.link }) { n ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClickLabel = "이 기사 듣기") { nav.newsPrepare(n.link, n.title) }) {
                            Column(Modifier.padding(14.dp)) {
                                Text(n.title, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                if (n.description.isNotBlank()) Text(n.description, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${n.feed} · ${n.pubDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            } else {
                LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                    if (lessons.isEmpty()) item { EmptyState("🎧", "아직 들은 기사가 없어요", "최신 뉴스에서 기사를 골라 보세요.") }
                    items(lessons, key = { it.id }) { l -> LessonRow(l, onOpen = { nav.newsLesson(l.id) }) }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(l: NewsLessonEntity, onOpen: () -> Unit) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClickLabel = "다시 듣기", onClick = onOpen)) {
        Row(Modifier.padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(l.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text((if (l.mode == "EASY") "쉬운 영어" else "원문") + " · " +
                        SimpleDateFormat("M/d HH:mm", Locale.KOREA).format(Date(l.createdAt)),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { scope.launch { c.db.news().deleteLesson(l.id) } }) { Icon(Icons.Default.Delete, "삭제") }
        }
    }
}

/** 기사 본문을 가져와 듣기용 레슨으로 만든다. url이 비어 있으면 직접 붙여넣기 모드 */
@Composable
fun NewsPrepareScreen(nav: Nav, url: String, title: String) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val settings by c.settings.state.collectAsState()
    val paste = url.isBlank()
    var text by rememberSaveable { mutableStateOf("") }
    var fetching by remember { mutableStateOf(!paste) }
    var error by remember { mutableStateOf<String?>(null) }
    // 초보는 쉬운 영어로 다시 쓴 기사가 기본
    var mode by rememberSaveable { mutableStateOf(if (settings.isBeginner) "EASY" else "ORIGINAL") }
    var building by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (!paste) {
            runCatching { c.news.fetchArticleText(url) }
                .onSuccess { text = it }
                .onFailure { error = it.message }
            fetching = false
        }
    }

    Screen(if (paste) "기사 붙여넣기" else "뉴스 듣기 준비", onBack = nav::back) { mod ->
        Column(mod.verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (!paste) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (fetching) Loading("기사 본문 가져오는 중…")
            error?.let { ErrorCard(it) }
            if (paste || error != null) {
                OutlinedTextField(
                    text, { text = it }, label = { Text("영어 기사·글을 붙여넣으세요") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), minLines = 6, maxLines = 14,
                )
            } else if (text.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text.take(700) + if (text.length > 700) " …" else "", Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
                Text("약 ${Sentences.split(text).size}문장", style = MaterialTheme.typography.labelSmall)
            }

            SectionTitle("듣기 방식")
            ModeRow("ORIGINAL", mode, "원문 그대로", "실제 기사 문장을 그대로 듣기 (앞부분 최대 20문장) + 한국어 해석") { mode = it }
            ModeRow("EASY", mode, "쉬운 영어로 바꿔 듣기", "내 수준에 맞게 AI가 다시 쓴 기사 (AI 연결 필요)") { mode = it }
            if (!settings.anyConnected) Text(
                "AI가 연결되어 있지 않아 해석·단어·패턴 없이 원문만 읽어 줘요.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = text.length > 50 && !building && !fetching,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    building = true; error = null
                    scope.launch {
                        val key = url.ifBlank { "paste:" + text.take(80).hashCode() }
                        val cached = c.db.news().lessonFor(key, mode)
                        if (cached != null) { building = false; nav.newsLesson(cached.id, replace = true); return@launch }
                        runCatching {
                            val json = if (settings.anyConnected) {
                                val level = settings.levelPrompt(c.db.tutor().usedWordCountOnce())
                                c.llm.quickJson(Prompts.newsLesson(text.take(7000), mode, level), 7000).toString()
                            } else {
                                val sentences = Sentences.split(text).take(30)
                                buildJsonObject {
                                    put("title", title.ifBlank { sentences.firstOrNull().orEmpty().take(80) })
                                    putJsonArray("sentences") { sentences.forEach { s -> addJsonObject { put("en", s); put("ko", "") } } }
                                }.toString()
                            }
                            val lessonTitle = title.ifBlank {
                                JsonText.parseObject(json)?.str("title").orEmpty().ifBlank { "붙여넣은 글" }
                            }
                            c.db.news().insertLesson(NewsLessonEntity(url = key, title = lessonTitle, mode = if (settings.anyConnected) mode else "ORIGINAL", json = json))
                        }.onSuccess { id -> nav.newsLesson(id, replace = true) }
                            .onFailure { error = it.message }
                        building = false
                    }
                },
            ) { Text(if (building) "준비 중…" else "듣기 시작") }
            if (building) Loading("문장을 나누고 해석하는 중… (기사 길이에 따라 30초 정도)")
        }
    }
}

@Composable
private fun ModeRow(value: String, selected: String, title: String, desc: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected == value, onClick = { onSelect(value) })
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FeedsScreen(nav: Nav) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val feeds by c.db.news().feeds().collectAsState(initial = emptyList())
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var testMsg by remember { mutableStateOf<String?>(null) }

    Screen("뉴스 피드 관리", onBack = nav::back) { mod ->
        LazyColumn(mod.padding(horizontal = 16.dp)) {
            item {
                Text("영어 뉴스 사이트의 RSS 주소를 추가할 수 있어요. 끄면 '전체'에서 빠져요.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(feeds, key = { it.id }) { f -> FeedRow(f, onToggle = { scope.launch { c.db.news().updateFeed(f.copy(enabled = it)) } },
                onDelete = { scope.launch { c.db.news().deleteFeed(f.id) } }) }
            item {
                SectionTitle("피드 추가")
                OutlinedTextField(name, { name = it }, label = { Text("이름 (예: CNN Top)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(url, { url = it.trim() }, label = { Text("RSS 주소 (https://…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    TextButton(enabled = url.startsWith("http"), onClick = {
                        testMsg = "확인 중…"
                        scope.launch {
                            testMsg = runCatching { c.news.fetchFeed(name, url) }
                                .fold({ "✅ 기사 ${it.size}개를 찾았어요" }, { "❌ ${it.message}" })
                        }
                    }) { Text("확인") }
                    Button(enabled = name.isNotBlank() && url.startsWith("http"), onClick = {
                        scope.launch { c.db.news().insertFeed(NewsFeedEntity(name = name.trim(), url = url)); name = ""; url = ""; testMsg = null }
                    }) { Text("추가") }
                }
                testMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FeedRow(f: NewsFeedEntity, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(f.name, fontWeight = FontWeight.SemiBold)
                Text(f.url, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = f.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "삭제") }
        }
    }
    Spacer(Modifier.width(1.dp))
}

/** 다른 화면에 있어도 지금 듣는 기사를 보여주고 바로 조작 */
@Composable
fun NowPlayingBar(onOpen: (Long) -> Unit) {
    val player = LocalContainer.current.newsPlayer
    val ps by player.state.collectAsState()
    if (!ps.active) return
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable(onClickLabel = "듣는 기사 열기") { onOpen(ps.lessonId) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text((if (ps.playing) "🎧 듣는 중 · " else "⏸ 일시정지 · ") + "${ps.index + 1}/${ps.sentences.size}",
                    style = MaterialTheme.typography.labelSmall)
                Text(ps.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = player::toggle) {
                Icon(if (ps.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (ps.playing) "일시정지" else "재생")
            }
            IconButton(onClick = player::close) { Icon(Icons.Default.Close, "듣기 종료") }
        }
    }
}

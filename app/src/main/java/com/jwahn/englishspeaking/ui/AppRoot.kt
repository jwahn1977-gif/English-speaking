package com.jwahn.englishspeaking.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jwahn.englishspeaking.AppContainer
import com.jwahn.englishspeaking.ui.common.LocalContainer
import com.jwahn.englishspeaking.ui.home.HomeScreen
import com.jwahn.englishspeaking.ui.home.OnboardingScreen
import com.jwahn.englishspeaking.ui.home.TodayScreen
import com.jwahn.englishspeaking.ui.news.FeedsScreen
import com.jwahn.englishspeaking.ui.news.NewsLessonScreen
import com.jwahn.englishspeaking.ui.news.NewsPrepareScreen
import com.jwahn.englishspeaking.ui.news.NewsScreen
import com.jwahn.englishspeaking.ui.pattern.PatternDetailScreen
import com.jwahn.englishspeaking.ui.pattern.PatternGenScreen
import com.jwahn.englishspeaking.ui.pattern.PatternReviewScreen
import com.jwahn.englishspeaking.ui.pattern.PatternsScreen
import com.jwahn.englishspeaking.ui.settings.SettingsScreen
import com.jwahn.englishspeaking.ui.tutor.ChatScreen
import com.jwahn.englishspeaking.ui.tutor.NewScenarioScreen
import com.jwahn.englishspeaking.ui.tutor.TutorScreen
import com.jwahn.englishspeaking.ui.vocab.ChapterScreen
import com.jwahn.englishspeaking.ui.vocab.QuizScreen
import com.jwahn.englishspeaking.ui.vocab.StudyScreen
import com.jwahn.englishspeaking.ui.vocab.VocabScreen
import com.jwahn.englishspeaking.ui.vocab.WordGenScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "홈", Icons.Default.Home),
    Tab("tutor", "AI 튜터", Icons.Default.RecordVoiceOver),
    Tab("vocab", "단어장", Icons.AutoMirrored.Filled.MenuBook),
    Tab("news", "뉴스 듣기", Icons.Default.Headphones),
    Tab("patterns", "패턴", Icons.Default.ViewQuilt),
)

/** 화면 이동 모음 */
class Nav(private val nav: NavHostController) {
    private val main = Handler(Looper.getMainLooper())

    /** 네비게이션은 반드시 메인 스레드에서 */
    private fun go(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else main.post(action)
    }

    fun back() = go { nav.popBackStack() }
    fun tab(route: String) = go {
        nav.navigate(route) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun settings() = go { nav.navigate("settings") }
    fun chat(scenarioId: Long, conversationId: Long = 0) = go { nav.navigate("chat/$scenarioId?conv=$conversationId") }
    fun newScenario() = go { nav.navigate("scenario_new") }
    fun chapter(id: Long) = go { nav.navigate("chapter/$id") }
    /** mode: chapter / due / bookmark */
    fun study(mode: String, id: Long = 0) = go { nav.navigate("study/$mode/$id") }
    fun quiz(id: Long) = go { nav.navigate("quiz/$id") }
    fun wordGen() = go { nav.navigate("wordgen") }
    fun pattern(id: Long) = go { nav.navigate("pattern/$id") }
    fun patternGen() = go { nav.navigate("patterngen") }
    fun patternReview() = go { nav.navigate("patternreview") }
    fun newsPrepare(url: String, title: String) =
        go { nav.navigate("newsprep?url=${Uri.encode(url)}&title=${Uri.encode(title)}") }
    fun newsPaste() = go { nav.navigate("newsprep?url=&title=") }
    fun newsLesson(id: Long, replace: Boolean = false) = go {
        nav.navigate("newslesson/$id") {
            if (replace) popUpTo("newsprep?url={url}&title={title}") { inclusive = true }
        }
    }
    fun feeds() = go { nav.navigate("feeds") }
    fun today() = go { nav.navigate("today") }
    fun handoffTutor(scenarioId: Long) = go { nav.navigate("handoff/$scenarioId") }
    /** 공식 서비스(Claude·ChatGPT·Gemini)를 앱 안 화면으로 연다. prompt가 없으면 로그인 화면 */
    fun webChat(app: com.jwahn.englishspeaking.ui.handoff.SubApp, prompt: String? = null,
                onImport: ((com.jwahn.englishspeaking.ui.handoff.Imported) -> Unit)? = null) {
        com.jwahn.englishspeaking.ui.handoff.WebChatRequest.prompt = prompt
        com.jwahn.englishspeaking.ui.handoff.WebChatRequest.onImport = onImport
        go { nav.navigate("webchat/${app.id}") }
    }
    fun finishOnboarding() = go {
        nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
    }
}

@Composable
fun AppRoot(container: AppContainer) {
    CompositionLocalProvider(LocalContainer provides container) {
        val nav = rememberNavController()
        val n = Nav(nav)
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val showBar = tabs.any { it.route == route }
        Scaffold(
            bottomBar = {
                if (showBar) NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { n.tab(t.route) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        ) { pad ->
            val start = remember { if (container.settings.current.onboarded) "home" else "onboarding" }
            NavHost(nav, startDestination = start, modifier = Modifier.padding(pad)) {
                composable("onboarding") { OnboardingScreen(onDone = n::finishOnboarding, onLogin = { n.webChat(it) }) }
                composable("today") { TodayScreen(n) }
                composable("webchat/{app}") { e ->
                    com.jwahn.englishspeaking.ui.handoff.WebChatScreen(n, e.arguments?.getString("app").orEmpty())
                }
                composable("handoff/{sid}", arguments = listOf(navArgument("sid") { type = NavType.LongType })) { e ->
                    com.jwahn.englishspeaking.ui.handoff.HandoffTutorScreen(n, e.arguments!!.getLong("sid"))
                }
                composable("home") { HomeScreen(n) }
                composable("tutor") { TutorScreen(n) }
                composable("vocab") { VocabScreen(n) }
                composable("news") { NewsScreen(n) }
                composable("patterns") { PatternsScreen(n) }
                composable("settings") { SettingsScreen(n) }
                composable(
                    "chat/{sid}?conv={conv}",
                    arguments = listOf(
                        navArgument("sid") { type = NavType.LongType },
                        navArgument("conv") { type = NavType.LongType; defaultValue = 0L },
                    ),
                ) { e ->
                    ChatScreen(n, e.arguments!!.getLong("sid"), e.arguments!!.getLong("conv"))
                }
                composable("scenario_new") { NewScenarioScreen(n) }
                composable("chapter/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    ChapterScreen(n, e.arguments!!.getLong("id"))
                }
                composable(
                    "study/{mode}/{id}",
                    arguments = listOf(navArgument("mode") { type = NavType.StringType }, navArgument("id") { type = NavType.LongType }),
                ) { e ->
                    StudyScreen(n, e.arguments!!.getString("mode")!!, e.arguments!!.getLong("id"))
                }
                composable("quiz/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    QuizScreen(n, e.arguments!!.getLong("id"))
                }
                composable("wordgen") { WordGenScreen(n) }
                composable("pattern/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    PatternDetailScreen(n, e.arguments!!.getLong("id"))
                }
                composable("patterngen") { PatternGenScreen(n) }
                composable("patternreview") { PatternReviewScreen(n) }
                composable(
                    "newsprep?url={url}&title={title}",
                    arguments = listOf(
                        navArgument("url") { type = NavType.StringType; defaultValue = "" },
                        navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) { e ->
                    NewsPrepareScreen(n, e.arguments?.getString("url").orEmpty(), e.arguments?.getString("title").orEmpty())
                }
                composable("newslesson/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                    NewsLessonScreen(n, e.arguments!!.getLong("id"))
                }
                composable("feeds") { FeedsScreen(n) }
            }
        }
    }
}

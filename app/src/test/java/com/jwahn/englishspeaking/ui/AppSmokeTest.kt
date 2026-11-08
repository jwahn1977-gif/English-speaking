package com.jwahn.englishspeaking.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.jwahn.englishspeaking.App
import com.jwahn.englishspeaking.AppContainer
import com.jwahn.englishspeaking.MainActivity
import com.jwahn.englishspeaking.data.settings.Provider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.addJsonObject
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/** 앱 전체 흐름 점검: 실제 화면을 띄우고, AI 응답은 가짜 서버(인터셉터)로 대신한다. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val requests = CopyOnWriteArrayList<String>()

    private val fakeAi = """
        {"reply":"Hi there! What can I get for you today?","reply_ko":"안녕하세요! 무엇을 드릴까요?",
         "correction":{"original":"I want a coffee","better":"I'd like a coffee, please.","why_ko":"더 공손한 표현이에요"},
         "patterns":[{"pattern":"I'd like ~, please","meaning_ko":"~ 주세요","example_en":"I'd like a latte, please.","example_ko":"라테 주세요."}],
         "words":[{"word":"barista","ipa":"bəˈriːstə","meaning_ko":"바리스타","example_en":"The barista smiled.","example_ko":"바리스타가 웃었다."}],
         "missions_done":[0],
         "suggestions":[{"en":"A latte, please.","ko":"라테 주세요."},{"en":"Can I get a small latte to go?","ko":"작은 라테 포장 되나요?"}],
         "summary_ko":"주문을 잘 마쳤어요.","good_ko":["끝까지 영어로 말했어요"],
         "improvements":[{"original":"I want a coffee","better":"I'd like a coffee, please.","why_ko":"공손하게"}],
         "score":85,
         "title":"City opens new park","title_ko":"시, 새 공원 개장","summary_ko2":"",
         "sentences":[{"en":"The city opened a new park on Monday.","ko":"시가 월요일에 새 공원을 열었다."},{"en":"Many families visited it.","ko":"많은 가족이 방문했다."}],
         "questions":[{"q":"What did the city open?","a":"A new park"}]}
    """.trimIndent()

    private val rss = """<?xml version="1.0"?><rss version="2.0"><channel><title>Test</title>
        <item><title>City opens new park</title><link>https://example.com/news/1</link>
        <description><![CDATA[<p>A new park opened downtown.</p>]]></description><pubDate>Sun, 08 Nov 2026 09:00:00 GMT</pubDate></item>
        </channel></rss>"""

    private val article = "<html><body><article>" + (1..4).joinToString("") {
        "<p>Paragraph $it. The city opened a new park on Monday and many families came to enjoy the sunny weather together.</p>"
    } + "</article></body></html>"

    private fun respond(chain: Interceptor.Chain, body: String, type: String) = Response.Builder()
        .request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
        .body(body.toResponseBody(type.toMediaType())).build()

    private val container: AppContainer get() = (rule.activity.application as App).container

    @Before
    fun setUp() {
        AppContainer.testInterceptor = Interceptor { chain ->
            val url = chain.request().url
            requests += url.toString()
            when {
                url.host == "api.anthropic.com" -> respond(chain, buildJsonObject {
                    put("stop_reason", "end_turn")
                    put("content", buildJsonArray { addJsonObject { put("type", "text"); put("text", fakeAi) } })
                }.toString(), "application/json")
                url.toString().contains("rss") || url.toString().contains("feeds") -> respond(chain, rss, "application/xml")
                url.host == "example.com" -> respond(chain, article, "text/html")
                url.host == "generativelanguage.googleapis.com" && url.encodedPath.endsWith("/models") -> respond(chain,
                    """{"models":[{"name":"models/gemini-test","supportedGenerationMethods":["generateContent"]}]}""", "application/json")
                else -> Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(404).message("NF")
                    .body("".toResponseBody(null)).build()
            }
        }
    }

    @After
    fun tearDown() { AppContainer.testInterceptor = null }

    private fun waitText(text: String, substring: Boolean = false, timeout: Long = 15_000) {
        try {
            rule.waitUntil(timeout) { rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            println("=== '$text' 를 찾지 못함. 현재 화면:\n" + rule.onRoot(useUnmergedTree = false).printToString(maxDepth = 30))
            println("=== 요청: $requests")
            throw e
        }
    }

    /** 화면 밖(목록 아래쪽)에 있으면 스크롤해서 찾는다 */
    private fun scrollTo(text: String, substring: Boolean = false) {
        val found = runCatching {
            rule.waitUntil(3_000) { rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }
        }.isSuccess
        if (!found) runCatching {
            rule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text, substring = substring))
            rule.waitForIdle()
        }
    }

    /** 첫 실행 안내를 기본값으로 통과 */
    private fun passOnboarding(learner: String? = null) {
        waitText("반가워요", substring = true)
        learner?.let { clickText(it) }
        repeat(3) { clickText("다음") }
        clickText("시작하기", substring = true)
        waitText("오늘의 공부 10분", substring = true)
    }

    private fun clickText(text: String, substring: Boolean = false) {
        scrollTo(text, substring)
        waitText(text, substring)
        val node = rule.onAllNodesWithText(text, substring = substring).onFirst()
        runCatching { node.performScrollTo() }
        // 화면 밖에 걸쳐 있어도 확실히 누르도록 클릭 동작을 직접 실행
        runCatching { node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick) }
            .onFailure { node.performClick() }
        rule.waitForIdle()
    }

    /** 하단 탭 이동 */
    private fun tab(label: String) {
        val m = hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        rule.waitUntil(15_000) { rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(m).onFirst().performClick()
        rule.waitForIdle()
    }

    private fun connectClaude() {
        container.settings.update { it.copy(keys = it.keys + (Provider.CLAUDE to "sk-ant-test"), defaultProvider = Provider.CLAUDE) }
    }

    @Test
    fun tabs_vocab_study_and_patterns() {
        passOnboarding()
        waitText("AI를 연결해 주세요", substring = true)
        tab("단어장")
        waitText("왕초보 과정", substring = true)
        clickText("일상생활")
        waitText("breakfast")
        clickText("카드 학습")
        // 처음 보는 단어는 뜻이 바로 보인다
        waitText("아침 식사")
        clickText("✓ 알아요")
        waitText("commute")
        runBlocking {
            val w = container.db.vocab().findWord("breakfast")!!
            assertEquals(1, w.box)
            assertEquals(1, w.correct)
        }
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        tab("패턴")
        scrollTo("I'm looking for ", substring = true)
        waitText("I'm looking for ", substring = true)
        tab("AI 튜터")
        waitText("카페에서 주문하기")
        tab("홈")
        rule.onNodeWithContentDescription("설정").performClick()
        waitText("기본으로 쓸 AI")
    }

    @Test
    fun tutor_chat_correction_save_and_report() {
        passOnboarding()
        connectClaude()
        tab("AI 튜터")
        clickText("카페에서 주문하기")
        waitText("Hi there! What can I get for you today?")
        // 예시 답변: 누르면 입력창에 들어간다
        clickText("A latte, please.")
        rule.onNode(hasSetTextAction()).assert(hasText("A latte, please."))
        rule.onNode(hasSetTextAction()).performTextClearance()
        rule.onNode(hasSetTextAction()).performTextInput("I want a coffee")
        rule.onNodeWithContentDescription("보내기").performClick()
        rule.waitUntil(15_000) {
            runBlocking { container.db.tutor().recentConversations(1).first().firstOrNull()?.let { container.db.tutor().messages(it.id).size } ?: 0 } >= 3
        }
        waitText("I'd like a coffee, please.")
        rule.onAllNodesWithText("저장하기", substring = true).onFirst().performScrollTo().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("저장").onFirst().performClick()
        rule.waitUntil(10_000) { runBlocking { container.db.patterns().byTemplate("I'd like ~, please") != null } }
        runBlocking {
            assertTrue(container.db.tutor().usedWordCountOnce() >= 2) // want, coffee
        }
        clickText("종료·리포트")
        waitText("85점")
        waitText("주문을 잘 마쳤어요.")
        val claudeCalls = requests.count { it.contains("api.anthropic.com/v1/messages") }
        assertEquals(3, claudeCalls) // 첫 인사 + 답변 + 리포트
    }

    @Test
    fun news_listening_flow() {
        passOnboarding("자신 있어요")
        connectClaude()
        tab("뉴스 듣기")
        waitText("City opens new park")
        rule.onAllNodesWithText("City opens new park").onFirst().performClick()
        waitText("Paragraph 1", substring = true)
        clickText("듣기 시작")
        waitText("The city opened a new park on Monday.")
        waitText("시, 새 공원 개장")
        clickText("단어 1")
        waitText("barista")
        clickText("퀴즈 1")
        waitText("Q. What did the city open?")
        runBlocking {
            assertNotNull(container.db.news().lessonFor("https://example.com/news/1", "ORIGINAL"))
        }
        // 재생 → 백그라운드 서비스 시작 → 화면을 나가도 계속 재생 상태
        org.robolectric.Shadows.shadowOf(rule.activity.application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        clickText("문장 2")
        rule.onNodeWithContentDescription("재생").performClick()
        rule.waitForIdle()
        rule.waitUntil(5_000) { container.newsPlayer.state.value.playing }
        val started = org.robolectric.Shadows.shadowOf(rule.activity.application).nextStartedService
        assertEquals("com.jwahn.englishspeaking.news.NewsPlaybackService", started?.component?.className)
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        tab("홈")
        waitText("듣는 중", substring = true)
        assertTrue(container.newsPlayer.state.value.playing)
        rule.onNodeWithContentDescription("듣기 종료").performClick()
        rule.waitForIdle()
        assertTrue(!container.newsPlayer.state.value.active)
    }

    @Test
    fun reminder_worker_posts_notification_when_reviews_due() {
        org.robolectric.Shadows.shadowOf(rule.activity.application)
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        passOnboarding()
        runBlocking {
            val w = container.db.vocab().findWord("breakfast")!!
            container.db.vocab().updateWord(w.copy(box = 1, nextDue = 0))
            val worker = androidx.work.testing.TestListenableWorkerBuilder<com.jwahn.englishspeaking.reminder.ReminderWorker>(rule.activity.application).build()
            assertEquals(androidx.work.ListenableWorker.Result.success(), worker.doWork())
        }
        val nm = rule.activity.getSystemService(android.app.NotificationManager::class.java)
        val posted = org.robolectric.Shadows.shadowOf(nm).allNotifications
        assertEquals(1, posted.size)
        val text = posted[0].extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString()
        assertTrue(text, text.contains("단어 1개"))
    }

    @Test
    fun other_screens_open_and_ai_generation() {
        passOnboarding()
        connectClaude()
        // 퀴즈
        tab("단어장")
        clickText("음식과 식당")
        clickText("퀴즈")
        waitText("뜻을 고르세요")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        // AI 단어장 생성 → 저장
        tab("단어장")
        clickText("AI 생성")
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("카페")
        clickText("단어 만들기")
        waitText("단어장에 저장 (1개)")
        clickText("단어장에 저장 (1개)")
        waitText("barista")
        runBlocking { assertNotNull(container.db.vocab().findWord("barista")) }
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        // 패턴 상세 · AI 패턴 생성 · 패턴 복습
        tab("패턴")
        clickText("I'm looking for ", substring = true)
        waitText("I'm looking for a gift for my sister.")
        clickText("AI로 예문 5개 더 만들기")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        clickText("AI 생성")
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("주문")
        clickText("패턴 만들기")
        clickText("전체 저장 (1개)")
        waitText("모두 저장됨 ✅")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        clickText("패턴 말하기 복습", substring = true)
        waitText("정답 보기")
        clickText("정답 보기")
        clickText("말할 수 있어요")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        // 나만의 시나리오 (AI로 채우기)
        tab("AI 튜터")
        rule.onNodeWithContentDescription("나만의 시나리오 만들기", useUnmergedTree = true).performClick()
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("집주인과 수리 요청")
        clickText("AI로 시나리오 만들기")
        waitText("저장하고 대화 시작")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        // 뉴스 피드 관리 · 붙여넣기 화면
        tab("뉴스 듣기")
        rule.onNodeWithContentDescription("뉴스 피드 관리").performClick()
        waitText("BBC World")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithContentDescription("직접 붙여넣기").performClick()
        waitText("영어 기사·글을 붙여넣으세요")
    }

    /** 왕초보: 첫 실행 → 오늘의 공부 → 새 단어 5개(알파벳 소리) → 모르면 다시 나옴 */
    @Test
    fun beginner_onboarding_today_plan_and_requeue() {
        passOnboarding("처음 배워요")
        assertEquals(com.jwahn.englishspeaking.data.settings.Learner.STARTER, container.settings.current.learner)
        clickText("오늘의 공부 10분", substring = true)
        waitText("1. 복습하기")
        clickText("2. 새 단어 5개")
        waitText("새 단어 배우기")
        waitText("A a")
        waitText("1 / 5")
        clickText("✗ 몰라요")
        waitText("B b")
        waitText("2 / 6") // 모르는 카드가 뒤에 다시 들어갔다
        clickText("↩ 방금 답 되돌리기")
        waitText("A a")
        waitText("1 / 5")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        waitText("✓ 완료", substring = true)
        // 글자 크기 설정
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithContentDescription("설정").performClick()
        clickText("아주 크게")
        assertEquals(1.4f, container.settings.current.fontScale)
        waitText("웹 로그인으로 연결하기")
    }

    private fun nextOpenedUrl(): String {
        val shadowApp = org.robolectric.Shadows.shadowOf(rule.activity.application)
        rule.waitUntil(10_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            shadowApp.peekNextStartedActivity() != null
        }
        return shadowApp.nextStartedActivity?.data?.toString().orEmpty()
    }

    /** 구독 계정: Chrome에서 로그인 → 튜터가 Chrome으로 지시문과 함께 열림 → 결과 가져와 저장 */
    @Test
    fun subscription_login_chrome_and_import() {
        passOnboarding()
        tab("홈")
        rule.onNodeWithContentDescription("설정").performClick()
        clickText("Claude 로그인")
        waitText("Chrome 창에서 열려 있어요", substring = true)
        assertEquals("CLAUDE", container.settings.current.subscriptionApp)
        assertEquals("https://claude.ai/login", nextOpenedUrl())
        // 앱은 Chrome의 로그인 상태를 읽을 수 없으므로 사용자가 확인
        clickText("✅ 로그인했어요")
        assertTrue(container.settings.current.isLoginConfirmed("CLAUDE"))
        waitText("🔐 로그인됨")
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }

        tab("AI 튜터")
        clickText("카페에서 주문하기")
        waitText("Chrome 창에서 열려 있어요", substring = true)
        val url = nextOpenedUrl()
        assertEquals("https://claude.ai/", url) // 미리 채우기 링크 대신 채팅 첫 화면 + 복사
        val cm = rule.activity.getSystemService(android.content.ClipboardManager::class.java)
        assertTrue(cm.primaryClip!!.getItemAt(0).text.toString().contains("말해요 영어"))

        val reply = "잘했어요!\n```json\n" +
            "{\"patterns\":[{\"pattern\":\"Could I get ~?\",\"meaning_ko\":\"~ 주실 수 있나요?\",\"example_en\":\"Could I get a latte?\",\"example_ko\":\"라테 주실 수 있나요?\"}]," +
            "\"words\":[{\"word\":\"decaf\",\"ipa\":\"ˈdiːkæf\",\"meaning_ko\":\"디카페인 커피\",\"example_en\":\"A decaf, please.\",\"example_ko\":\"디카페인 주세요.\"}]}\n```"
        rule.activity.runOnUiThread { cm.setPrimaryClip(android.content.ClipData.newPlainText("r", reply)) }
        clickText("📋 결과 가져오기")
        waitText("가져온 결과: 패턴 1 · 단어 1")
        clickText("모두 저장")
        rule.waitUntil(10_000) { runBlocking { container.db.patterns().byTemplate("Could I get ~?") != null } }
        rule.waitUntil(10_000) { runBlocking { container.db.vocab().findWord("decaf") != null } }
    }

    /** 구글 로그인: Gemini는 Chrome 창으로 열리고, 비밀번호 찾기는 구글 계정 복구로 */
    @Test
    fun google_login_opens_chrome_and_password_recovery() {
        assertTrue(com.jwahn.englishspeaking.ui.handoff.isGoogleSignIn("https://accounts.google.com/v3/signin/identifier?x=1"))
        assertTrue(!com.jwahn.englishspeaking.ui.handoff.isGoogleSignIn("https://claude.ai/login"))
        passOnboarding()
        tab("홈")
        rule.onNodeWithContentDescription("설정").performClick()
        val shadowApp = org.robolectric.Shadows.shadowOf(rule.activity.application)
        clickText("구글 계정 비밀번호 찾기")
        rule.waitUntil(5_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            shadowApp.peekNextStartedActivity() != null
        }
        assertEquals("https://accounts.google.com/signin/recovery", shadowApp.nextStartedActivity?.data?.toString())
        assertTrue(container.settings.current.opensExternally("CLAUDE")) // 기본은 Chrome

        clickText("Gemini 로그인")
        waitText("Chrome 창에서 열려 있어요", substring = true)
        rule.waitUntil(5_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            shadowApp.peekNextStartedActivity() != null
        }
        assertEquals("https://gemini.google.com/app", shadowApp.nextStartedActivity?.data?.toString())
        clickText("로그인이 안 돼요? · 비밀번호 찾기")
        waitText("Gemini 로그인 도움말")
        clickText("구글 비밀번호 찾기")
        rule.waitUntil(5_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            shadowApp.peekNextStartedActivity() != null
        }
        assertEquals("https://accounts.google.com/signin/recovery", shadowApp.nextStartedActivity?.data?.toString())

        // 원하면 앱 안 화면으로 바꿀 수 있다 (Claude·ChatGPT만)
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        clickText("Claude 로그인")
        nextOpenedUrl()
        clickText("앱 안 화면으로 쓰기 (이메일 로그인만 가능)")
        assertTrue(!container.settings.current.opensExternally("CLAUDE"))
        assertEquals("https://claude.ai/login", org.robolectric.Shadows.shadowOf(findWebView()).lastLoadedUrl)
        assertTrue(container.settings.current.opensExternally("GEMINI"))
    }

    /** 무료 Gemini 키: Chrome에서 키 복사 → 돌아오면 자동 입력·연결 확인 → 튜터가 앱 안에서 동작 */
    @Test
    fun free_gemini_key_connects_and_tutor_runs_in_app() {
        passOnboarding()
        tab("홈")
        rule.onNodeWithContentDescription("설정").performClick()
        clickText("Chrome에서 무료 키 받기")
        assertEquals("https://aistudio.google.com/apikey", nextOpenedUrl())
        val cm = rule.activity.getSystemService(android.content.ClipboardManager::class.java)
        rule.activity.runOnUiThread { cm.setPrimaryClip(android.content.ClipData.newPlainText("k", "AIzaSyTestKey1234567890abcdefghijk")) }
        // Chrome에서 돌아온 것처럼 화면을 다시 띄운다
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        rule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        waitText("연결 확인 완료", substring = true)
        assertEquals(Provider.GEMINI, container.settings.current.defaultProvider)
        assertTrue(requests.any { it.contains("generativelanguage.googleapis.com/v1beta/models") })
        assertTrue(!container.settings.current.subscriptionOnly)
    }

    /** Claude 앱이 설치돼 있으면 링크 대신 앱 첫 화면을 바로 연다 (Claude Code 등 엉뚱한 화면 방지) */
    @Test
    fun installed_claude_app_opens_directly_with_prompt_copied() {
        val pm = org.robolectric.Shadows.shadowOf(rule.activity.packageManager)
        val launcher = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER).setPackage("com.anthropic.claude")
        pm.addResolveInfoForIntent(launcher, android.content.pm.ResolveInfo().apply {
            activityInfo = android.content.pm.ActivityInfo().apply {
                packageName = "com.anthropic.claude"; name = "com.anthropic.claude.MainActivity"
                applicationInfo = android.content.pm.ApplicationInfo().apply { packageName = "com.anthropic.claude" }
            }
        })
        passOnboarding()
        container.settings.update { it.copy(subscriptionApp = "CLAUDE") }
        tab("홈")
        waitText("Claude 앱으로 넘겨서 사용", substring = true)
        tab("AI 튜터")
        clickText("카페에서 주문하기")
        val shadowApp = org.robolectric.Shadows.shadowOf(rule.activity.application)
        rule.waitUntil(10_000) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            shadowApp.peekNextStartedActivity() != null
        }
        val started = shadowApp.nextStartedActivity!!
        assertEquals("com.anthropic.claude", started.`package` ?: started.component?.packageName)
        assertEquals(null, started.data) // 링크로 열지 않는다
        val cm = rule.activity.getSystemService(android.content.ClipboardManager::class.java)
        assertTrue(cm.primaryClip!!.getItemAt(0).text.toString().contains("말해요 영어"))
        waitText("Claude 앱으로 넘어갔어요", substring = true)
    }

    /** 자동 입력이 안 될 때: 붙여넣기 버튼 → 연결하기 */
    @Test
    fun free_gemini_key_manual_paste() {
        passOnboarding()
        tab("홈")
        rule.onNodeWithContentDescription("설정").performClick()
        val cm = rule.activity.getSystemService(android.content.ClipboardManager::class.java)
        rule.activity.runOnUiThread { cm.setPrimaryClip(android.content.ClipData.newPlainText("k", "  AIzaSyManualKey1234567890abcdefgh  ")) }
        clickText("📋 붙여넣기")
        clickText("연결하기")
        waitText("연결 확인 완료", substring = true)
        assertEquals("AIzaSyManualKey1234567890abcdefgh", container.settings.current.key(Provider.GEMINI))
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        waitText("Gemini 연결됨", substring = true)
    }

    private fun findWebView(): android.webkit.WebView {
        fun search(v: android.view.View): android.webkit.WebView? = when (v) {
            is android.webkit.WebView -> v
            is android.view.ViewGroup -> (0 until v.childCount).firstNotNullOfOrNull { search(v.getChildAt(it)) }
            else -> null
        }
        var found: android.webkit.WebView? = null
        rule.waitUntil(10_000) { found = search(rule.activity.window.decorView); found != null }
        return found!!
    }
}

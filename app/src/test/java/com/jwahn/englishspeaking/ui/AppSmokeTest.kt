package com.jwahn.englishspeaking.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
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

    private fun clickText(text: String, substring: Boolean = false) {
        waitText(text, substring)
        val node = rule.onAllNodesWithText(text, substring = substring).onFirst()
        runCatching { node.performScrollTo() }
        node.performClick()
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
        waitText("AI를 연결해 주세요", substring = true)
        tab("단어장")
        clickText("일상생활")
        waitText("breakfast")
        clickText("카드 학습")
        clickText("뜻 보기")
        waitText("아침 식사")
        clickText("알아요")
        waitText("commute")
        runBlocking {
            val w = container.db.vocab().findWord("breakfast")!!
            assertEquals(1, w.box)
            assertEquals(1, w.correct)
        }
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.activity.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        tab("패턴")
        waitText("I'm looking for ", substring = true)
        tab("AI 튜터")
        waitText("카페에서 주문하기")
        tab("홈")
        rule.onNodeWithContentDescription("설정").performClick()
        waitText("기본으로 쓸 AI")
    }

    @Test
    fun tutor_chat_correction_save_and_report() {
        connectClaude()
        tab("AI 튜터")
        clickText("카페에서 주문하기")
        waitText("Hi there! What can I get for you today?")
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
    }

    @Test
    fun other_screens_open_and_ai_generation() {
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
}

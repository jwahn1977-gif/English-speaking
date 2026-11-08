package com.jwahn.englishspeaking.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.jwahn.englishspeaking.App
import com.jwahn.englishspeaking.data.db.AppDatabase
import com.jwahn.englishspeaking.data.db.BeginnerSeed
import com.jwahn.englishspeaking.data.llm.ChatMsg
import com.jwahn.englishspeaking.data.llm.LlmClient
import com.jwahn.englishspeaking.data.llm.LlmException
import com.jwahn.englishspeaking.data.llm.OpenRouterAuth
import com.jwahn.englishspeaking.data.settings.Profiles
import com.jwahn.englishspeaking.data.settings.Provider
import com.jwahn.englishspeaking.data.settings.SettingsStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpgradeAndAuthTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    /** v0.1~0.2 때의 DB 구조 (버전 1) */
    private val v1Schema = listOf(
        "CREATE TABLE IF NOT EXISTS `chapter` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `source` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `word` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `chapterId` INTEGER NOT NULL, `text` TEXT NOT NULL, `ipa` TEXT NOT NULL, `meaningKo` TEXT NOT NULL, `exampleEn` TEXT NOT NULL, `exampleKo` TEXT NOT NULL, `bookmarked` INTEGER NOT NULL, `box` INTEGER NOT NULL, `nextDue` INTEGER NOT NULL, `correct` INTEGER NOT NULL, `wrong` INTEGER NOT NULL, `bestScore` INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS `index_word_chapterId` ON `word` (`chapterId`)",
        "CREATE INDEX IF NOT EXISTS `index_word_nextDue` ON `word` (`nextDue`)",
        "CREATE TABLE IF NOT EXISTS `pattern` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `template` TEXT NOT NULL, `meaningKo` TEXT NOT NULL, `category` TEXT NOT NULL, `source` TEXT NOT NULL, `box` INTEGER NOT NULL, `nextDue` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_pattern_template` ON `pattern` (`template`)",
        "CREATE TABLE IF NOT EXISTS `pattern_example` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `patternId` INTEGER NOT NULL, `en` TEXT NOT NULL, `ko` TEXT NOT NULL)",
        "CREATE INDEX IF NOT EXISTS `index_pattern_example_patternId` ON `pattern_example` (`patternId`)",
        "CREATE TABLE IF NOT EXISTS `scenario` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `title` TEXT NOT NULL, `emoji` TEXT NOT NULL, `persona` TEXT NOT NULL, `goal` TEXT NOT NULL, `missions` TEXT NOT NULL, `builtin` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `conversation` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `scenarioId` INTEGER NOT NULL, `title` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `reportJson` TEXT)",
        "CREATE TABLE IF NOT EXISTS `message` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `conversationId` INTEGER NOT NULL, `role` TEXT NOT NULL, `text` TEXT NOT NULL, `ko` TEXT NOT NULL, `correctionJson` TEXT, `extrasJson` TEXT, `createdAt` INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS `index_message_conversationId` ON `message` (`conversationId`)",
        "CREATE TABLE IF NOT EXISTS `used_word` (`word` TEXT NOT NULL, `firstUsedAt` INTEGER NOT NULL, PRIMARY KEY(`word`))",
        "CREATE TABLE IF NOT EXISTS `daily_stat` (`date` TEXT NOT NULL, `reviews` INTEGER NOT NULL, `sentences` INTEGER NOT NULL, `listened` INTEGER NOT NULL, PRIMARY KEY(`date`))",
        "CREATE TABLE IF NOT EXISTS `news_feed` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `url` TEXT NOT NULL, `enabled` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `news_lesson` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `url` TEXT NOT NULL, `title` TEXT NOT NULL, `mode` TEXT NOT NULL, `json` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `lastPosition` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
    )

    /** 이미 쓰던 폰에서 업데이트해도 학습 기록이 그대로 남는지 */
    @Test fun migration_v1_to_v2_keeps_progress_and_adds_beginner_course() = runBlocking {
        val file = ctx.getDatabasePath(AppDatabase.fileName("legacytest"))
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            v1Schema.forEach { db.execSQL(it) }
            db.execSQL("INSERT INTO chapter (title, description, source, createdAt) VALUES ('일상생활', '', 'builtin', 1)")
            db.execSQL(
                "INSERT INTO word (chapterId, text, ipa, meaningKo, exampleEn, exampleKo, bookmarked, box, nextDue, correct, wrong, bestScore) " +
                    "VALUES (1, 'breakfast', '', '아침 식사', '', '', 1, 4, 99, 7, 1, 80)"
            )
            db.version = 1
        }
        val db = AppDatabase.create(ctx, "legacytest")
        val w = db.vocab().findWord("breakfast")!!
        assertEquals(4, w.box); assertEquals(7, w.correct); assertTrue(w.bookmarked); assertEquals("", w.emoji)
        assertEquals(2, db.vocab().chapter(1)!!.level)
        Repo(db).seedIfNeeded()
        val chapters = db.vocab().allChapters()
        assertTrue(chapters.any { it.source == "beginner" && it.title == "알파벳 소리" && it.level == 0 })
        assertEquals(BeginnerSeed.chapters.size, chapters.count { it.source == "beginner" })
        assertEquals(4, db.vocab().findWord("breakfast")!!.box)
        db.close()
    }

    @Test fun beginner_seed_rows_are_well_formed() {
        val all = BeginnerSeed.all()
        val words = all.flatMap { it.second }
        assertTrue("단어 수 ${words.size}", words.size >= 330)
        words.forEach { w ->
            assertTrue(w.text, w.text.isNotBlank() && w.meaningKo.isNotBlank() && w.exampleEn.isNotBlank() && w.emoji.isNotBlank())
        }
        assertEquals(26, all.first { it.first.title == "알파벳 소리" }.second.size)
        assertEquals(50, all.first { it.first.title.startsWith("생활 표현") }.second.size)
    }

    /** 웹 로그인: 브라우저가 localhost로 돌아오면 코드를 받아 키로 바꾼다 */
    @Test fun openrouter_web_login_end_to_end() = runBlocking {
        var exchangedBody = ""
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            val req = chain.request()
            assertEquals("https://openrouter.ai/api/v1/auth/keys", req.url.toString())
            exchangedBody = Buffer().also { req.body!!.writeTo(it) }.readUtf8()
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"key":"sk-or-v1-test"}""".toResponseBody("application/json".toMediaType())).build()
        }).build()
        var opened = ""
        var page = ""
        val key = OpenRouterAuth(http).login(openBrowser = { url ->
            opened = url
            val callback = URLDecoder.decode(url.substringAfter("callback_url=").substringBefore("&"), "UTF-8")
            // 브라우저 흉내: favicon 요청이 먼저 와도 무시하고, 로그인 후 callback으로 돌아온다
            thread {
                Thread.sleep(200)
                runCatching { (URL(callback.replace("/callback", "/favicon.ico")).openConnection() as HttpURLConnection).inputStream.readBytes() }
                val conn = URL("$callback?code=abc123").openConnection() as HttpURLConnection
                page = conn.inputStream.readBytes().decodeToString()
            }
        }, timeoutMs = 10_000)
        assertEquals("sk-or-v1-test", key)
        assertTrue(opened.startsWith("https://openrouter.ai/auth?callback_url=http%3A%2F%2Flocalhost%3A"))
        assertTrue(opened.contains("code_challenge_method=S256"))
        assertTrue(exchangedBody.contains("\"code\":\"abc123\""))
        assertTrue(exchangedBody.contains("code_verifier"))
        Thread.sleep(200)
        assertTrue(page.contains("연결 완료"))
    }

    @Test fun pkce_challenge_is_sha256_of_verifier() {
        val p = OpenRouterAuth.pkce()
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(p.verifier.toByteArray())
        val expected = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        assertEquals(expected, p.challenge)
        assertEquals("x y", OpenRouterAuth.codeFromRequestLine("GET /callback?state=1&code=x%20y HTTP/1.1"))
        assertNull(OpenRouterAuth.codeFromRequestLine("GET /favicon.ico HTTP/1.1"))
    }

    /** OpenRouter로 호출, 사용량 기록, 하루 한도 */
    @Test fun openrouter_chat_usage_and_daily_limit() = runBlocking {
        val settings = SettingsStore(ctx, "usagetest")
        settings.update {
            it.copy(keys = it.keys + (Provider.OPENROUTER to "sk-or-x"), defaultProvider = Provider.OPENROUTER, dailyAiLimit = 2)
        }
        var lastUrl = ""
        var lastAuth = ""
        val http = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            lastUrl = chain.request().url.toString()
            lastAuth = chain.request().header("Authorization").orEmpty()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(
                    """{"choices":[{"message":{"content":"{\"reply\":\"Hi\"}"}}],"usage":{"prompt_tokens":10,"completion_tokens":5}}"""
                        .toResponseBody("application/json".toMediaType())
                ).build()
        }).build()
        val llm = LlmClient(settings, http)
        val o = llm.completeJson("sys", listOf(ChatMsg("user", "hello")))
        assertEquals("https://openrouter.ai/api/v1/chat/completions", lastUrl)
        assertEquals("Bearer sk-or-x", lastAuth)
        assertTrue(o.toString().contains("Hi"))
        assertEquals(1, settings.usage.value.todayCalls)
        assertEquals(15, settings.usage.value.todayTokens)
        llm.completeJson("sys", listOf(ChatMsg("user", "again")))
        try {
            llm.completeJson("sys", listOf(ChatMsg("user", "third")))
            fail("한도를 넘으면 막아야 함")
        } catch (e: LlmException) {
            assertTrue(e.message!!.contains("한도"))
        }
    }

    /** 가족 프로필: 학습 기록과 설정은 따로, AI 키는 함께 */
    @Test fun profiles_keep_separate_progress_but_share_keys() = runBlocking {
        val app = ctx as App
        val first = app.container
        first.settings.update { it.copy(keys = it.keys + (Provider.GEMINI to "AIza-shared"), nickname = "엄마", onboarded = true) }
        first.repo.seedIfNeeded()
        val w = first.db.vocab().findWord("breakfast")!!
        first.db.vocab().updateWord(w.copy(box = 5))

        val kid = first.profiles.add("민준", "🧒")
        app.switchProfile(kid.id)
        val second = app.container
        assertNotSame(first, second)
        second.repo.seedIfNeeded()
        assertEquals(0, second.db.vocab().findWord("breakfast")!!.box)
        assertEquals("", second.settings.current.nickname)
        assertEquals(false, second.settings.current.onboarded)
        assertEquals("AIza-shared", second.settings.current.key(Provider.GEMINI))

        app.switchProfile("default")
        val back = app.container
        assertEquals(5, back.db.vocab().findWord("breakfast")!!.box)
        assertEquals("엄마", back.settings.current.nickname)
        back.profiles.remove(ctx, kid.id)
        assertEquals(1, Profiles(ctx).list.value.size)
    }
}

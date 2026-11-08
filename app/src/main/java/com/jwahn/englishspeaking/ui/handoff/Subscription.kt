package com.jwahn.englishspeaking.ui.handoff

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.jwahn.englishspeaking.data.db.ScenarioEntity
import java.net.URLEncoder

/**
 * 구독 계정(Claude Pro, ChatGPT Plus, Gemini) 사용.
 * 앱이 수업용 지시문을 만들어 공식 앱/사이트로 넘기고, 결과는 복사해서 가져온다.
 * (공식 앱의 로그인 세션을 그대로 쓰며, 이 앱이 계정 정보나 대화를 가로채지 않는다)
 */
enum class SubApp(val id: String, val label: String, val loginUrl: String, val pkg: String, val prefill: Boolean) {
    CLAUDE("CLAUDE", "Claude", "https://claude.ai/login", "com.anthropic.claude", false),
    CHATGPT("CHATGPT", "ChatGPT", "https://chatgpt.com/auth/login", "com.openai.chatgpt", true),
    GEMINI("GEMINI", "Gemini", "https://gemini.google.com/app", "com.google.android.apps.bard", false);

    /** 지시문을 입력칸에 미리 채워 여는 주소 (지원하지 않으면 새 대화 화면) */
    fun chatUrl(prompt: String): String {
        val q = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
        return when (this) {
            // 미리 채우기 링크는 Claude 앱에서 다른 화면(Claude Code 등)으로 열릴 수 있어 채팅 첫 화면만 연다
            CLAUDE -> "https://claude.ai/"
            CHATGPT -> "https://chatgpt.com/?q=$q"
            GEMINI -> "https://gemini.google.com/app"
        }
    }

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id }
    }
}

fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("prompt", text))
}

fun readClipboard(context: Context): String {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return runCatching { cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() }.getOrNull().orEmpty()
}

private val mainHandler = Handler(Looper.getMainLooper())

/** 어느 스레드에서 불려도 메인 스레드에서 실행 (토스트·화면 열기) */
private fun onMain(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
}

/** 지시문을 복사하고 공식 앱(설치돼 있으면 앱, 아니면 브라우저)으로 연다 */
fun openInSubApp(context: Context, app: SubApp, prompt: String) = onMain {
    copyText(context, prompt)
    val url = app.chatUrl(prompt)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // 공식 앱이 링크를 처리하면 앱으로, 아니면 브라우저로 열린다
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "${app.label}를 열 수 없어요.", Toast.LENGTH_LONG).show(); return@onMain }
    Toast.makeText(
        context,
        if (app.prefill) "지시문이 입력칸에 들어가요. 안 보이면 길게 눌러 붙여넣기 하세요."
        else "지시문을 복사했어요. 입력칸을 길게 눌러 붙여넣기 하세요.",
        Toast.LENGTH_LONG,
    ).show()
}

object HandoffPrompts {
    private const val RESULT_JSON =
        """{"patterns":[{"pattern","meaning_ko","example_en","example_ko"}],"words":[{"word","ipa","meaning_ko","example_en","example_ko"}]} (3-5 patterns, up to 5 words)"""

    fun tutor(s: ScenarioEntity, level: String, kids: Boolean): String = """
📚 [말해요 영어] 영어 말하기 수업 지시문이에요. 그대로 보내기(↑)를 누르세요.

Role-play with me (Korean English learner). You are ${s.persona}. Scenario: ${s.title}. ${s.goal}
My level: $level. My missions: ${s.missions.lines().filter { it.isNotBlank() }.joinToString(" / ")}
Rules: stay in character, 1-3 short sentences, end with a question. If I make a mistake or write Korean, first write "✏️ Better: ..." with a short Korean note. Then add "💡 You can say:" with 2 easy answers.${if (kids) " I am a child: very simple words, never ask personal info." else ""}
When I type END: give Korean feedback, then one JSON code block: $RESULT_JSON
Start now with a short greeting.
""".trimIndent()

    const val TUTOR_END = "END"
}

/** 구글 계정 로그인 페이지인지 (앱 안 화면에서는 구글이 로그인을 막는다) */
fun isGoogleSignIn(url: String?): Boolean {
    val host = runCatching { Uri.parse(url).host }.getOrNull().orEmpty()
    return host == "accounts.google.com" || host.endsWith(".accounts.google.com")
}

/** 로그인 도움말: 비밀번호 찾기 주소와 안내 */
object LoginHelp {
    const val GOOGLE_RECOVERY = "https://accounts.google.com/signin/recovery"
    const val OPENAI_RESET = "https://chatgpt.com/auth/login"
    const val CLAUDE_LOGIN = "https://claude.ai/login"

    fun note(app: SubApp): String = when (app) {
        SubApp.CLAUDE -> "Claude는 비밀번호가 없어요. 이메일을 넣으면 오는 인증 코드·링크로 로그인해요. 구글 계정으로 가입했다면 'Chrome으로 열기'를 쓰세요."
        SubApp.CHATGPT -> "ChatGPT 로그인 화면에서 이메일 입력 후 '비밀번호를 잊으셨나요?'를 누르면 재설정 메일이 와요. 구글 계정으로 가입했다면 'Chrome으로 열기'를 쓰세요."
        SubApp.GEMINI -> "Gemini는 구글 계정으로만 로그인해요. 구글은 앱 안 화면 로그인을 막기 때문에 Chrome 창으로 열어요."
    }

    fun resetUrl(app: SubApp): String = when (app) {
        SubApp.CLAUDE -> CLAUDE_LOGIN
        SubApp.CHATGPT -> OPENAI_RESET
        SubApp.GEMINI -> GOOGLE_RECOVERY
    }
}

/**
 * Chrome 창(맞춤 탭)으로 연다. 구글 로그인은 여기서 정상 동작하고, 로그인은 Chrome에 계속 남는다.
 * Chrome이 없으면 기본 브라우저로 연다.
 */
fun openInChrome(context: Context, url: String) = onMain {
    val uri = Uri.parse(url)
    runCatching {
        androidx.browser.customtabs.CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .apply {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // 설치된 Claude·ChatGPT 앱이 링크를 가로채지 않도록 Chrome에서 연다
                androidx.browser.customtabs.CustomTabsClient.getPackageName(context, listOf("com.android.chrome"))
                    ?.let { intent.setPackage(it) }
            }
            .launchUrl(context, uri)
    }.recoverCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { Toast.makeText(context, "브라우저를 열 수 없어요.", Toast.LENGTH_LONG).show() }
}

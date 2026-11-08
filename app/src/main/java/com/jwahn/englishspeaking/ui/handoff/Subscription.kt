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
    CLAUDE("CLAUDE", "Claude", "https://claude.ai/login", "com.anthropic.claude", true),
    CHATGPT("CHATGPT", "ChatGPT", "https://chatgpt.com/auth/login", "com.openai.chatgpt", true),
    GEMINI("GEMINI", "Gemini", "https://gemini.google.com/app", "com.google.android.apps.bard", false);

    /** 지시문을 입력칸에 미리 채워 여는 주소 (지원하지 않으면 새 대화 화면) */
    fun chatUrl(prompt: String): String {
        val q = URLEncoder.encode(prompt, "UTF-8").replace("+", "%20")
        return when (this) {
            CLAUDE -> "https://claude.ai/new?q=$q"
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
        """{"patterns":[{"pattern":"I'd like ~","meaning_ko":"~ 주세요","example_en":"...","example_ko":"..."}],"words":[{"word":"...","ipa":"...","meaning_ko":"...","example_en":"...","example_ko":"..."}]}"""

    fun tutor(s: ScenarioEntity, level: String, kids: Boolean): String = """
Let's do an English speaking role-play. I am a Korean learner.
You are ${s.persona}. Scenario: ${s.title}. ${s.goal}
My level: $level. Match your words and sentence length to my level.
My missions: ${s.missions.lines().filter { it.isNotBlank() }.joinToString(" / ")}

Rules:
- Stay in character. Reply in 1-3 short sentences and usually end with a question.
- If my message has a mistake or is in Korean, first write "✏️ Better: <natural English>" with a short Korean explanation, then reply in character.
- After each reply, add "💡 You can say:" with 2 easy example answers for me.
${if (kids) "- I am a child. Use very simple words, be warm, and never ask for personal information." else ""}
- When I type END: give feedback in Korean (잘한 점, 고칠 점), then output one JSON code block exactly in this shape:
$RESULT_JSON
(3-5 useful patterns from our conversation, up to 5 key words)

Start now with a short greeting in character.
""".trimIndent()

    const val TUTOR_END = "END"
}

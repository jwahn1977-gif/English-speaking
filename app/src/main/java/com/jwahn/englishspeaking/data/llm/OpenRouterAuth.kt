package com.jwahn.englishspeaking.data.llm

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.resume

/**
 * OpenRouter 웹 로그인 (OAuth PKCE).
 * 1) 폰 안에서만 열리는 임시 주소(localhost)를 만들고
 * 2) 브라우저로 OpenRouter 로그인 페이지를 연다
 * 3) 로그인 후 브라우저가 임시 주소로 돌아오면 받은 코드로 API 키를 발급받는다
 */
class OpenRouterAuth(private val http: OkHttpClient) {

    data class Pkce(val verifier: String, val challenge: String)

    companion object {
        fun pkce(random: SecureRandom = SecureRandom()): Pkce {
            val bytes = ByteArray(48).also { random.nextBytes(it) }
            val verifier = b64url(bytes)
            val challenge = b64url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
            return Pkce(verifier, challenge)
        }

        private fun b64url(b: ByteArray) = Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

        fun authUrl(callback: String, challenge: String) =
            "https://openrouter.ai/auth?callback_url=${URLEncoder.encode(callback, "UTF-8")}" +
                "&code_challenge=$challenge&code_challenge_method=S256"

        /** "GET /callback?code=abc HTTP/1.1" → "abc" */
        fun codeFromRequestLine(line: String): String? {
            val path = line.split(" ").getOrNull(1) ?: return null
            val query = path.substringAfter('?', "")
            return query.split('&').firstOrNull { it.startsWith("code=") }
                ?.substringAfter("code=")?.let { URLDecoder.decode(it, "UTF-8") }?.takeIf { it.isNotBlank() }
        }

        private const val DONE_HTML = """<!doctype html><html lang="ko"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<body style="font-family:sans-serif;text-align:center;padding:48px 16px;font-size:20px">
<div style="font-size:56px">✅</div><h2>연결 완료!</h2><p>'말해요 영어' 앱으로 돌아가세요.</p></body></html>"""
    }

    /**
     * 로그인 전 과정을 수행하고 발급된 API 키를 돌려준다.
     * openBrowser: 로그인 페이지 주소를 브라우저로 여는 함수
     */
    suspend fun login(openBrowser: (String) -> Unit, timeoutMs: Long = 5 * 60_000): String {
        val pk = pkce()
        val server = withContext(Dispatchers.IO) { ServerSocket(0, 5, InetAddress.getByName("127.0.0.1")) }
        try {
            val callback = "http://localhost:${server.localPort}/callback"
            openBrowser(authUrl(callback, pk.challenge))
            val code = withTimeout(timeoutMs) { waitForCode(server) }
            return exchange(code, pk.verifier)
        } finally {
            runCatching { server.close() }
        }
    }

    private suspend fun waitForCode(server: ServerSocket): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { runCatching { server.close() } }
            try {
                while (cont.isActive) {
                    server.accept().use { sock ->
                        val line = sock.getInputStream().bufferedReader().readLine().orEmpty()
                        val code = codeFromRequestLine(line)
                        val body = if (code != null) DONE_HTML else "<html><body></body></html>"
                        val bytes = body.toByteArray()
                        sock.getOutputStream().apply {
                            write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                                "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray())
                            write(bytes); flush()
                        }
                        if (code != null) { cont.resume(code); return@suspendCancellableCoroutine }
                    }
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resumeWith(Result.failure(LlmException("로그인을 기다리다 중단됐어요: ${e.message}")))
            }
        }
    }

    suspend fun exchange(code: String, verifier: String): String = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("code", code); put("code_verifier", verifier); put("code_challenge_method", "S256")
        }.toString().toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("https://openrouter.ai/api/v1/auth/keys").post(body).build()
        http.newCall(req).execute().use { r ->
            val text = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw LlmException("로그인 확인 실패 (${r.code}). 다시 시도해 주세요.")
            Json.parseToJsonElement(text).jsonObject["key"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() } ?: throw LlmException("키를 받지 못했어요. 다시 시도해 주세요.")
        }
    }
}

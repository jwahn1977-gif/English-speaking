package com.jwahn.englishspeaking.data.llm

import com.jwahn.englishspeaking.data.settings.Provider
import com.jwahn.englishspeaking.data.settings.SettingsStore
import com.jwahn.englishspeaking.domain.JsonText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class ChatMsg(val role: String, val text: String) // role: user / assistant

class LlmException(message: String) : Exception(message)

/**
 * Claude / OpenAI / Gemini 공통 클라이언트.
 * 사용자가 설정에 입력한 API 키(BYOK)로 각 사 API를 직접 호출한다.
 */
class LlmClient(
    private val settings: SettingsStore,
    private val http: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonType = "application/json".toMediaType()

    fun activeProvider(): Provider? {
        val s = settings.current
        return if (s.hasKey(s.defaultProvider)) s.defaultProvider else s.connectedProviders.firstOrNull()
    }

    fun requireProvider(): Provider = activeProvider()
        ?: throw LlmException("AI가 연결되지 않았어요.\n설정 > AI 연결에서 Claude, ChatGPT, Gemini 중 하나의 API 키를 입력해 주세요.")

    /** 텍스트 응답 */
    suspend fun complete(
        system: String,
        messages: List<ChatMsg>,
        jsonMode: Boolean = false,
        maxTokens: Int = 2048,
        provider: Provider? = null,
    ): String = withContext(Dispatchers.IO) {
        val p = provider ?: requireProvider()
        val s = settings.current
        val key = s.key(p).ifBlank { throw LlmException("${p.label} API 키가 없습니다.") }
        val model = s.model(p)
        val merged = mergeRoles(messages)
        when (p) {
            Provider.CLAUDE -> claude(key, model, system, merged, maxTokens)
            Provider.OPENAI -> openai(key, model, system, merged, maxTokens, jsonMode)
            Provider.GEMINI -> gemini(key, model, system, merged, maxTokens, jsonMode)
        }
    }

    /** JSON 객체 응답 (프롬프트에서 JSON 형식을 지시해야 함) */
    suspend fun completeJson(
        system: String,
        messages: List<ChatMsg>,
        maxTokens: Int = 3000,
        provider: Provider? = null,
    ): JsonObject {
        val text = complete(system, messages, jsonMode = true, maxTokens = maxTokens, provider = provider)
        return JsonText.parseObject(text)
            ?: throw LlmException("AI 응답을 해석하지 못했어요. 다시 시도해 주세요.\n\n${text.take(300)}")
    }

    suspend fun quickJson(prompt: String, maxTokens: Int = 3000): JsonObject =
        completeJson(
            system = "You are an expert English teacher for Korean learners. Always answer with a single valid JSON object only, no markdown.",
            messages = listOf(ChatMsg("user", prompt)),
            maxTokens = maxTokens,
        )

    /** 연결 테스트 + 모델 목록 */
    suspend fun listModels(p: Provider, key: String): List<String> = withContext(Dispatchers.IO) {
        if (key.isBlank()) throw LlmException("API 키를 입력하세요.")
        val req = when (p) {
            Provider.CLAUDE -> Request.Builder().url("https://api.anthropic.com/v1/models?limit=100")
                .header("x-api-key", key).header("anthropic-version", "2023-06-01").build()
            Provider.OPENAI -> Request.Builder().url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer $key").build()
            Provider.GEMINI -> Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=200")
                .header("x-goog-api-key", key).build()
        }
        val body = execute(req)
        val root = json.parseToJsonElement(body).jsonObject
        when (p) {
            Provider.CLAUDE -> root["data"]?.jsonArray?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
            Provider.OPENAI -> root["data"]?.jsonArray
                ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
                ?.filter { id -> id.startsWith("gpt") || id.startsWith("chatgpt") || Regex("^o\\d").containsMatchIn(id) }
                ?.filterNot { id -> listOf("audio", "realtime", "transcribe", "tts", "image", "search").any { id.contains(it) } }
                ?.sortedDescending()
            Provider.GEMINI -> root["models"]?.jsonArray?.mapNotNull { m ->
                val o = m.jsonObject
                val methods = o["supportedGenerationMethods"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                if ("generateContent" in methods) o["name"]?.jsonPrimitive?.contentOrNull?.removePrefix("models/") else null
            }?.filter { it.startsWith("gemini") }
        }.orEmpty()
    }

    // ---- providers ----

    private fun claude(key: String, model: String, system: String, msgs: List<ChatMsg>, maxTokens: Int): String {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", maxTokens)
            put("system", system)
            putJsonArray("messages") {
                msgs.forEach { m -> addJsonObject { put("role", m.role); put("content", m.text) } }
            }
        }
        val req = Request.Builder().url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", key)
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody(jsonType)).build()
        val root = json.parseToJsonElement(execute(req)).jsonObject
        val text = root["content"]?.jsonArray
            ?.filter { it.jsonObject["type"]?.jsonPrimitive?.contentOrNull == "text" }
            ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
        if (root["stop_reason"]?.jsonPrimitive?.contentOrNull == "refusal" && text.isNullOrBlank()) {
            throw LlmException("AI가 이 요청에 응답하지 않았어요. 표현을 바꿔 다시 시도해 주세요.")
        }
        return text?.takeIf { it.isNotBlank() } ?: throw LlmException("빈 응답을 받았어요.")
    }

    private fun openai(key: String, model: String, system: String, msgs: List<ChatMsg>, maxTokens: Int, jsonMode: Boolean): String {
        val body = buildJsonObject {
            put("model", model)
            put("max_completion_tokens", maxTokens)
            putJsonArray("messages") {
                addJsonObject { put("role", "system"); put("content", system) }
                msgs.forEach { m -> addJsonObject { put("role", m.role); put("content", m.text) } }
            }
            if (jsonMode) putJsonObject("response_format") { put("type", "json_object") }
        }
        val req = Request.Builder().url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody(jsonType)).build()
        val root = json.parseToJsonElement(execute(req)).jsonObject
        return root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: throw LlmException("빈 응답을 받았어요.")
    }

    private fun gemini(key: String, model: String, system: String, msgs: List<ChatMsg>, maxTokens: Int, jsonMode: Boolean): String {
        val body = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") { addJsonObject { put("text", system) } }
            }
            putJsonArray("contents") {
                msgs.forEach { m ->
                    addJsonObject {
                        put("role", if (m.role == "assistant") "model" else "user")
                        putJsonArray("parts") { addJsonObject { put("text", m.text) } }
                    }
                }
            }
            putJsonObject("generationConfig") {
                // 생각(thinking) 토큰이 출력 한도를 소모하는 모델이 있어 여유 있게 잡는다.
                put("maxOutputTokens", maxOf(maxTokens * 2, 4096))
                if (jsonMode) put("responseMimeType", "application/json")
            }
        }
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toString().toRequestBody(jsonType)).build()
        val root = json.parseToJsonElement(execute(req)).jsonObject
        val parts = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray ?: JsonArray(emptyList())
        val text = parts.filter { it.jsonObject["thought"]?.jsonPrimitive?.booleanOrNull != true }
            .joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
        return text.takeIf { it.isNotBlank() } ?: throw LlmException("빈 응답을 받았어요. (안전 필터 또는 토큰 한도)")
    }

    private fun execute(req: Request): String {
        val resp = try {
            http.newCall(req).execute()
        } catch (e: java.io.IOException) {
            throw LlmException("네트워크 오류: ${e.message ?: "연결 실패"}")
        }
        resp.use {
            val body = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                val msg = runCatching {
                    val o = json.parseToJsonElement(body).jsonObject
                    o["error"]?.let { e ->
                        if (e is JsonObject) e["message"]?.jsonPrimitive?.contentOrNull else e.jsonPrimitive.contentOrNull
                    }
                }.getOrNull() ?: body.take(200)
                val hint = when (it.code) {
                    401, 403 -> " (API 키를 확인하세요)"
                    404 -> " (모델 이름을 확인하세요)"
                    429 -> " (사용량 한도 초과 또는 크레딧 부족)"
                    else -> ""
                }
                throw LlmException("API 오류 ${it.code}$hint\n$msg")
            }
            return body
        }
    }

    /** Claude·Gemini는 같은 역할의 연속 메시지를 싫어하므로 합친다. 첫 메시지는 user 여야 한다. */
    private fun mergeRoles(msgs: List<ChatMsg>): List<ChatMsg> {
        val out = mutableListOf<ChatMsg>()
        for (m in msgs.filter { it.text.isNotBlank() }) {
            val last = out.lastOrNull()
            if (last != null && last.role == m.role) out[out.size - 1] = ChatMsg(m.role, last.text + "\n\n" + m.text)
            else out += m
        }
        if (out.firstOrNull()?.role == "assistant") out.add(0, ChatMsg("user", "(start)"))
        if (out.isEmpty()) out += ChatMsg("user", "(start)")
        return out
    }
}

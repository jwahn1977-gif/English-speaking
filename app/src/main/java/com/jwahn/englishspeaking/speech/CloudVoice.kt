package com.jwahn.englishspeaking.speech

import android.content.Context
import android.util.Base64
import com.jwahn.englishspeaking.data.settings.Provider
import com.jwahn.englishspeaking.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
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
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/** 읽어 주는 목소리 엔진 */
enum class VoiceEngine(val label: String, val needs: Provider?, val voices: List<Pair<String, String>>, val defaultVoice: String) {
    DEVICE("휴대폰 음성", null, emptyList(), ""),
    OPENAI(
        "OpenAI 고음질", Provider.OPENAI,
        listOf("nova" to "Nova (밝은 여성)", "shimmer" to "Shimmer (부드러운 여성)", "alloy" to "Alloy (중성)",
            "echo" to "Echo (차분한 남성)", "onyx" to "Onyx (낮은 남성)", "fable" to "Fable (영국식)"),
        "nova",
    ),
    GEMINI(
        "Gemini 고음질", Provider.GEMINI,
        listOf("Kore" to "Kore (또렷한 여성)", "Aoede" to "Aoede (밝은 여성)", "Leda" to "Leda (젊은 여성)",
            "Puck" to "Puck (경쾌한 남성)", "Charon" to "Charon (차분한 남성)", "Orus" to "Orus (단단한 남성)"),
        "Kore",
    );

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: DEVICE
    }
}

class VoiceException(message: String) : Exception(message)

/**
 * AI 고음질 음성. 이미 연결한 OpenAI·Gemini API 키를 쓴다.
 * 한 번 받은 음성은 폰에 저장해 두고 다시 쓰므로, 같은 문장은 요금·인터넷 없이 바로 재생된다.
 */
class CloudVoice(context: Context, private val settings: SettingsStore, private val http: OkHttpClient) {

    private val dir = File(context.cacheDir, "voice").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val OPENAI_MODEL = "gpt-4o-mini-tts"
        const val OPENAI_FALLBACK_MODEL = "tts-1"
        const val GEMINI_TTS_MODEL = "gemini-2.5-flash-preview-tts"
        private const val MAX_CACHE_BYTES = 150L * 1024 * 1024

        /** Gemini가 주는 PCM(16bit, mono)을 재생 가능한 WAV로 감싼다 */
        fun pcmToWav(pcm: ByteArray, sampleRate: Int = 24000, channels: Int = 1, bits: Int = 16): ByteArray {
            val byteRate = sampleRate * channels * bits / 8
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray()); putInt(36 + pcm.size); put("WAVE".toByteArray())
                put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(channels.toShort())
                putInt(sampleRate); putInt(byteRate); putShort((channels * bits / 8).toShort()); putShort(bits.toShort())
                put("data".toByteArray()); putInt(pcm.size)
            }.array()
            return header + pcm
        }

        fun cacheKey(engine: VoiceEngine, voice: String, text: String): String =
            MessageDigest.getInstance("SHA-256").digest("${engine.name}|$voice|${text.trim()}".toByteArray())
                .joinToString("") { "%02x".format(it) }.take(40)
    }

    /** 지금 설정으로 고음질 음성을 쓸 수 있는지 */
    fun enabled(): Boolean {
        val s = settings.current
        val engine = VoiceEngine.of(s.voiceEngine)
        return engine != VoiceEngine.DEVICE && engine.needs != null && s.hasKey(engine.needs)
    }

    private fun currentVoice(engine: VoiceEngine): String =
        settings.current.cloudVoice.takeIf { v -> engine.voices.any { it.first == v } } ?: engine.defaultVoice

    /** 문장 음성 파일을 돌려준다 (저장된 게 있으면 그대로) */
    suspend fun fetch(text: String): File = withContext(Dispatchers.IO) {
        val s = settings.current
        val engine = VoiceEngine.of(s.voiceEngine)
        val provider = engine.needs ?: throw VoiceException("휴대폰 음성을 쓰는 중이에요.")
        val key = s.key(provider).ifBlank { throw VoiceException("${provider.label} 키가 필요해요.") }
        val voice = currentVoice(engine)
        val ext = if (engine == VoiceEngine.OPENAI) "mp3" else "wav"
        val file = File(dir, cacheKey(engine, voice, text) + "." + ext)
        if (file.exists() && file.length() > 0) { file.setLastModified(System.currentTimeMillis()); return@withContext file }
        val bytes = when (engine) {
            VoiceEngine.OPENAI -> openai(key, voice, text)
            VoiceEngine.GEMINI -> gemini(key, voice, text)
            VoiceEngine.DEVICE -> throw VoiceException("휴대폰 음성을 쓰는 중이에요.")
        }
        val tmp = File(dir, file.name + ".tmp")
        tmp.writeBytes(bytes)
        tmp.renameTo(file)
        prune()
        file
    }

    private fun post(req: Request): ByteArray {
        val resp = try { http.newCall(req).execute() } catch (e: java.io.IOException) {
            throw VoiceException("네트워크 오류: ${e.message}")
        }
        resp.use {
            val body = it.body?.bytes() ?: ByteArray(0)
            if (!it.isSuccessful) throw VoiceException("음성 만들기 실패 (${it.code}) ${String(body).take(150)}")
            return body
        }
    }

    private fun openai(key: String, voice: String, text: String): ByteArray {
        fun req(model: String) = Request.Builder().url("https://api.openai.com/v1/audio/speech")
            .header("Authorization", "Bearer $key")
            .post(buildJsonObject {
                put("model", model); put("voice", voice); put("input", text); put("response_format", "mp3")
            }.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            post(req(OPENAI_MODEL))
        } catch (e: VoiceException) {
            // 모델 이름이 바뀌었거나 계정에서 못 쓰는 경우 예전 모델로 한 번 더
            if (e.message?.contains("(400)") == true || e.message?.contains("(404)") == true) post(req(OPENAI_FALLBACK_MODEL)) else throw e
        }
    }

    private fun gemini(key: String, voice: String, text: String): ByteArray {
        val body = buildJsonObject {
            putJsonArray("contents") { addJsonObject { putJsonArray("parts") { addJsonObject { put("text", text) } } } }
            putJsonObject("generationConfig") {
                putJsonArray("responseModalities") { add(kotlinx.serialization.json.JsonPrimitive("AUDIO")) }
                putJsonObject("speechConfig") {
                    putJsonObject("voiceConfig") { putJsonObject("prebuiltVoiceConfig") { put("voiceName", voice) } }
                }
            }
        }
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_TTS_MODEL:generateContent")
            .header("x-goog-api-key", key)
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        val root = json.parseToJsonElement(String(post(req))).jsonObject
        val part = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray
            ?.firstOrNull { it.jsonObject["inlineData"] != null }?.jsonObject?.get("inlineData")?.jsonObject
            ?: throw VoiceException("Gemini가 음성을 돌려주지 않았어요.")
        val data = part["data"]?.jsonPrimitive?.contentOrNull ?: throw VoiceException("음성 데이터가 비어 있어요.")
        val mime = part["mimeType"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val rate = Regex("rate=(\\d+)").find(mime)?.groupValues?.get(1)?.toIntOrNull() ?: 24000
        return pcmToWav(Base64.decode(data, Base64.DEFAULT), rate)
    }

    /** 저장된 음성이 너무 많아지면 오래된 것부터 지운다 */
    private fun prune() {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { f ->
            if (total <= MAX_CACHE_BYTES * 8 / 10) return
            total -= f.length(); f.delete()
        }
    }

    fun cacheSizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L
    fun clearCache() { dir.listFiles()?.forEach { it.delete() } }
}

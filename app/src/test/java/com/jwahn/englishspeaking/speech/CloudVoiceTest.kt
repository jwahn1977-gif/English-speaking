package com.jwahn.englishspeaking.speech

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.jwahn.englishspeaking.data.settings.Provider
import com.jwahn.englishspeaking.data.settings.SettingsStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CloudVoiceTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val requests = mutableListOf<Pair<String, String>>()

    private fun body(r: Request) = r.body?.let { b -> Buffer().also { b.writeTo(it) }.readUtf8() }.orEmpty()

    private fun client(handler: (Request) -> Response) = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        requests += chain.request().url.toString() to body(chain.request())
        handler(chain.request())
    }).build()

    private fun ok(r: Request, bytes: ByteArray, type: String) = Response.Builder().request(r).protocol(Protocol.HTTP_1_1)
        .code(200).message("OK").body(bytes.toResponseBody(type.toMediaType())).build()

    @Test fun openai_voice_request_cache_and_model_fallback() = runBlocking {
        val settings = SettingsStore(ctx, "voicetest1")
        settings.update { it.copy(keys = it.keys + (Provider.OPENAI to "sk-test"), voiceEngine = "OPENAI", cloudVoice = "shimmer") }
        val voice = CloudVoice(ctx, settings, client { r ->
            // 새 모델이 없다고 가정하고 예전 모델로 다시 시도하는지 확인
            if (body(r).contains(CloudVoice.OPENAI_MODEL))
                Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(404).message("NF")
                    .body("model not found".toResponseBody(null)).build()
            else ok(r, byteArrayOf(1, 2, 3), "audio/mpeg")
        })
        voice.clearCache()
        assertTrue(voice.enabled())
        val f1 = voice.fetch("Hello there.")
        assertEquals(3L, f1.length())
        assertTrue(f1.name.endsWith(".mp3"))
        assertEquals(2, requests.size)
        assertEquals("https://api.openai.com/v1/audio/speech", requests[0].first)
        assertTrue(requests[1].second.contains("\"voice\":\"shimmer\"") && requests[1].second.contains("\"input\":\"Hello there.\""))
        assertTrue(requests[1].second.contains(CloudVoice.OPENAI_FALLBACK_MODEL))
        // 같은 문장은 저장된 파일을 다시 쓴다 (요청 없음)
        val f2 = voice.fetch("Hello there.")
        assertEquals(f1.path, f2.path)
        assertEquals(2, requests.size)
    }

    @Test fun gemini_voice_pcm_becomes_wav() = runBlocking {
        val settings = SettingsStore(ctx, "voicetest2")
        settings.update { it.copy(keys = it.keys + (Provider.GEMINI to "AIza-test"), voiceEngine = "GEMINI", cloudVoice = "Puck") }
        val pcm = ByteArray(480) { (it % 7).toByte() }
        val data = Base64.encodeToString(pcm, Base64.NO_WRAP)
        val voice = CloudVoice(ctx, settings, client { r ->
            ok(r, """{"candidates":[{"content":{"parts":[{"inlineData":{"mimeType":"audio/L16;codec=pcm;rate=24000","data":"$data"}}]}}]}""".toByteArray(), "application/json")
        })
        voice.clearCache()
        val f = voice.fetch("Good morning!")
        assertTrue(requests[0].first.contains("gemini-2.5-flash-preview-tts:generateContent"))
        assertTrue(requests[0].second.contains("\"voiceName\":\"Puck\"") && requests[0].second.contains("AUDIO"))
        val bytes = f.readBytes()
        assertEquals("RIFF", String(bytes, 0, 4))
        assertEquals("WAVE", String(bytes, 8, 4))
        assertEquals(44 + pcm.size, bytes.size)
    }

    @Test fun device_voice_or_missing_key_is_not_cloud() {
        val settings = SettingsStore(ctx, "voicetest3")
        val voice = CloudVoice(ctx, settings, OkHttpClient())
        assertFalse(voice.enabled())
        settings.update { it.copy(voiceEngine = "OPENAI") }
        assertFalse(voice.enabled()) // 키가 없으면 휴대폰 음성
    }
}

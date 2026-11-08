package com.jwahn.englishspeaking.speech

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * 시스템 음성인식(구글 음성 입력 등)을 띄워 결과 문장을 받는다.
 * 별도 권한이 필요 없고 대부분의 안드로이드 폰에서 동작한다.
 * 반환값: (language) -> Unit 을 호출하면 인식 시작. language 예: "en-US", "ko-KR"
 */
@Composable
fun rememberSpeechInput(onResult: (String) -> Unit): (String, String?) -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val text = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) callback.value(text)
        }
    }
    return { lang, prompt ->
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (prompt != null) putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        try {
            launcher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "음성 인식 앱이 없어요. Google 앱(음성 입력)을 설치해 주세요.", Toast.LENGTH_LONG).show()
        }
    }
}

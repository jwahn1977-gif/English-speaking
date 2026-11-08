package com.jwahn.englishspeaking

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jwahn.englishspeaking.ui.AppRoot
import com.jwahn.englishspeaking.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as App).container
        setContent {
            val s by container.settings.state.collectAsState()
            AppTheme(fontScale = s.fontScale) {
                AppRoot(container)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val c = (application as App).container
        // 뉴스를 듣는 중이면 백그라운드에서도 계속 재생
        if (!isChangingConfigurations && !c.newsPlayer.state.value.playing) c.tts.stop()
    }

    companion object {
        /** 가족 프로필을 바꾸고 앱 화면을 처음부터 다시 연다 */
        fun switchProfile(activity: Activity, profileId: String) {
            (activity.application as App).switchProfile(profileId)
            activity.startActivity(
                Intent(activity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            activity.finish()
        }
    }
}

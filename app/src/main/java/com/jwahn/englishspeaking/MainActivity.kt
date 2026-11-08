package com.jwahn.englishspeaking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jwahn.englishspeaking.ui.AppRoot
import com.jwahn.englishspeaking.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as App).container
        setContent {
            AppTheme {
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
}

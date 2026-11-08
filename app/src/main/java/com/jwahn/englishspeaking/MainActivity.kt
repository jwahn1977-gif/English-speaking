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
        if (!isChangingConfigurations) (application as App).container.tts.stop()
    }
}

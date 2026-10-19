package com.ranzan.zbuilds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ranzan.zbuilds.ui.home.HomeScreen
import com.ranzan.zbuilds.ui.settings.SettingsScreen
import com.ranzan.zbuilds.ui.theme.ZBuildsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZBuildsTheme {
                var currentScreen by rememberSaveable { mutableStateOf("home") }

                BackHandler(enabled = currentScreen == "settings") {
                    currentScreen = "home"
                }

                when (currentScreen) {
                    "home" -> {
                        HomeScreen(
                            onSettingsClick = { currentScreen = "settings" }
                        )
                    }
                    "settings" -> {
                        SettingsScreen(
                            onNavigateBack = { currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }
}

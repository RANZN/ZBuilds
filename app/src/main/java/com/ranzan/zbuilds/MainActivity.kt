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
import com.ranzan.zbuilds.ui.home.HomeScreenHost
import com.ranzan.zbuilds.ui.settings.SettingsScreenHost
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
                        HomeScreenHost(
                            onSettingsClick = { currentScreen = "settings" }
                        )
                    }
                    "settings" -> {
                        SettingsScreenHost(
                            onNavigateBack = { currentScreen = "home" }
                        )
                    }
                }
            }
        }
    }
}

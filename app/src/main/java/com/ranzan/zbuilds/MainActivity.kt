package com.ranzan.zbuilds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ranzan.zbuilds.ui.home.HomeScreen
import com.ranzan.zbuilds.ui.theme.ZBuildsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZBuildsTheme {
                HomeScreen()
            }
        }
    }
}

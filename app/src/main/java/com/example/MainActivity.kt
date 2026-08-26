package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.ThemeMode
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.TripTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as? TripTimerApplication
        val settingsFlow = app?.settingsRepository?.settingsFlow

        setContent {
            val settings by settingsFlow?.collectAsStateWithLifecycle(
                initialValue = com.example.domain.model.AppSettings()
            ) ?: androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(com.example.domain.model.AppSettings())
            }

            TripTimerTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}

package io.github.yina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import io.github.yina.ui.home.HomeScreen
import io.github.yina.ui.study.StudyScreen
import io.github.yina.ui.theme.Motion
import io.github.yina.ui.theme.ThemeMode
import io.github.yina.ui.theme.ThemePreferences
import io.github.yina.ui.theme.YinaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val preferences = remember { ThemePreferences(context) }
            var themeModeName by rememberSaveable {
                mutableStateOf(preferences.mode.name)
            }
            val themeMode = ThemeMode.fromStorage(themeModeName)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SideEffect {
                WindowCompat.getInsetsController(
                    this@MainActivity.window,
                    this@MainActivity.window.decorView,
                ).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            YinaTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var studyDeckId by rememberSaveable { mutableStateOf<Long?>(null) }
                    AnimatedContent(
                        targetState = studyDeckId,
                        transitionSpec = {
                            fadeIn(Motion.fade()) togetherWith fadeOut(Motion.fade())
                        },
                        label = "root-screen",
                    ) { deckId ->
                        if (deckId == null) {
                            HomeScreen(
                                onOpenDeck = { studyDeckId = it },
                                themeMode = themeMode,
                                onThemeModeChange = { selected ->
                                    themeModeName = selected.name
                                    preferences.mode = selected
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            StudyScreen(
                                deckId = deckId,
                                onExit = { studyDeckId = null },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

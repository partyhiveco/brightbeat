package com.example.vosclone

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.vosclone.chart.Chart
import com.example.vosclone.chart.ChartLoader
import com.example.vosclone.engine.GameSession
import com.example.vosclone.engine.TimingCalibrationStore
import com.example.vosclone.ui.calibration.CalibrationScreen
import com.example.vosclone.ui.gameplay.GameplayScreen
import com.example.vosclone.ui.menu.MenuScreen
import com.example.vosclone.ui.navigation.RootDestination
import com.example.vosclone.ui.profile.ProfileScreen
import com.example.vosclone.ui.results.ResultsScreen
import com.example.vosclone.ui.shop.ShopScreen
import com.example.vosclone.ui.theme.VosCloneTheme

private val DEMO_CHART_FILES = listOf(
    "demo1.json",
    "demo2.json",
    "demo3.json",
    "demo4.json",
    "demo5.json",
    "demo6.json",
    "demo7.json",
    "demo8.json"
)

private sealed class Screen {
    data object Menu : Screen()
    data object Shop : Screen()
    data object Profile : Screen()
    data object Calibration : Screen()
    data class Playing(val chart: Chart) : Screen()
    data class Results(val session: GameSession) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let the concert stage continue behind both Android system bars.
        // Explicit layout flags keep this compatible with the app's minSdk
        // without adding another window-insets dependency.
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val demoCharts = remember {
                DEMO_CHART_FILES.map { ChartLoader.loadFromAssets(this, it) }
            }
            var screen by remember { mutableStateOf<Screen>(Screen.Menu) }
            var timingOffsetMs by remember { mutableLongStateOf(TimingCalibrationStore.load(this)) }
            val onNavigate: (RootDestination) -> Unit = { destination ->
                screen = when (destination) {
                    RootDestination.SETLIST -> Screen.Menu
                    RootDestination.SHOP -> Screen.Shop
                    RootDestination.PROFILE -> Screen.Profile
                }
            }

            VosCloneTheme {
                Crossfade(targetState = screen, animationSpec = tween(220), label = "screen-transition") { s ->
                    when (s) {
                        is Screen.Menu -> MenuScreen(
                            charts = demoCharts,
                            onSelectChart = { chart -> screen = Screen.Playing(chart) },
                            onNavigate = onNavigate
                        )
                        is Screen.Shop -> ShopScreen(onNavigate = onNavigate)
                        is Screen.Profile -> ProfileScreen(
                            onNavigate = onNavigate,
                            onCalibrate = { screen = Screen.Calibration }
                        )
                        is Screen.Calibration -> CalibrationScreen(
                            currentOffsetMs = timingOffsetMs,
                            onSave = { offsetMs ->
                                timingOffsetMs = offsetMs
                                TimingCalibrationStore.save(this, offsetMs)
                                screen = Screen.Profile
                            },
                            onCancel = { screen = Screen.Profile }
                        )
                        is Screen.Playing -> GameplayScreen(
                            chart = s.chart,
                            assetAudioPath = "audio/${s.chart.audioFile}",
                            timingOffsetMs = timingOffsetMs,
                            onFinished = { session -> screen = Screen.Results(session) },
                            onQuit = { screen = Screen.Menu }
                        )
                        is Screen.Results -> ResultsScreen(
                            session = s.session,
                            onPlayAgain = { screen = Screen.Playing(s.session.chart) },
                            onBackToMenu = { screen = Screen.Menu }
                        )
                    }
                }
            }
        }
    }
}

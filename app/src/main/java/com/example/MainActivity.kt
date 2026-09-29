package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Settings
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.LiveTrackingScreen
import com.example.ui.screens.RallyAnalysisScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PadelViewModel

import androidx.compose.material.icons.filled.Groups
import com.example.ui.screens.MatchesAndPlayersScreen

enum class AppScreen(val title: String, val icon: ImageVector, val tag: String) {
    TRACKER("Матч & Трекер", Icons.Default.Videocam, "nav_tracker"),
    MATCHES("Матчи & Игроки", Icons.Default.Groups, "nav_matches"),
    DASHBOARD("Дашборд", Icons.Default.Analytics, "nav_dashboard"),
    RALLIES("Розыгрыши", Icons.Default.Timeline, "nav_rallies"),
    SETTINGS("Настройки", Icons.Default.Settings, "nav_settings"),
    CALIBRATION("Калибровка", Icons.Default.CropFree, "nav_calibration")
}

class MainActivity : ComponentActivity() {
    private val viewModel: PadelViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.TRACKER) }

                BackHandler(enabled = currentScreen != AppScreen.TRACKER) {
                    currentScreen = AppScreen.TRACKER
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                androidx.compose.foundation.layout.Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SportsTennis,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = "PadelVision",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                                    androidx.compose.material3.Surface(
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "BT Audio • 10x20m",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            AppScreen.values().forEach { screen ->
                                val selected = currentScreen == screen
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = screen.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag(screen.tag),
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.TRACKER -> LiveTrackingScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
                            )
                            AppScreen.MATCHES -> MatchesAndPlayersScreen(
                                viewModel = viewModel,
                                onNavigateToCourtTracker = { currentScreen = AppScreen.TRACKER }
                            )
                            AppScreen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToRallies = { currentScreen = AppScreen.RALLIES }
                            )
                            AppScreen.RALLIES -> RallyAnalysisScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                            )
                            AppScreen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToCalibration = { currentScreen = AppScreen.CALIBRATION }
                            )
                            AppScreen.CALIBRATION -> CalibrationScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }

    private var lastVolUpTime = 0L
    private var lastVolDownTime = 0L

    /**
     * Поддержка аппаратных кнопок громкости / пульта Bluetooth (селфи-пульт / наушники / колонка / умное кольцо)
     * Одиночное нажатие:
     *   - Громкость вверх = +1 очко Ближним
     *   - Громкость вниз = +1 очко Дальним
     * Двойной быстрый клик (< 400 мс):
     *   - Двойной клик вверх = -1 очко Ближним (или отмена)
     *   - Двойной клик вниз = -1 очко Дальним (или отмена)
     * Play/Pause = Повторить текущий счёт голосом в колонку
     * Двойной Play/Pause = Отмена последнего действия (Undo)
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = event.action
        val keyCode = event.keyCode
        val now = System.currentTimeMillis()

        if (action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    if (now - lastVolUpTime < 450) {
                        // Быстрый дабл-клик: отнять очко у Ближних (-1)
                        lastVolUpTime = 0L
                        viewModel.deductPointTeam1()
                    } else {
                        lastVolUpTime = now
                        viewModel.addPointTeam1(triggerVoice = true)
                    }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    if (now - lastVolDownTime < 450) {
                        // Быстрый дабл-клик: отнять очко у Дальних (-1)
                        lastVolDownTime = 0L
                        viewModel.deductPointTeam2()
                    } else {
                        lastVolDownTime = now
                        viewModel.addPointTeam2(triggerVoice = true)
                    }
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_HEADSETHOOK -> {
                    viewModel.repeatScoreVoice()
                    return true
                }
            }
        }
        // Подавляем системный регулятор громкости для кнопок громкости при отпускании (ACTION_UP)
        if (action == KeyEvent.ACTION_UP && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true
        }

        return super.dispatchKeyEvent(event)
    }
}

package com.example.ui.screens

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.PadelViewModel
import java.util.concurrent.Executors

@Composable
fun LiveTrackingScreen(
    viewModel: PadelViewModel,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isTracking by viewModel.isTrackingActive.collectAsStateWithLifecycle()
    val liveFps by viewModel.liveFps.collectAsStateWithLifecycle()
    val ballPos by viewModel.detectedBallScreenPos.collectAsStateWithLifecycle()
    val liveSpeed by viewModel.currentShotSpeedLive.collectAsStateWithLifecycle()
    val recentDetections by viewModel.recentDetectionsCount.collectAsStateWithLifecycle()
    val calibrationPoints by viewModel.calibrationPoints.collectAsStateWithLifecycle()
    val scoreState by viewModel.scoreState.collectAsStateWithLifecycle()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsStateWithLifecycle()
    val useAliceVoice by viewModel.useAliceVoice.collectAsStateWithLifecycle()
    val currentVoice by viewModel.currentSpeechKitVoice.collectAsStateWithLifecycle()
    val isAlicePrewarming by viewModel.isAlicePrewarming.collectAsStateWithLifecycle()
    val autoCvScoreEnabled by viewModel.autoCvScoreEnabled.collectAsStateWithLifecycle()
    val npuStats by viewModel.npuStats.collectAsStateWithLifecycle()

    val currentMatch by viewModel.currentMatch.collectAsStateWithLifecycle()
    val recognizedPlayers by viewModel.recognizedPlayers.collectAsStateWithLifecycle()

    var useRealCameraPreview by remember { mutableStateOf(true) }
    var showScoreModeDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val recPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recPulse"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Трекер корта и табло счёта",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Озвучка через Bluetooth • Правила FIP Падел",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTracking) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = recPulse))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "60 FPS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_navigate_to_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ==========================================
        // БОЛЬШОЕ ТАБЛО СЧЕТА МАТЧА ДЛЯ КОРТА
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_score_board_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Статус сетов и геймов
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Индикатор звука (компактный кликабельный бейдж)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isVoiceEnabled) {
                                if (useAliceVoice) Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color(0xFF22C55E).copy(alpha = 0.15f)
                            } else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleVoiceEnabled() }
                                .testTag("btn_toggle_voice_quick")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = null,
                                    tint = if (!isVoiceEnabled) Color.Gray else if (useAliceVoice) Color(0xFF8B5CF6) else Color(0xFF22C55E),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (!isVoiceEnabled) "Без звука" else if (useAliceVoice) currentVoice.title else "Системный",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (!isVoiceEnabled) Color.Gray else if (useAliceVoice) Color(0xFF8B5CF6) else Color(0xFF22C55E),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Режим игры
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showScoreModeDialog = true }
                                .testTag("btn_change_score_mode")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (scoreState.scoreMode) {
                                        com.example.model.ScoreMode.AMERICANO -> "АМЕРИКАНО (до ${scoreState.americanoTargetPoints})"
                                        com.example.model.ScoreMode.GOLDEN_POINT -> "GOLDEN POINT"
                                        com.example.model.ScoreMode.TRADITIONAL_ADVANTAGE -> "ADVANTAGE"
                                        com.example.model.ScoreMode.TIE_BREAK_ONLY -> "ТАЙ-БРЕЙК"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (scoreState.scoreMode == com.example.model.ScoreMode.AMERICANO) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val totalPlayed = (scoreState.team1PointsDisplay.toIntOrNull() ?: 0) + (scoreState.team2PointsDisplay.toIntOrNull() ?: 0)
                            val remaining = (scoreState.americanoTargetPoints - totalPlayed).coerceAtLeast(0)
                            Text(
                                text = "Разыграно: $totalPlayed из ${scoreState.americanoTargetPoints} очков",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (scoreState.isMatchFinished) "МАТЧ ЗАВЕРШЁН" else "Осталось: $remaining очков",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (scoreState.isMatchFinished) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }
                    }

                    // Матч и турнир
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆 ${currentMatch.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = currentMatch.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Очки команд (крупно и симметрично)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ближние
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "БЛИЖНИЕ (ТЫ)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (scoreState.servingTeam == 1) Color(0xFF22D3EE) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${currentMatch.playerNear1} & ${currentMatch.playerNear2}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Фиксированное пространство под бейдж подачи для идеальной симметрии
                            Box(
                                modifier = Modifier.height(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (scoreState.servingTeam == 1) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF22D3EE).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "ПОДАЧА",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF22D3EE)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scoreState.team1PointsDisplay,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("team1_points_text")
                            )
                            Text(
                                text = "Сеты: ${scoreState.team1Sets}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = ":",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Дальние
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "ДАЛЬНИЕ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (scoreState.servingTeam == 2) Color(0xFFF97316) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${currentMatch.playerFar1} & ${currentMatch.playerFar2}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Фиксированное пространство под бейдж подачи для идеальной симметрии
                            Box(
                                modifier = Modifier.height(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (scoreState.servingTeam == 2) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFF97316).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "ПОДАЧА",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF97316)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scoreState.team2PointsDisplay,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("team2_points_text")
                            )
                            Text(
                                text = "Сеты: ${scoreState.team2Sets}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Индикатор статуса подачи: 1-я или 2-я подача
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (scoreState.serveAttempt == 1) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val serverTeamName = if (scoreState.servingTeam == 1) "Ближние" else "Дальние"
                            Text(
                                text = "🎾 Подаёт: $serverTeamName • ${if (scoreState.serveAttempt == 1) "1-я Подача" else "⚠️ ВТОРАЯ ПОДАЧА"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (scoreState.serveAttempt == 1) Color(0xFF10B981) else Color(0xFFD97706)
                            )
                            Text(
                                text = if (scoreState.serveAttempt == 1) "Без ошибок" else "Риск двойной ошибки!",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (scoreState.serveAttempt == 1) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFD97706)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // КНОПКИ УПРАВЛЕНИЯ СЧЕТОМ (УДОБНО НА КОРТЕ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Ближние (+1 и -1)
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.addPointTeam1(triggerVoice = true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("score_add_team1_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("+1 Ближние", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.deductPointTeam1() },
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(52.dp)
                                    .testTag("score_minus_team1_button"),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("–1", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        // Дальние (+1 и -1)
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.addPointTeam2(triggerVoice = true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("score_add_team2_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("+1 Дальние", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.deductPointTeam2() },
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(52.dp)
                                    .testTag("score_minus_team2_button"),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("–1", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // КНОПКИ ДЛЯ ПЕРЕПОДАЧИ (LET) И ОШИБКИ ПОДАЧИ (FAULT)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.registerLet() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_score_let_replay"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🔄 Переподача (Let)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.registerFault() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_score_fault"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (scoreState.serveAttempt == 1) "❌ Ошибка (Вторая)" else "❌ Двойная ошибка",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (scoreState.serveAttempt == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Вспомогательные кнопки: Повторить, Отмена очка, Сброс
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.repeatScoreVoice() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("repeat_score_button")
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Повторить", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { viewModel.undoScore() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("undo_score_button")
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Отмена", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetMatchScore() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("reset_match_score_button")
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сброс", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Видеокадр с наложением гомографии
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("live_camera_preview_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (useRealCameraPreview) {
                        RealCameraPreview(viewModel = viewModel)
                        // Наложение контуров корта и трекинга поверх живого кадра
                        LiveCameraTrackingOverlay(
                            calibrationPoints = calibrationPoints,
                            ballPos = ballPos,
                            isTracking = isTracking
                        )
                    } else {
                        // Виртуальный кадр с перспективой корта
                        VirtualCourtCameraFeed(
                            calibrationPoints = calibrationPoints,
                            ballPos = ballPos,
                            isTracking = isTracking
                        )
                    }

                    // Бейдж переключения источника камеры
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (useRealCameraPreview) "Штатная Камера" else "Виртуальный корт",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { useRealCameraPreview = !useRealCameraPreview },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Switch Camera Source",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Плашка со скоростью текущего удара
                    if (liveSpeed > 0f) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color(0xFFEAB308),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${liveSpeed.toInt()} км/ч",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEAB308),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // ПАНЕЛЬ УПРАВЛЕНИЯ ТРЕКИНГОМ
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleTracking() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTracking) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("toggle_tracking_button")
                    ) {
                        Icon(
                            imageVector = if (isTracking) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTracking) "Остановить трекинг корта" else "Запустить трекинг (60 FPS)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "NPU • ${npuStats.inferenceTimeMs}ms",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp
                                )
                            }
                            if (autoCvScoreEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Авто-очки CV",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("btn_court_settings_link")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Все настройки", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showScoreModeDialog) {
        ScoreModeSelectionDialog(
            currentMode = scoreState.scoreMode,
            currentTargetPoints = scoreState.americanoTargetPoints,
            onDismiss = { showScoreModeDialog = false },
            onSelectMode = { mode ->
                viewModel.setScoreMode(mode)
            },
            onSelectAmericanoTarget = { target ->
                viewModel.setAmericanoTargetPoints(target)
            }
        )
    }
}

@Composable
fun ScoreModeSelectionDialog(
    currentMode: com.example.model.ScoreMode,
    currentTargetPoints: Int,
    onDismiss: () -> Unit,
    onSelectMode: (com.example.model.ScoreMode) -> Unit,
    onSelectAmericanoTarget: (Int) -> Unit
) {
    var selectedTarget by remember { mutableStateOf(currentTargetPoints) }
    var customInput by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Формат подсчета очков", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Выберите систему ведения счёта для матча:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Форматы: Американо (по умолчанию), Golden Point, Advantage, Тай-брейк
                com.example.model.ScoreMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectMode(mode)
                            }
                            .testTag("score_mode_option_${mode.name}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.displayName,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (mode == com.example.model.ScoreMode.AMERICANO) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "ПО УМОЛЧАНИЮ",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mode.shortDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Настройка очков для «Американо»
                if (currentMode == com.example.model.ScoreMode.AMERICANO) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Сумма очков турнира Американо:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Быстрый выбор: 15, 21, 24, 32 очков
                    val presets = listOf(15, 21, 24, 32)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { pts ->
                            val isPresetActive = !isCustomSelected && selectedTarget == pts
                            FilterChip(
                                selected = isPresetActive,
                                onClick = {
                                    isCustomSelected = false
                                    selectedTarget = pts
                                    onSelectAmericanoTarget(pts)
                                },
                                label = { Text("$pts", fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Свой вариант
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { input ->
                                customInput = input.filter { it.isDigit() }
                                val customInt = customInput.toIntOrNull()
                                if (customInt != null && customInt in 4..120) {
                                    isCustomSelected = true
                                    selectedTarget = customInt
                                    onSelectAmericanoTarget(customInt)
                                }
                            },
                            label = { Text("Свой лимит очков") },
                            placeholder = { Text("напр. 28, 40") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}

@Composable
fun RealCameraPreview(viewModel: PadelViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(ctx), viewModel.frameAnalyzer)
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun VirtualCourtCameraFeed(
    calibrationPoints: List<com.example.model.CourtCalibrationPoint>,
    ballPos: Pair<Float, Float>?,
    isTracking: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Фон видеокадра (синий корт с задней стены)
        drawRect(Color(0xFF0F1E36))

        val tl = calibrationPoints.getOrNull(0)
        val tr = calibrationPoints.getOrNull(1)
        val br = calibrationPoints.getOrNull(2)
        val bl = calibrationPoints.getOrNull(3)

        if (tl != null && tr != null && br != null && bl != null) {
            val pTL = Offset(tl.screenX * w, tl.screenY * h)
            val pTR = Offset(tr.screenX * w, tr.screenY * h)
            val pBR = Offset(br.screenX * w, br.screenY * h)
            val pBL = Offset(bl.screenX * w, bl.screenY * h)

            // Контур корта
            val courtPath = Path().apply {
                moveTo(pTL.x, pTL.y)
                lineTo(pTR.x, pTR.y)
                lineTo(pBR.x, pBR.y)
                lineTo(pBL.x, pBL.y)
                close()
            }

            drawPath(
                path = courtPath,
                color = Color(0xFF1E3A8A).copy(alpha = 0.5f)
            )
            drawPath(
                path = courtPath,
                color = Color(0xFF60A5FA),
                style = Stroke(width = 3.dp.toPx())
            )

            // Сетка (на 50% длины перспективы)
            val netL = Offset((pTL.x + pBL.x) * 0.5f, (pTL.y + pBL.y) * 0.5f)
            val netR = Offset((pTR.x + pBR.x) * 0.5f, (pTR.y + pBR.y) * 0.5f)
            drawLine(
                color = Color.White,
                start = netL,
                end = netR,
                strokeWidth = 3.dp.toPx()
            )
        }

        // Положение детектированного мяча
        if (isTracking && ballPos != null) {
            val ballOffset = Offset(ballPos.first * w, ballPos.second * h)
            // Тень мяча
            drawCircle(
                color = Color.Black.copy(alpha = 0.4f),
                radius = 10.dp.toPx(),
                center = ballOffset.copy(y = ballOffset.y + 6.dp.toPx())
            )
            // Мяч
            drawCircle(
                color = Color(0xFFEAB308),
                radius = 8.dp.toPx(),
                center = ballOffset
            )
            // Ореол детектора
            drawCircle(
                color = Color(0xFF22C55E),
                radius = 16.dp.toPx(),
                center = ballOffset,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun LiveCameraTrackingOverlay(
    calibrationPoints: List<com.example.model.CourtCalibrationPoint>,
    ballPos: Pair<Float, Float>?,
    isTracking: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val tl = calibrationPoints.getOrNull(0)
        val tr = calibrationPoints.getOrNull(1)
        val br = calibrationPoints.getOrNull(2)
        val bl = calibrationPoints.getOrNull(3)

        if (tl != null && tr != null && br != null && bl != null) {
            val pTL = Offset(tl.screenX * w, tl.screenY * h)
            val pTR = Offset(tr.screenX * w, tr.screenY * h)
            val pBR = Offset(br.screenX * w, br.screenY * h)
            val pBL = Offset(bl.screenX * w, bl.screenY * h)

            val courtPath = Path().apply {
                moveTo(pTL.x, pTL.y)
                lineTo(pTR.x, pTR.y)
                lineTo(pBR.x, pBR.y)
                lineTo(pBL.x, pBL.y)
                close()
            }

            // Полупрозрачная подсветка зоны корта
            drawPath(
                path = courtPath,
                color = Color(0x2238BDF8)
            )
            // Контур корта
            drawPath(
                path = courtPath,
                color = Color(0xFF38BDF8),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )
            )

            // Сетка
            val netL = Offset((pTL.x + pBL.x) * 0.5f, (pTL.y + pBL.y) * 0.5f)
            val netR = Offset((pTR.x + pBR.x) * 0.5f, (pTR.y + pBR.y) * 0.5f)
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = netL,
                end = netR,
                strokeWidth = 2.dp.toPx()
            )
        }

        // Положение детектированного мяча поверх кадра камеры
        if (isTracking && ballPos != null) {
            val ballOffset = Offset(ballPos.first * w, ballPos.second * h)
            drawCircle(
                color = Color(0xFFEAB308),
                radius = 9.dp.toPx(),
                center = ballOffset
            )
            drawCircle(
                color = Color(0xFF22C55E),
                radius = 18.dp.toPx(),
                center = ballOffset,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}

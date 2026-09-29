package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cv.AccelerationBackend
import com.example.viewmodel.PadelViewModel

@Composable
fun SettingsScreen(
    viewModel: PadelViewModel,
    onNavigateToCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsStateWithLifecycle()
    val useAliceVoice by viewModel.useAliceVoice.collectAsStateWithLifecycle()
    val currentVoice by viewModel.currentSpeechKitVoice.collectAsStateWithLifecycle()
    val isAlicePrewarming by viewModel.isAlicePrewarming.collectAsStateWithLifecycle()
    val autoCvScoreEnabled by viewModel.autoCvScoreEnabled.collectAsStateWithLifecycle()
    val npuStats by viewModel.npuStats.collectAsStateWithLifecycle()
    val scoreState by viewModel.scoreState.collectAsStateWithLifecycle()

    var showScoreModeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    text = "Настройки системы",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Параметры матча, озвучки, NPU нейросети и калибровки",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ==========================================
        // РАЗДЕЛ 1: ПРАВИЛА И ФОРМАТ МАТЧА
        // ==========================================
        item {
            Text(
                text = "ФОРМАТ МАТЧА",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Режим игры и счёт", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = when (scoreState.scoreMode) {
                                    com.example.model.ScoreMode.AMERICANO -> "Американо (${scoreState.americanoTargetPoints} очков, каждые 4 подачи)"
                                    com.example.model.ScoreMode.GOLDEN_POINT -> "Классический с решающим мячом (Golden Point)"
                                    com.example.model.ScoreMode.TRADITIONAL_ADVANTAGE -> "Классический с больше/меньше (Advantage)"
                                    com.example.model.ScoreMode.TIE_BREAK_ONLY -> "Одиночный тай-брейк до 7 очков"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showScoreModeDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_settings_change_score_mode")
                        ) {
                            Text("Изменить", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Авто-подсчет очков по CV", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Начислять очко при виннере или двойном отскоке",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoCvScoreEnabled,
                            onCheckedChange = { viewModel.toggleAutoCvScore() },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("settings_auto_cv_score_switch")
                        )
                    }
                }
            }
        }

        // ==========================================
        // РАЗДЕЛ 2: ГОЛОСОВАЯ ОЗВУЧКА И ОФФЛАЙН КЭШ
        // ==========================================
        item {
            Text(
                text = "ОЗВУЧКА НА БЛЮТУЗ-КОЛОНКУ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Голосовые объявления", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Объявление счёта, ошибок подачи и матчболов",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isVoiceEnabled,
                            onCheckedChange = { viewModel.toggleVoiceEnabled() },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("settings_voice_switch")
                        )
                    }

                    if (isVoiceEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Синтез речи SpeechKit", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (useAliceVoice) "Голос: ${currentVoice.title} (${currentVoice.description})" else "Системный синтезатор Android",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (useAliceVoice) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useAliceVoice,
                                onCheckedChange = { viewModel.toggleAliceVoice() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF8B5CF6),
                                    checkedTrackColor = Color(0xFF8B5CF6).copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.testTag("settings_speechkit_voice_switch")
                            )
                        }

                        if (useAliceVoice) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Выбор голоса комментатора:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                com.example.audio.SpeechKitVoice.values().forEach { voice ->
                                    val isSelected = currentVoice == voice
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8B5CF6)) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.selectSpeechKitVoice(voice) }
                                            .testTag("voice_option_${voice.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = voice.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = if (isSelected) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = voice.gender,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = voice.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.VolumeUp,
                                                    contentDescription = null,
                                                    tint = Color(0xFF8B5CF6),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Офлайн кэширование (${currentVoice.title})", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(
                                            "Скачать все игровые фразы с голосом ${currentVoice.title} для 100% игры без интернета",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.prewarmAliceVoiceCache() },
                                        enabled = !isAlicePrewarming,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                        modifier = Modifier.testTag("settings_btn_prewarm_cache")
                                    ) {
                                        Text(
                                            text = if (isAlicePrewarming) "Загрузка..." else "📥 Скачать кэш",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // РАЗДЕЛ 3: НЕЙРОСЕТЬ И АППАРАТНОЕ УСКОРЕНИЕ (NPU / GPU)
        // ==========================================
        item {
            Text(
                text = "НЕЙРОСЕТЬ И АППАРАТНОЕ УСКОРЕНИЕ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Qualcomm Hexagon NPU (HTP)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${npuStats.inferenceTimeMs} ms • ${npuStats.fps.toInt()} FPS",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Text(
                        text = "Текущая модель: ${npuStats.modelPrecision}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Входной тензор: Статический shape (1, 640, 640, 3) INT8 (w8a8)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Выбор активной нейросети
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isBallActive = npuStats.modelPrecision.contains("padel_ball")
                        OutlinedButton(
                            onClick = { viewModel.switchActiveModel(isPoseModel = false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (isBallActive) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("Padel Ball INT8", fontSize = 11.sp, fontWeight = if (isBallActive) FontWeight.Bold else FontWeight.Normal)
                        }

                        OutlinedButton(
                            onClick = { viewModel.switchActiveModel(isPoseModel = true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (!isBallActive) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("YOLO11n-Pose INT8", fontSize = 11.sp, fontWeight = if (!isBallActive) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    Text(
                        text = "Аппаратный бэкенд вычислений:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccelerationBackend.values().forEach { backend ->
                            val isSelected = npuStats.backend == backend
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setAccelerationBackend(backend) }
                                    .testTag("backend_${backend.name}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when(backend) {
                                            AccelerationBackend.QUALCOMM_NPU_QNN -> "Hexagon NPU"
                                            AccelerationBackend.QUALCOMM_GPU_OPENCL -> "Adreno GPU"
                                            AccelerationBackend.NNAPI_HARDWARE -> "NNAPI"
                                            AccelerationBackend.CPU_MULTITHREAD -> "Oryon CPU"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // РАЗДЕЛ 4: КАЛИБРОВКА КОРТА
        // ==========================================
        item {
            Text(
                text = "ГЕОМЕТРИЯ КОРТА",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCalibration() }
                    .testTag("settings_calibration_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CropFree,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text("Калибровка гомографии корта", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Настройка 4 углов 10x20м по камере",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
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

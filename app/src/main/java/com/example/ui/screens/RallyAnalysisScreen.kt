package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PadelRally
import com.example.model.PadelShot
import com.example.model.ShotOutcome
import com.example.ui.components.ColorCodingMode
import com.example.ui.components.CourtVisualMode
import com.example.ui.components.PadelCourtCanvas
import com.example.ui.components.getShotColor
import com.example.viewmodel.PadelViewModel

@Composable
fun RallyAnalysisScreen(
    viewModel: PadelViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allShots by viewModel.allShots.collectAsStateWithLifecycle()
    val rallies = viewModel.rallies

    var expandedRallyId by remember { mutableStateOf<Int?>(1) }
    var focusedRallyShots by remember {
        mutableStateOf(allShots.filter { it.rallyId == 1 })
    }
    var activePlaybackIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Хронология розыгрышей (Rallies)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Пошаговый разбор эпизодов тай-брейка из матча",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Mini Court Viewer for the active expanded rally
        item {
            val rallyShots = allShots.filter { it.rallyId == (expandedRallyId ?: 1) }
            val displayedShots = if (activePlaybackIndex != null) {
                rallyShots.take(activePlaybackIndex!! + 1)
            } else {
                rallyShots
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("rally_court_preview"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Розыгрыш #${expandedRallyId ?: 1} на корте",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    val count = rallyShots.size
                                    if (count > 0) {
                                        activePlaybackIndex = ((activePlaybackIndex ?: -1) + 1) % count
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("step_rally_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Шаг удара",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (activePlaybackIndex != null) "Удар ${activePlaybackIndex!! + 1}/${rallyShots.size}" else "Воспроизвести",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PadelCourtCanvas(
                        shots = displayedShots,
                        selectedShot = displayedShots.lastOrNull(),
                        visualMode = CourtVisualMode.TRAJECTORY,
                        colorMode = ColorCodingMode.BY_SHOT_TYPE,
                        modifier = Modifier.height(280.dp)
                    )
                }
            }
        }

        // List of all rallies
        items(rallies) { rally ->
            val rallyShots = allShots.filter { it.rallyId == rally.id }
            val isExpanded = expandedRallyId == rally.id

            RallyCard(
                rally = rally,
                shots = rallyShots,
                isExpanded = isExpanded,
                onClick = {
                    expandedRallyId = if (isExpanded) null else rally.id
                    activePlaybackIndex = null
                },
                onSelectShot = { shot ->
                    viewModel.selectShot(shot)
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun RallyCard(
    rally: PadelRally,
    shots: List<PadelShot>,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onSelectShot: (PadelShot) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("rally_item_${rally.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (rally.winnerTeam == "Near") MaterialTheme.colorScheme.primaryContainer
                                else Color(0xFFEF4444).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${rally.rallyNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (rally.winnerTeam == "Near") MaterialTheme.colorScheme.primary else Color(0xFFEF4444)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Счёт: ${rally.score}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Подавал: ${rally.server} • ${rally.durationSec} сек • ${shots.size} удара",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = rally.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Expanded Shots List
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Последовательность касаний:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    shots.forEach { shot ->
                        ShotStepRow(shot = shot, onClick = { onSelectShot(shot) })
                    }
                }
            }
        }
    }
}

@Composable
fun ShotStepRow(shot: PadelShot, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(getShotColor(shot, ColorCodingMode.BY_SHOT_TYPE))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "${shot.shotType.displayName} (${shot.player})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Отскок: (${String.format("%.1f", shot.bounceX)}, ${String.format("%.1f", shot.bounceY)})м",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${shot.speedKmh.toInt()} км/ч",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (shot.outcome.isSuccess) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = shot.outcome.displayName.substringBefore(" "),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (shot.outcome.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

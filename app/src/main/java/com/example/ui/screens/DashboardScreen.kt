package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PadelShot
import com.example.model.ShotOutcome
import com.example.model.ShotType
import com.example.ui.components.ColorCodingMode
import com.example.ui.components.CourtVisualMode
import com.example.ui.components.PadelCourtCanvas
import com.example.ui.components.getShotColor
import com.example.viewmodel.PadelViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: PadelViewModel,
    onNavigateToRallies: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMatch by viewModel.currentMatch.collectAsStateWithLifecycle()
    val filteredShots by viewModel.filteredShots.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val selectedShot by viewModel.selectedShot.collectAsStateWithLifecycle()

    val visualMode by viewModel.visualMode.collectAsStateWithLifecycle()
    val colorMode by viewModel.colorMode.collectAsStateWithLifecycle()
    val selectedTeam by viewModel.selectedTeam.collectAsStateWithLifecycle()
    val selectedShotType by viewModel.selectedShotType.collectAsStateWithLifecycle()
    val selectedOutcome by viewModel.selectedOutcome.collectAsStateWithLifecycle()
    val minSpeed by viewModel.minSpeedKmh.collectAsStateWithLifecycle()
    val selectedRallyId by viewModel.selectedRallyId.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Match Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("match_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentMatch.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = currentMatch.scoreFinal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${currentMatch.location} • ${currentMatch.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currentMatch.courtType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // KPI Metric Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Ударов",
                    value = "${stats.totalShots}",
                    icon = Icons.Default.SportsTennis,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Ср. скорость",
                    value = "${stats.avgSpeedKmh.toInt()} км/ч",
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Макс. скорость",
                    value = "${stats.maxSpeedKmh.toInt()} км/ч",
                    icon = Icons.Default.FlashOn,
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Точность",
                    value = "${stats.inPercentage.toInt()}%",
                    icon = Icons.Default.Assessment,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Court Visualization Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("court_visualizer_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Mode Switcher Tabs
                    TabRow(
                        selectedTabIndex = visualMode.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[visualMode.ordinal]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = visualMode == CourtVisualMode.SCATTER,
                            onClick = { viewModel.setVisualMode(CourtVisualMode.SCATTER) },
                            text = { Text("Точки (Scatter)") }
                        )
                        Tab(
                            selected = visualMode == CourtVisualMode.HEATMAP,
                            onClick = { viewModel.setVisualMode(CourtVisualMode.HEATMAP) },
                            text = { Text("Тепловая карта") }
                        )
                        Tab(
                            selected = visualMode == CourtVisualMode.TRAJECTORY,
                            onClick = { viewModel.setVisualMode(CourtVisualMode.TRAJECTORY) },
                            text = { Text("Векторы") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color Coding Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Цвет:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ColorCodingChip("Тип удара", colorMode == ColorCodingMode.BY_SHOT_TYPE) {
                            viewModel.setColorMode(ColorCodingMode.BY_SHOT_TYPE)
                        }
                        ColorCodingChip("Скорость", colorMode == ColorCodingMode.BY_SPEED) {
                            viewModel.setColorMode(ColorCodingMode.BY_SPEED)
                        }
                        ColorCodingChip("Исход", colorMode == ColorCodingMode.BY_OUTCOME) {
                            viewModel.setColorMode(ColorCodingMode.BY_OUTCOME)
                        }
                        ColorCodingChip("Игрок", colorMode == ColorCodingMode.BY_PLAYER) {
                            viewModel.setColorMode(ColorCodingMode.BY_PLAYER)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2D Padel Court Canvas
                    PadelCourtCanvas(
                        shots = filteredShots,
                        selectedShot = selectedShot,
                        visualMode = visualMode,
                        colorMode = colorMode,
                        onShotSelected = { viewModel.selectShot(it) },
                        onCourtTapped = { x, y ->
                            viewModel.addManualBounce(x, y)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Нажмите на любую точку отскока для просмотра деталей или на свободное место корта для добавления замера",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Selected Shot Details Sheet / Card (if any selected)
        if (selectedShot != null) {
            item {
                SelectedShotDetailsCard(
                    shot = selectedShot!!,
                    onClose = { viewModel.selectShot(null) }
                )
            }
        }

        // Filters Section (Tableau Slice-and-Dice)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filters_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Фильтры",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Фильтры аналитики (${filteredShots.size} ударов)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (selectedTeam != "All" || selectedShotType != null || selectedOutcome != null || minSpeed > 0f || selectedRallyId != null) {
                            Text(
                                text = "Сбросить",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { viewModel.clearFilters() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Team filter
                    Text(
                        text = "Сторона корта:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTeam == "All",
                            onClick = { viewModel.setTeamFilter("All") },
                            label = { Text("Все") }
                        )
                        FilterChip(
                            selected = selectedTeam == "Near",
                            onClick = { viewModel.setTeamFilter("Near") },
                            label = { Text("Ближняя (вы)") }
                        )
                        FilterChip(
                            selected = selectedTeam == "Far",
                            onClick = { viewModel.setTeamFilter("Far") },
                            label = { Text("Дальняя (соперники)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shot Types Chips
                    Text(
                        text = "Тип удара:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedShotType == null,
                            onClick = { viewModel.setShotTypeFilter(null) },
                            label = { Text("Все типы") }
                        )
                        ShotType.values().forEach { type ->
                            FilterChip(
                                selected = selectedShotType == type,
                                onClick = {
                                    viewModel.setShotTypeFilter(if (selectedShotType == type) null else type)
                                },
                                label = { Text(type.shortName) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Min Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Минимальная скорость: ${minSpeed.toInt()} км/ч",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Slider(
                        value = minSpeed,
                        onValueChange = { viewModel.setMinSpeed(it) },
                        valueRange = 0f..120f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Shot Type Breakdown Statistics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Распределение ударов в матче",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ShotTypeRow("Смэши (Smash)", stats.smashCount, Color(0xFFF43F5E), filteredShots.size)
                    ShotTypeRow("Бандехи (Bandeja)", stats.bandejaCount, Color(0xFF8B5CF6), filteredShots.size)
                    ShotTypeRow("Воллеи у сетки (Volley)", stats.volleyCount, Color(0xFF06B6D4), filteredShots.size)
                    ShotTypeRow("Чистые виннеры", stats.winnerCount, Color(0xFFCCFF00), filteredShots.size)
                    ShotTypeRow("Ошибки / Аут", stats.unforcedErrorCount, Color(0xFFEF4444), filteredShots.size)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ColorCodingChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SelectedShotDetailsCard(
    shot: PadelShot,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_shot_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(getShotColor(shot, ColorCodingMode.BY_SHOT_TYPE))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Удар #${shot.shotNumber} (Розыгрыш #${shot.rallyId})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Игрок: ${shot.player}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Тип: ${shot.shotType.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Координаты корта: X=${String.format("%.2f", shot.bounceX)}м, Y=${String.format("%.2f", shot.bounceY)}м",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${shot.speedKmh.toInt()} км/ч",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = shot.outcome.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (shot.outcome.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }

            if (shot.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 ${shot.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ShotTypeRow(
    title: String,
    count: Int,
    barColor: Color,
    totalCount: Int
) {
    val pct = if (totalCount > 0) count.toFloat() / totalCount else 0f
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "$count (${(pct * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}

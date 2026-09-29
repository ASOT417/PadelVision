package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PadelMatch
import com.example.model.PadelPlayer
import com.example.viewmodel.PadelViewModel

@Composable
fun MatchesAndPlayersScreen(
    viewModel: PadelViewModel,
    onNavigateToCourtTracker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Матчи, 1: Игроки (CV)
    var showCreateMatchDialog by remember { mutableStateOf(false) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }

    val matches by viewModel.matchesList.collectAsStateWithLifecycle()
    val players by viewModel.playersList.collectAsStateWithLifecycle()
    val activeMatchId by viewModel.activeMatchId.collectAsStateWithLifecycle()
    val recognizedCandidates by viewModel.recognizedPlayers.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showCreateMatchDialog = true
                    else showAddPlayerDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_match_or_player")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "Создать матч" else "Добавить игрока"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Вкладки: Матчи и История / Игроки и Распознавание
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("История матчей (${matches.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_matches_history")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Игроки & CV (${players.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_players_recognition")
                )
            }

            if (selectedTab == 0) {
                // Список матчей
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Матчи и турниры",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { showCreateMatchDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_create_new_match_header")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Новый матч", fontSize = 12.sp)
                            }
                        }
                    }

                    items(matches, key = { it.id }) { match ->
                        val isActive = match.id == activeMatchId
                        MatchHistoryCard(
                            match = match,
                            isActive = isActive,
                            onSelect = {
                                viewModel.selectMatch(match.id)
                                onNavigateToCourtTracker()
                            },
                            onDelete = { viewModel.deleteMatch(match.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                // Раздел игроков и CV-распознавания
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "CV-Распознавание игроков",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Модель YOLO11n-Pose определяет позицию на корте (Drive/Reverse) и привязывает удары по цвету ракетки и футболки.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Активная расстановка на корте 10x20м:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(recognizedCandidates) { candidate ->
                        RecognizedCandidateCard(candidate = candidate)
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Зарегистрированные профили игроков:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showAddPlayerDialog = true }) {
                                Text("+ Добавить")
                            }
                        }
                    }

                    items(players, key = { it.id }) { player ->
                        PlayerProfileCard(
                            player = player,
                            onDelete = { viewModel.deletePlayer(player.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // ==========================================
    // ДИАЛОГ СОЗДАНИЯ МАТЧА
    // ==========================================
    if (showCreateMatchDialog) {
        CreateMatchDialog(
            players = players,
            onDismiss = { showCreateMatchDialog = false },
            onCreate = { title, location, courtType, pn1, pn2, pf1, pf2, mode, americanoPts ->
                viewModel.createNewMatch(title, location, courtType, pn1, pn2, pf1, pf2, mode, americanoPts)
                showCreateMatchDialog = false
                onNavigateToCourtTracker()
            }
        )
    }

    // ==========================================
    // ДИАЛОГ ДОБАВЛЕНИЯ ИГРОКА (ДЛЯ РАСПОЗНАВАНИЯ)
    // ==========================================
    if (showAddPlayerDialog) {
        AddPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onAdd = { name, team, side, racket, shirt, emoji ->
                viewModel.addPlayer(name, team, side, racket, shirt, emoji)
                showAddPlayerDialog = false
            }
        )
    }
}

@Composable
fun MatchHistoryCard(
    match: PadelMatch,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("match_card_${match.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "АКТИВНЫЙ МАТЧ",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = match.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить матч",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = match.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = match.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Составы команд
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔵 Ближние: ${match.playerNear1} & ${match.playerNear2}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🟠 Дальние: ${match.playerFar1} & ${match.playerFar2}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Статистика матча
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SportsTennis, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${match.totalShots} ударов", style = MaterialTheme.typography.labelSmall)

                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEAB308))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "макс ${match.maxSpeedKmh.toInt()} км/ч", style = MaterialTheme.typography.labelSmall)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = match.scoreFinal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun RecognizedCandidateCard(candidate: com.example.cv.RecognizedPlayerCandidate) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (candidate.team == "Near") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = candidate.matchedPlayer?.avatarEmoji ?: "👤",
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = candidate.matchedPlayer?.name ?: candidate.positionOnCourt,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Позиция: ${candidate.positionOnCourt} • ${candidate.detectedColorHex}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${(candidate.matchConfidence * 100).toInt()}% CV",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerProfileCard(
    player: PadelPlayer,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = player.avatarEmoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = player.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${player.preferredSide} • Ракетка: ${player.racketColor}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Форма: ${player.shirtColor} • Команда: ${if (player.defaultTeam == "Near") "Ближние" else "Дальние"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun CreateMatchDialog(
    players: List<PadelPlayer>,
    onDismiss: () -> Unit,
    onCreate: (title: String, location: String, courtType: String, pn1: String, pn2: String, pf1: String, pf2: String, mode: com.example.model.ScoreMode, americanoPts: Int) -> Unit
) {
    var title by remember { mutableStateOf("Финал: Ближние vs Дальние") }
    var location by remember { mutableStateOf("Padel Arena Center") }
    var courtType by remember { mutableStateOf("Падел-корт 10x20м (Mondo Supercourt)") }

    var pNear1 by remember { mutableStateOf(players.getOrNull(0)?.name ?: "Ближний левый (Drive)") }
    var pNear2 by remember { mutableStateOf(players.getOrNull(1)?.name ?: "Никита Ф. (Reverse)") }
    var pFar1 by remember { mutableStateOf(players.getOrNull(2)?.name ?: "Дальний левый (Drive)") }
    var pFar2 by remember { mutableStateOf(players.getOrNull(3)?.name ?: "Дальний правый (Reverse)") }

    var selectedScoreMode by remember { mutableStateOf(com.example.model.ScoreMode.AMERICANO) }
    var americanoPts by remember { mutableStateOf(24) }
    var customPtsInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать новый матч", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название матча") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Формат подсчета очков (по умолчанию Американо)
                Text("Формат подсчета очков:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedScoreMode == com.example.model.ScoreMode.AMERICANO,
                        onClick = { selectedScoreMode = com.example.model.ScoreMode.AMERICANO },
                        label = { Text("Американо", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedScoreMode == com.example.model.ScoreMode.GOLDEN_POINT,
                        onClick = { selectedScoreMode = com.example.model.ScoreMode.GOLDEN_POINT },
                        label = { Text("Теннисный (GP)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (selectedScoreMode == com.example.model.ScoreMode.AMERICANO) {
                    Text("Лимит очков турнира Американо:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(15, 21, 24, 32).forEach { pts ->
                            FilterChip(
                                selected = americanoPts == pts && customPtsInput.isBlank(),
                                onClick = {
                                    americanoPts = pts
                                    customPtsInput = ""
                                },
                                label = { Text("$pts") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customPtsInput,
                        onValueChange = { input ->
                            customPtsInput = input.filter { it.isDigit() }
                            val num = customPtsInput.toIntOrNull()
                            if (num != null && num in 4..120) {
                                americanoPts = num
                            }
                        },
                        label = { Text("Или свой лимит (напр. 28)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Локация / Клуб") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Игроки вашей команды (Ближние):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pNear1,
                        onValueChange = { pNear1 = it },
                        label = { Text("Игрок 1 (Лев)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pNear2,
                        onValueChange = { pNear2 = it },
                        label = { Text("Игрок 2 (Прав)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Игроки соперников (Дальние):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pFar1,
                        onValueChange = { pFar1 = it },
                        label = { Text("Игрок 3 (Лев)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pFar2,
                        onValueChange = { pFar2 = it },
                        label = { Text("Игрок 4 (Прав)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, location, courtType, pNear1, pNear2, pFar1, pFar2, selectedScoreMode, americanoPts) }
            ) {
                Text("Начать матч")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, team: String, side: String, racket: String, shirt: String, emoji: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var team by remember { mutableStateOf("Near") }
    var side by remember { mutableStateOf("Левый (Drive)") }
    var racket by remember { mutableStateOf("Черная (Babolat)") }
    var shirt by remember { mutableStateOf("Темная футболка") }
    var emoji by remember { mutableStateOf("🎾") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый игрок (для распознавания)", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя / Фамилия игрока") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Команда:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = team == "Near",
                        onClick = { team = "Near" },
                        label = { Text("Ближние") }
                    )
                    FilterChip(
                        selected = team == "Far",
                        onClick = { team = "Far" },
                        label = { Text("Дальние") }
                    )
                }

                Text("Сторона на корте:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = side.contains("Левый"),
                        onClick = { side = "Левый (Drive)" },
                        label = { Text("Левый (Drive)") }
                    )
                    FilterChip(
                        selected = side.contains("Правый"),
                        onClick = { side = "Правый (Reverse)" },
                        label = { Text("Правый (Reverse)") }
                    )
                }

                OutlinedTextField(
                    value = shirt,
                    onValueChange = { shirt = it },
                    label = { Text("Цвет футболки (признак CV)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = racket,
                    onValueChange = { racket = it },
                    label = { Text("Цвет ракетки (признак CV)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name, team, side, racket, shirt, emoji)
                    }
                }
            ) {
                Text("Сохранить профиль")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

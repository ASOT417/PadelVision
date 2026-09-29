package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.math.HomographyEngine
import com.example.viewmodel.PadelViewModel
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.hypot

@Composable
fun CalibrationScreen(
    viewModel: PadelViewModel,
    modifier: Modifier = Modifier
) {
    val points by viewModel.calibrationPoints.collectAsStateWithLifecycle()
    val homography by viewModel.homographyEngine.collectAsStateWithLifecycle()

    var useRealCamera by remember { mutableStateOf(true) }
    var testPointScreen by remember { mutableStateOf<Pair<Float, Float>?>(Pair(0.5f, 0.5f)) }
    var testPointCourt by remember {
        mutableStateOf(homography.pixelToCourt(0.5f, 0.5f))
    }
    var draggedPointId by remember { mutableStateOf<Int?>(null) }
    var selectedPointId by remember { mutableStateOf<Int?>(1) } // Выбранная точка для стрелочной микро-подстройки
    var isFullscreenCalibration by remember { mutableStateOf(false) }

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
                        text = "Калибровка гомографии корта",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Проекция 4 углов (u, v) камеры в метры корта 10x20м",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Camera Viewport & 4-Point Interactive Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calibration_canvas_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Кадр камеры (1080p @ 60 FPS)",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { useRealCamera = !useRealCamera }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = if (useRealCamera) Color(0xFF10B981) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (useRealCamera) "Камера активна" else "Схема корта",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (useRealCamera) Color(0xFF10B981) else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Кнопка открытия во весь экран для удобной калибровки
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { isFullscreenCalibration = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Во весь экран",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calibration interactive Canvas with CameraX Preview background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF090D16))
                    ) {
                        if (useRealCamera) {
                            RealCameraPreview(viewModel = viewModel)
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("calibration_interactive_canvas")
                                .pointerInput(points) {
                                    detectTapGestures { tapOffset ->
                                        val u = tapOffset.x / size.width
                                        val v = tapOffset.y / size.height
                                        testPointScreen = Pair(u, v)
                                        testPointCourt = homography.pixelToCourt(u, v)
                                    }
                                }
                                .pointerInput(points) {
                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            // Find closest point to drag
                                            val closest = points.minByOrNull { pt ->
                                                val px = pt.screenX * size.width
                                                val py = pt.screenY * size.height
                                                hypot(startOffset.x - px, startOffset.y - py)
                                            }
                                            if (closest != null) {
                                                val px = closest.screenX * size.width
                                                val py = closest.screenY * size.height
                                                if (hypot(startOffset.x - px, startOffset.y - py) < 140f) {
                                                    draggedPointId = closest.id
                                                    selectedPointId = closest.id
                                                }
                                            }
                                        },
                                        onDragEnd = { draggedPointId = null },
                                        onDragCancel = { draggedPointId = null },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            draggedPointId?.let { id ->
                                                val pt = points.find { it.id == id }
                                                if (pt != null) {
                                                    val newX = (pt.screenX * size.width + dragAmount.x) / size.width
                                                    val newY = (pt.screenY * size.height + dragAmount.y) / size.height
                                                    viewModel.updateCalibrationPoint(id, newX, newY)
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            val w = size.width
                            val h = size.height

                            // Draw simulated court camera perspective background only if camera is off
                            if (!useRealCamera) {
                                drawLine(
                                    color = Color(0x3038BDF8),
                                    start = Offset(0f, h * 0.45f),
                                    end = Offset(w, h * 0.45f),
                                    strokeWidth = 1f
                                )
                            }

                            // 1. Draw Homography Quad Polygon
                            if (points.size == 4) {
                                val p1 = Offset(points[0].screenX * w, points[0].screenY * h)
                                val p2 = Offset(points[1].screenX * w, points[1].screenY * h)
                                val p3 = Offset(points[2].screenX * w, points[2].screenY * h)
                                val p4 = Offset(points[3].screenX * w, points[3].screenY * h)

                                val polyPath = Path().apply {
                                    moveTo(p1.x, p1.y)
                                    lineTo(p2.x, p2.y)
                                    lineTo(p3.x, p3.y)
                                    lineTo(p4.x, p4.y)
                                    close()
                                }

                                // Fill court perspective area
                                drawPath(
                                    path = polyPath,
                                    color = Color(0x220284C7)
                                )
                                // Stroke
                                drawPath(
                                    path = polyPath,
                                    color = Color(0xFF38BDF8),
                                    style = Stroke(
                                        width = 3.5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                                    )
                                )

                                // Projected Net line across center of perspective
                                val netLeft = Offset((p1.x + p4.x) / 2f, (p1.y + p4.y) / 2f)
                                val netRight = Offset((p2.x + p3.x) / 2f, (p2.y + p3.y) / 2f)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.6f),
                                    start = netLeft,
                                    end = netRight,
                                    strokeWidth = 2f
                                )
                            }

                            // 2. Draw 4 Control Handle Points
                            points.forEach { pt ->
                                val px = pt.screenX * w
                                val py = pt.screenY * h
                                val isDragged = draggedPointId == pt.id

                                // Outer handle glow
                                drawCircle(
                                    color = Color(0xFF38BDF8).copy(alpha = if (isDragged) 0.5f else 0.25f),
                                    radius = if (isDragged) 24f else 18f,
                                    center = Offset(px, py)
                                )
                                // Inner solid pin
                                drawCircle(
                                    color = Color.White,
                                    radius = 8f,
                                    center = Offset(px, py)
                                )
                                drawCircle(
                                    color = Color(0xFF0284C7),
                                    radius = 6f,
                                    center = Offset(px, py)
                                )
                            }

                            // 3. Draw Test Point (if tapped)
                            testPointScreen?.let { (u, v) ->
                                val tx = u * w
                                val ty = v * h
                                drawCircle(
                                    color = Color(0xFFCCFF00),
                                    radius = 9f,
                                    center = Offset(tx, ty)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 4f,
                                    center = Offset(tx, ty)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Панель выбора угла и точной подстройки стрелками (Nudge D-Pad)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🎯 Точная подстройка выбранного угла (шаг 0.5%):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // 4 вкладки углов
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            points.forEach { pt ->
                                val isSelected = selectedPointId == pt.id
                                val label = when(pt.id) {
                                    1 -> "1: Ближн Л"
                                    2 -> "2: Ближн П"
                                    3 -> "3: Дальн П"
                                    4 -> "4: Дальн Л"
                                    else -> "Точка ${pt.id}"
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedPointId = pt.id }
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Стрелки микро-сдвига (D-Pad)
                        val step = 0.005f // 0.5% сдвига за клик
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val activePt = points.find { it.id == selectedPointId }

                            OutlinedButton(
                                onClick = {
                                    activePt?.let { pt ->
                                        viewModel.updateCalibrationPoint(pt.id, (pt.screenX - step).coerceIn(0f, 1f), pt.screenY)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("◀ Влево", fontSize = 11.sp, color = Color.White)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        activePt?.let { pt ->
                                            viewModel.updateCalibrationPoint(pt.id, pt.screenX, (pt.screenY - step).coerceIn(0f, 1f))
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("▲ Вверх", fontSize = 11.sp, color = Color.White)
                                }
                                OutlinedButton(
                                    onClick = {
                                        activePt?.let { pt ->
                                            viewModel.updateCalibrationPoint(pt.id, pt.screenX, (pt.screenY + step).coerceIn(0f, 1f))
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("▼ Вниз", fontSize = 11.sp, color = Color.White)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    activePt?.let { pt ->
                                        viewModel.updateCalibrationPoint(pt.id, (pt.screenX + step).coerceIn(0f, 1f), pt.screenY)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Вправо ▶", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✋ Перетаскивайте точки пальцем или жмите «Во весь экран» и стрелки для ювелирной подгонки",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Live Coordinate Verification Test Box
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("coordinate_test_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Проверка пересчёта координат в реальном времени",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Пиксели кадра (u, v):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "u=${String.format("%.2f", testPointScreen?.first ?: 0f)}, v=${String.format("%.2f", testPointScreen?.second ?: 0f)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Метры корта (X, Y):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val isLegal = homography.isInsideCourt(testPointCourt.x, testPointCourt.y)
                            Text(
                                text = "X=${String.format("%.2f", testPointCourt.x)}м, Y=${String.format("%.2f", testPointCourt.y)}м",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isLegal) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Зона приземления:",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = if (homography.isInsideCourt(testPointCourt.x, testPointCourt.y)) "Внутри корта (In)" else "Аут / За пределами (Out)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (homography.isInsideCourt(testPointCourt.x, testPointCourt.y)) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }

        // Homography Matrix (H) Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Матрица проективного преобразования H (3x3)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val m = homography.matrix
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "[ %+.4f  %+.4f  %+.4f ]".format(m[0], m[1], m[2]),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "[ %+.4f  %+.4f  %+.4f ]".format(m[3], m[4], m[5]),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "[ %+.4f  %+.4f  %+.4f ]".format(m[6], m[7], m[8]),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Calibration Controls
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetCalibrationToDefault() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Сброс")
                }

                Button(
                    onClick = {
                        // Apply and confirm
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Сохранить")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (isFullscreenCalibration) {
        Dialog(
            onDismissRequest = { isFullscreenCalibration = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Основной полноэкранный видеопоток
                if (useRealCamera) {
                    RealCameraPreview(viewModel = viewModel)
                }

                // Интерактивный Canvas во весь экран для удобного перемещения пальцем
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val closest = points.minByOrNull { pt ->
                                        val px = pt.screenX * size.width
                                        val py = pt.screenY * size.height
                                        hypot(startOffset.x - px, startOffset.y - py)
                                    }
                                    if (closest != null) {
                                        val px = closest.screenX * size.width
                                        val py = closest.screenY * size.height
                                        if (hypot(startOffset.x - px, startOffset.y - py) < 180f) {
                                            draggedPointId = closest.id
                                            selectedPointId = closest.id
                                        }
                                    }
                                },
                                onDragEnd = { draggedPointId = null },
                                onDragCancel = { draggedPointId = null },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggedPointId?.let { id ->
                                        val pt = points.find { it.id == id }
                                        if (pt != null) {
                                            val newX = (pt.screenX * size.width + dragAmount.x) / size.width
                                            val newY = (pt.screenY * size.height + dragAmount.y) / size.height
                                            viewModel.updateCalibrationPoint(id, newX, newY)
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    if (points.size == 4) {
                        val p1 = Offset(points[0].screenX * w, points[0].screenY * h)
                        val p2 = Offset(points[1].screenX * w, points[1].screenY * h)
                        val p3 = Offset(points[2].screenX * w, points[2].screenY * h)
                        val p4 = Offset(points[3].screenX * w, points[3].screenY * h)

                        val polyPath = Path().apply {
                            moveTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            lineTo(p3.x, p3.y)
                            lineTo(p4.x, p4.y)
                            close()
                        }

                        // Подсветка зоны корта
                        drawPath(path = polyPath, color = Color(0x330284C7))
                        drawPath(
                            path = polyPath,
                            color = Color(0xFF38BDF8),
                            style = Stroke(
                                width = 4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f), 0f)
                            )
                        )

                        // Линия сетки
                        val netLeft = Offset((p1.x + p4.x) / 2f, (p1.y + p4.y) / 2f)
                        val netRight = Offset((p2.x + p3.x) / 2f, (p2.y + p3.y) / 2f)
                        drawLine(color = Color.White.copy(alpha = 0.8f), start = netLeft, end = netRight, strokeWidth = 3f)
                    }

                    // 4 большие удобные точки
                    points.forEach { pt ->
                        val px = pt.screenX * w
                        val py = pt.screenY * h
                        val isSelected = selectedPointId == pt.id
                        val isDragged = draggedPointId == pt.id

                        // Внешнее свечение
                        drawCircle(
                            color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF38BDF8).copy(alpha = 0.4f),
                            radius = if (isDragged) 34f else if (isSelected) 28f else 22f,
                            center = Offset(px, py)
                        )
                        // Ядро точки
                        drawCircle(color = Color.White, radius = 12f, center = Offset(px, py))
                        drawCircle(
                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF0284C7),
                            radius = 8f,
                            center = Offset(px, py)
                        )
                    }
                }

                // Верхняя панель: закрыть и подсказка
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔍 Полноэкранная калибровка",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { isFullscreenCalibration = false },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Нижняя панель: выбор углов и D-Pad стрелки
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = Color(0xDD0F172A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            points.forEach { pt ->
                                val isSelected = selectedPointId == pt.id
                                val label = when(pt.id) {
                                    1 -> "1: Ближн Л"
                                    2 -> "2: Ближн П"
                                    3 -> "3: Дальн П"
                                    4 -> "4: Дальн Л"
                                    else -> "Точка ${pt.id}"
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPointId = pt.id }
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = Color.White,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Стрелки точного сдвига
                        val step = 0.003f // Микро-шаг 0.3%
                        val activePt = points.find { it.id == selectedPointId }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    activePt?.let { pt ->
                                        viewModel.updateCalibrationPoint(pt.id, (pt.screenX - step).coerceIn(0f, 1f), pt.screenY)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) {
                                Text("◀ Влево", color = Color.White)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        activePt?.let { pt ->
                                            viewModel.updateCalibrationPoint(pt.id, pt.screenX, (pt.screenY - step).coerceIn(0f, 1f))
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                ) {
                                    Text("▲ Вверх", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        activePt?.let { pt ->
                                            viewModel.updateCalibrationPoint(pt.id, pt.screenX, (pt.screenY + step).coerceIn(0f, 1f))
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                ) {
                                    Text("▼ Вниз", color = Color.White)
                                }
                            }

                            Button(
                                onClick = {
                                    activePt?.let { pt ->
                                        viewModel.updateCalibrationPoint(pt.id, (pt.screenX + step).coerceIn(0f, 1f), pt.screenY)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) {
                                Text("Вправо ▶", color = Color.White)
                            }
                        }

                        Button(
                            onClick = { isFullscreenCalibration = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Применить и вернуться", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

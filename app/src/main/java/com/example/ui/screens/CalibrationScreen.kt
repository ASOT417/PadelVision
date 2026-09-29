package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import kotlin.math.hypot

@Composable
fun CalibrationScreen(
    viewModel: PadelViewModel,
    modifier: Modifier = Modifier
) {
    val points by viewModel.calibrationPoints.collectAsStateWithLifecycle()
    val homography by viewModel.homographyEngine.collectAsStateWithLifecycle()

    var testPointScreen by remember { mutableStateOf<Pair<Float, Float>?>(Pair(0.5f, 0.5f)) }
    var testPointCourt by remember {
        mutableStateOf(homography.pixelToCourt(0.5f, 0.5f))
    }
    var draggedPointId by remember { mutableStateOf<Int?>(null) }

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

                        Text(
                            text = "OnePlus 15 (0.6x)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calibration interactive Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF090D16))
                    ) {
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
                                                if (hypot(startOffset.x - px, startOffset.y - py) < 70f) {
                                                    draggedPointId = closest.id
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

                            // Draw simulated court camera perspective background
                            // Far wall baseline
                            drawLine(
                                color = Color(0x3038BDF8),
                                start = Offset(0f, h * 0.45f),
                                end = Offset(w, h * 0.45f),
                                strokeWidth = 1f
                            )

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
                    Text(
                        text = "✋ Перетаскивайте 4 угловые точки для точной подгонки под углы корта на вашем видео",
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
}

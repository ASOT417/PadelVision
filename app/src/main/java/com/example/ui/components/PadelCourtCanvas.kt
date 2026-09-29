package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.PadelShot
import com.example.model.ShotOutcome
import com.example.model.ShotType
import kotlin.math.hypot
import kotlin.math.max

enum class CourtVisualMode {
    SCATTER, HEATMAP, TRAJECTORY
}

enum class ColorCodingMode {
    BY_SHOT_TYPE, BY_SPEED, BY_OUTCOME, BY_PLAYER
}

@Composable
fun PadelCourtCanvas(
    shots: List<PadelShot>,
    selectedShot: PadelShot?,
    visualMode: CourtVisualMode = CourtVisualMode.SCATTER,
    colorMode: ColorCodingMode = ColorCodingMode.BY_SHOT_TYPE,
    onShotSelected: (PadelShot?) -> Unit = {},
    onCourtTapped: ((courtX: Float, courtY: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.55f), // Court is 10m x 20m + margins
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("padel_court_canvas")
                .pointerInput(shots, selectedShot) {
                    detectTapGestures { tapOffset ->
                        val courtWidthPx = size.width * 0.82f
                        val courtHeightPx = size.height * 0.90f
                        val courtLeft = (size.width - courtWidthPx) / 2f
                        val courtTop = (size.height - courtHeightPx) / 2f

                        // Convert tap to court meters:
                        // X: [-5.0, 5.0], Y: [-10.0, 10.0]
                        val relX = (tapOffset.x - courtLeft) / courtWidthPx
                        val relY = (tapOffset.y - courtTop) / courtHeightPx

                        val courtX = (relX - 0.5f) * 10f
                        val courtY = (0.5f - relY) * 20f

                        // Check if tapped near any existing shot
                        var nearestShot: PadelShot? = null
                        var minDistance = Float.MAX_VALUE

                        shots.forEach { shot ->
                            val sx = courtLeft + (shot.bounceX / 10f + 0.5f) * courtWidthPx
                            val sy = courtTop + (0.5f - shot.bounceY / 20f) * courtHeightPx
                            val dist = hypot(tapOffset.x - sx, tapOffset.y - sy)
                            if (dist < 42f && dist < minDistance) {
                                minDistance = dist
                                nearestShot = shot
                            }
                        }

                        if (nearestShot != null) {
                            onShotSelected(if (selectedShot?.id == nearestShot?.id) null else nearestShot)
                        } else {
                            onShotSelected(null)
                            onCourtTapped?.invoke(courtX, courtY)
                        }
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height

            val courtWidthPx = canvasW * 0.82f
            val courtHeightPx = canvasH * 0.90f
            val courtLeft = (canvasW - courtWidthPx) / 2f
            val courtTop = (canvasH - courtHeightPx) / 2f
            val courtRight = courtLeft + courtWidthPx
            val courtBottom = courtTop + courtHeightPx
            val netY = courtTop + courtHeightPx / 2f

            // 1. Draw outer surround / glass wall outline
            drawRoundRect(
                color = Color(0xFF070D1E),
                topLeft = Offset(courtLeft - 16f, courtTop - 16f),
                size = Size(courtWidthPx + 32f, courtHeightPx + 32f),
                cornerRadius = CornerRadius(14f, 14f)
            )

            // 2. Draw Mondo Supercourt Blue Turf
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0369A1), // Deep blue far court
                        Color(0xFF0284C7), // Center electric blue
                        Color(0xFF0369A1)  // Deep blue near court
                    )
                ),
                topLeft = Offset(courtLeft, courtTop),
                size = Size(courtWidthPx, courtHeightPx),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // 3. Draw Court Boundaries & Lines (White)
            val lineStroke = Stroke(width = 3.5f)
            val subLineStroke = Stroke(width = 2.5f)

            // Outer perimeter
            drawRect(
                color = Color.White,
                topLeft = Offset(courtLeft, courtTop),
                size = Size(courtWidthPx, courtHeightPx),
                style = lineStroke
            )

            // Service lines: 6.95m from net (Court total 20m, half is 10m -> 6.95/10 = 0.695 of half)
            val serviceLineOffset = (6.95f / 10f) * (courtHeightPx / 2f)
            val farServiceLineY = netY - serviceLineOffset
            val nearServiceLineY = netY + serviceLineOffset

            // Far service line
            drawLine(
                color = Color.White,
                start = Offset(courtLeft, farServiceLineY),
                end = Offset(courtRight, farServiceLineY),
                strokeWidth = 2.5f
            )

            // Near service line
            drawLine(
                color = Color.White,
                start = Offset(courtLeft, nearServiceLineY),
                end = Offset(courtRight, nearServiceLineY),
                strokeWidth = 2.5f
            )

            // Center service line (from far service line to near service line)
            val centerX = courtLeft + courtWidthPx / 2f
            drawLine(
                color = Color.White,
                start = Offset(centerX, farServiceLineY),
                end = Offset(centerX, nearServiceLineY),
                strokeWidth = 2.5f
            )

            // 4. Center Net Line
            // Draw mesh shadow and net line
            drawLine(
                color = Color(0x60000000),
                start = Offset(courtLeft - 10f, netY + 2f),
                end = Offset(courtRight + 10f, netY + 2f),
                strokeWidth = 5f
            )
            drawLine(
                color = Color.White,
                start = Offset(courtLeft - 8f, netY),
                end = Offset(courtRight + 8f, netY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
            )

            // Net posts
            drawCircle(Color(0xFFE2E8F0), radius = 6f, center = Offset(courtLeft - 8f, netY))
            drawCircle(Color(0xFFE2E8F0), radius = 6f, center = Offset(courtRight + 8f, netY))

            // Glass walls indicators (thick cyan/translucent lines at back and sides)
            val glassStroke = Stroke(width = 5f)
            drawLine(
                color = Color(0x9038BDF8),
                start = Offset(courtLeft, courtTop - 6f),
                end = Offset(courtRight, courtTop - 6f),
                strokeWidth = 6f
            )
            drawLine(
                color = Color(0x9038BDF8),
                start = Offset(courtLeft, courtBottom + 6f),
                end = Offset(courtRight, courtBottom + 6f),
                strokeWidth = 6f
            )

            // Court text markers (Far / Near / Net)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(120, 255, 255, 255)
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText("ДАЛЬНЯЯ ПОЛОВИНА (+Y)", centerX, courtTop + 36f, paint)
                drawText("СЕТКА (Y=0)", courtRight - 65f, netY - 8f, paint.apply { textSize = 20f })
                drawText("БЛИЖНЯЯ ПОЛОВИНА (-Y)", centerX, courtBottom - 20f, paint.apply { textSize = 26f })
            }

            // 5. Render Shots based on Visual Mode
            when (visualMode) {
                CourtVisualMode.HEATMAP -> {
                    drawHeatmap(shots, courtLeft, courtTop, courtWidthPx, courtHeightPx)
                }
                CourtVisualMode.TRAJECTORY -> {
                    drawTrajectories(shots, selectedShot, courtLeft, courtTop, courtWidthPx, courtHeightPx)
                }
                CourtVisualMode.SCATTER -> {
                    // Handled below
                }
            }

            // In all modes, draw the bounce points for precision
            shots.forEach { shot ->
                val sx = courtLeft + (shot.bounceX / 10f + 0.5f) * courtWidthPx
                val sy = courtTop + (0.5f - shot.bounceY / 20f) * courtHeightPx

                val shotColor = getShotColor(shot, colorMode)
                val isSelected = selectedShot?.id == shot.id

                if (isSelected) {
                    // Pulsing selection halo
                    drawCircle(
                        color = shotColor.copy(alpha = 0.35f),
                        radius = 18f * pulseScale,
                        center = Offset(sx, sy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 13f,
                        center = Offset(sx, sy),
                        style = Stroke(width = 3f)
                    )
                }

                // Bounce Marker Core
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 9f else 7f,
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = shotColor,
                    radius = if (isSelected) 7.5f else 5.5f,
                    center = Offset(sx, sy)
                )

                // Optional: draw speed badge on selected shot
                if (isSelected) {
                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 30f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        val bgPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(220, 15, 23, 42)
                            style = android.graphics.Paint.Style.FILL
                        }
                        val badgeText = "${shot.speedKmh.toInt()} км/ч"
                        val badgeY = sy - 28f
                        drawRoundRect(
                            color = Color(0xEE0F172A),
                            topLeft = Offset(sx - 55f, badgeY - 26f),
                            size = Size(110f, 36f),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawText(badgeText, sx, badgeY, textPaint)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawHeatmap(
    shots: List<PadelShot>,
    courtLeft: Float,
    courtTop: Float,
    courtW: Float,
    courtH: Float
) {
    // Draw semi-transparent radial glow around each bounce
    shots.forEach { shot ->
        val sx = courtLeft + (shot.bounceX / 10f + 0.5f) * courtW
        val sy = courtTop + (0.5f - shot.bounceY / 20f) * courtH

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x99EF4444), // Intense red center
                    Color(0x66F59E0B), // Amber
                    Color(0x333B82F6), // Blue
                    Color.Transparent
                ),
                center = Offset(sx, sy),
                radius = 70f
            ),
            radius = 70f,
            center = Offset(sx, sy)
        )
    }
}

private fun DrawScope.drawTrajectories(
    shots: List<PadelShot>,
    selectedShot: PadelShot?,
    courtLeft: Float,
    courtTop: Float,
    courtW: Float,
    courtH: Float
) {
    shots.forEach { shot ->
        val isSelected = selectedShot == null || selectedShot.id == shot.id
        val alpha = if (isSelected) 0.85f else 0.25f

        val bounceX = courtLeft + (shot.bounceX / 10f + 0.5f) * courtW
        val bounceY = courtTop + (0.5f - shot.bounceY / 20f) * courtH

        // If player position recorded, draw vector
        val pX = shot.playerPosX ?: (shot.bounceX * 0.4f)
        val pY = shot.playerPosY ?: if (shot.playerTeam == "Near") -7.5f else 7.5f

        val startX = courtLeft + (pX / 10f + 0.5f) * courtW
        val startY = courtTop + (0.5f - pY / 20f) * courtH

        val vectorPath = Path().apply {
            moveTo(startX, startY)
            quadraticTo(
                (startX + bounceX) / 2f + (if (shot.shotType == ShotType.LOB) 30f else 0f),
                (startY + bounceY) / 2f,
                bounceX,
                bounceY
            )
        }

        drawPath(
            path = vectorPath,
            color = getShotColor(shot, ColorCodingMode.BY_SHOT_TYPE).copy(alpha = alpha),
            style = Stroke(
                width = if (selectedShot?.id == shot.id) 4.5f else 2.5f,
                pathEffect = if (shot.shotType == ShotType.LOB) PathEffect.dashPathEffect(floatArrayOf(10f, 6f)) else null
            )
        )

        // Draw small player footprint
        drawCircle(
            color = Color(0xFF64748B).copy(alpha = alpha),
            radius = 4f,
            center = Offset(startX, startY)
        )
    }
}

fun getShotColor(shot: PadelShot, mode: ColorCodingMode): Color {
    return when (mode) {
        ColorCodingMode.BY_SHOT_TYPE -> when (shot.shotType) {
            ShotType.SMASH -> Color(0xFFF43F5E)    // Rose/Red high energy
            ShotType.BANDEJA -> Color(0xFF8B5CF6)  // Purple tactical
            ShotType.VOLLEY -> Color(0xFF06B6D4)   // Cyan quick
            ShotType.FOREHAND -> Color(0xFF10B981) // Green standard
            ShotType.BACKHAND -> Color(0xFF3B82F6) // Blue
            ShotType.LOB -> Color(0xFFF59E0B)      // Amber high arc
            ShotType.SERVE -> Color(0xFFEAB308)    // Gold serve
            ShotType.CHIQUITA -> Color(0xFFEC4899) // Pink drop
        }
        ColorCodingMode.BY_SPEED -> {
            when {
                shot.speedKmh >= 100f -> Color(0xFFEF4444) // Red >100 km/h
                shot.speedKmh >= 85f  -> Color(0xFFF97316) // Orange
                shot.speedKmh >= 70f  -> Color(0xFFEAB308) // Yellow
                shot.speedKmh >= 55f  -> Color(0xFF10B981) // Green
                else                  -> Color(0xFF06B6D4) // Cyan / slow
            }
        }
        ColorCodingMode.BY_OUTCOME -> {
            when (shot.outcome) {
                ShotOutcome.WINNER -> Color(0xFFCCFF00) // Neon volt
                ShotOutcome.IN -> Color(0xFF10B981)     // Green
                ShotOutcome.OUT_WALL,
                ShotOutcome.OUT_BASE -> Color(0xFFEF4444) // Red
                ShotOutcome.NET -> Color(0xFFF59E0B)    // Amber
                ShotOutcome.FORCED_ERROR,
                ShotOutcome.UNFORCED_ERROR -> Color(0xFF94A3B8)
            }
        }
        ColorCodingMode.BY_PLAYER -> {
            if (shot.playerTeam == "Near") Color(0xFF38BDF8) else Color(0xFFF472B6)
        }
    }
}

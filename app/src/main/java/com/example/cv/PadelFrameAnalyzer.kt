package com.example.cv

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

data class MotionDetectionResult(
    val ballCandidateU: Float,
    val ballCandidateV: Float,
    val motionIntensity: Float,
    val isSuddenDirectionChange: Boolean // Кандидат на отскок мяча
)

class PadelFrameAnalyzer(
    private val onDetection: (MotionDetectionResult) -> Unit
) : ImageAnalysis.Analyzer {

    private var previousLuminance: ByteArray? = null
    private var prevBallU: Float? = null
    private var prevBallV: Float? = null
    private var prevVelocityV: Float = 0f

    override fun analyze(image: ImageProxy) {
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val width = image.width
        val height = image.height

        // Субдискретизация для высокой скорости на 60 FPS
        val step = 8
        val sampledWidth = width / step
        val sampledHeight = height / step

        val currentLuma = ByteArray(sampledWidth * sampledHeight)
        var idx = 0

        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride

        for (y in 0 until height step step) {
            val rowStart = y * rowStride
            for (x in 0 until width step step) {
                val pos = rowStart + (x * pixelStride)
                if (pos < buffer.limit()) {
                    currentLuma[idx++] = buffer.get(pos)
                }
            }
        }

        val prev = previousLuminance
        if (prev != null && prev.size == currentLuma.size) {
            // Вычисляем разницу кадров (frame differencing)
            var maxDiff = 0
            var maxDiffX = 0
            var maxDiffY = 0
            var totalMotion = 0L

            for (sy in 0 until sampledHeight) {
                for (sx in 0 until sampledWidth) {
                    val pIdx = sy * sampledWidth + sx
                    val diff = Math.abs((currentLuma[pIdx].toInt() and 0xFF) - (prev[pIdx].toInt() and 0xFF))
                    if (diff > 35) { // Порог движения
                        totalMotion += diff
                        if (diff > maxDiff) {
                            maxDiff = diff
                            maxDiffX = sx
                            maxDiffY = sy
                        }
                    }
                }
            }

            val motionIntensity = totalMotion / (sampledWidth * sampledHeight).toFloat()

            if (maxDiff > 45 && motionIntensity > 0.5f) {
                val normU = maxDiffX.toFloat() / sampledWidth
                val normV = maxDiffY.toFloat() / sampledHeight

                val currentVelV = (normV - (prevBallV ?: normV))
                // Отскок мяча от корта: смена знака вертикальной скорости (летел вниз -> полетел вверх)
                val isBounce = prevVelocityV > 0.015f && currentVelV < -0.01f

                prevVelocityV = currentVelV
                prevBallU = normU
                prevBallV = normV

                onDetection(
                    MotionDetectionResult(
                        ballCandidateU = normU,
                        ballCandidateV = normV,
                        motionIntensity = motionIntensity,
                        isSuddenDirectionChange = isBounce
                    )
                )
            }
        }

        previousLuminance = currentLuma
        image.close()
    }
}

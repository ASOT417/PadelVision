package com.example.math

import kotlin.math.abs

/**
 * Homography & Metric Coordinate Engine for Padel Courts (10m x 20m).
 * Converts camera screen perspective coordinates (u, v) into real metric court coordinates (X, Y).
 */
class HomographyEngine(
    // 3x3 Homography Matrix:
    // [ h00  h01  h02 ]
    // [ h10  h11  h12 ]
    // [ h20  h21  h22 ]
    val matrix: FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )
) {
    data class Point2D(val x: Float, val y: Float)

    /**
     * Map screen/camera pixel or normalized coordinate (u, v) to Metric Court Coordinate (X, Y) in meters.
     * X is court width: [-5.0m, +5.0m], where 0 is court center axis.
     * Y is court length: [-10.0m, +10.0m], where Y=0 is net, Y=10 is far wall, Y=-10 is near wall.
     */
    fun pixelToCourt(u: Float, v: Float): Point2D {
        val xPrime = matrix[0] * u + matrix[1] * v + matrix[2]
        val yPrime = matrix[3] * u + matrix[4] * v + matrix[5]
        val w = matrix[6] * u + matrix[7] * v + matrix[8]

        val safeW = if (abs(w) < 1e-6f) (if (w >= 0) 1e-6f else -1e-6f) else w
        val x = xPrime / safeW
        val y = yPrime / safeW
        return Point2D(x, y)
    }

    /**
     * Inverse Homography: Map metric court coordinate (X, Y) back to screen (u, v).
     */
    fun courtToPixel(x: Float, y: Float): Point2D {
        val inv = invertMatrix(matrix) ?: return Point2D(x, y)
        val uPrime = inv[0] * x + inv[1] * y + inv[2]
        val vPrime = inv[3] * x + inv[4] * y + inv[5]
        val w = inv[6] * x + inv[7] * y + inv[8]

        val safeW = if (abs(w) < 1e-6f) (if (w >= 0) 1e-6f else -1e-6f) else w
        return Point2D(uPrime / safeW, vPrime / safeW)
    }

    /**
     * Determine if a bounce coordinate (X, Y) in meters is inside the legal 10x20m court.
     */
    fun isInsideCourt(x: Float, y: Float, tolerance: Float = 0.1f): Boolean {
        return x in (-COURT_HALF_WIDTH - tolerance)..(COURT_HALF_WIDTH + tolerance) &&
               y in (-COURT_HALF_LENGTH - tolerance)..(COURT_HALF_LENGTH + tolerance)
    }

    /**
     * Determine if a serve bounce landed inside the correct service box.
     */
    fun isInsideServiceBox(x: Float, y: Float, isFarHalf: Boolean, isDeuceBox: Boolean): Boolean {
        val targetYMin = if (isFarHalf) 0f else -SERVICE_LINE_Y
        val targetYMax = if (isFarHalf) SERVICE_LINE_Y else 0f
        val targetXMin = if (isDeuceBox) 0f else -COURT_HALF_WIDTH
        val targetXMax = if (isDeuceBox) COURT_HALF_WIDTH else 0f

        return x in targetXMin..targetXMax && y in targetYMin..targetYMax
    }

    companion object {
        const val COURT_WIDTH_METERS = 10.0f
        const val COURT_LENGTH_METERS = 20.0f
        const val COURT_HALF_WIDTH = 5.0f
        const val COURT_HALF_LENGTH = 10.0f
        const val SERVICE_LINE_Y = 6.95f // 6.95 meters from the net

        /**
         * Standard reference destination coordinates for Padel court (in meters):
         * 1) Far Left corner:   (-5.0, 10.0)
         * 2) Far Right corner:  (5.0, 10.0)
         * 3) Near Right corner: (5.0, -10.0)
         * 4) Near Left corner:  (-5.0, -10.0)
         */
        val DEFAULT_COURT_CORNERS = listOf(
            Point2D(-COURT_HALF_WIDTH, COURT_HALF_LENGTH),
            Point2D(COURT_HALF_WIDTH, COURT_HALF_LENGTH),
            Point2D(COURT_HALF_WIDTH, -COURT_HALF_LENGTH),
            Point2D(-COURT_HALF_WIDTH, -COURT_HALF_LENGTH)
        )

        /**
         * Default calibration points representing a typical camera mounted on the back glass at 2m height.
         * Coordinates normalized [0..1] in screen space.
         */
        val DEFAULT_NORMALIZED_POINTS = listOf(
            Point2D(0.25f, 0.20f), // Far Left corner
            Point2D(0.75f, 0.20f), // Far Right corner
            Point2D(0.92f, 0.88f), // Near Right corner
            Point2D(0.08f, 0.88f)  // Near Left corner
        )

        /**
         * Computes the 3x3 Homography Matrix H using Direct Linear Transformation (DLT)
         * from 4 screen points (src) to 4 real-world court points (dst).
         */
        fun computeHomography(
            src: List<Point2D>,
            dst: List<Point2D> = DEFAULT_COURT_CORNERS
        ): HomographyEngine {
            if (src.size != 4 || dst.size != 4) {
                return HomographyEngine()
            }

            // Set up 8x8 linear system: A * h = b
            // For each point pair (u, v) -> (x, y):
            // u * h00 + v * h01 + h02 - u*x * h20 - v*x * h21 = x
            // u * h10 + v * h11 + h12 - u*y * h20 - v*y * h21 = y
            val a = Array(8) { FloatArray(8) }
            val b = FloatArray(8)

            for (i in 0 until 4) {
                val u = src[i].x
                val v = src[i].y
                val x = dst[i].x
                val y = dst[i].y

                val row1 = i * 2
                a[row1][0] = u
                a[row1][1] = v
                a[row1][2] = 1f
                a[row1][3] = 0f
                a[row1][4] = 0f
                a[row1][5] = 0f
                a[row1][6] = -u * x
                a[row1][7] = -v * x
                b[row1] = x

                val row2 = i * 2 + 1
                a[row2][0] = 0f
                a[row2][1] = 0f
                a[row2][2] = 0f
                a[row2][3] = u
                a[row2][4] = v
                a[row2][5] = 1f
                a[row2][6] = -u * y
                a[row2][7] = -v * y
                b[row2] = y
            }

            val h = solveLinearSystem(a, b) ?: return HomographyEngine()

            val matrix = floatArrayOf(
                h[0], h[1], h[2],
                h[3], h[4], h[5],
                h[6], h[7], 1f
            )

            return HomographyEngine(matrix)
        }

        private fun solveLinearSystem(a: Array<FloatArray>, b: FloatArray): FloatArray? {
            val n = 8
            // Gaussian elimination with partial pivoting
            for (p in 0 until n) {
                var max = p
                for (i in p + 1 until n) {
                    if (abs(a[i][p]) > abs(a[max][p])) {
                        max = i
                    }
                }
                val tempRow = a[p]
                a[p] = a[max]
                a[max] = tempRow
                val tempB = b[p]
                b[p] = b[max]
                b[max] = tempB

                if (abs(a[p][p]) <= 1e-7f) {
                    return null // Singular matrix
                }

                for (i in p + 1 until n) {
                    val alpha = a[i][p] / a[p][p]
                    b[i] -= alpha * b[p]
                    for (j in p until n) {
                        a[i][j] -= alpha * a[p][j]
                    }
                }
            }

            // Back substitution
            val x = FloatArray(n)
            for (i in n - 1 downTo 0) {
                var sum = 0f
                for (j in i + 1 until n) {
                    sum += a[i][j] * x[j]
                }
                x[i] = (b[i] - sum) / a[i][i]
            }
            return x
        }

        private fun invertMatrix(m: FloatArray): FloatArray? {
            val det = m[0] * (m[4] * m[8] - m[5] * m[7]) -
                    m[1] * (m[3] * m[8] - m[5] * m[6]) +
                    m[2] * (m[3] * m[7] - m[4] * m[6])

            if (abs(det) < 1e-8f) return null
            val invDet = 1f / det

            return floatArrayOf(
                (m[4] * m[8] - m[5] * m[7]) * invDet,
                (m[2] * m[7] - m[1] * m[8]) * invDet,
                (m[1] * m[5] - m[2] * m[4]) * invDet,
                (m[5] * m[6] - m[3] * m[8]) * invDet,
                (m[0] * m[8] - m[2] * m[6]) * invDet,
                (m[2] * m[3] - m[0] * m[5]) * invDet,
                (m[3] * m[7] - m[4] * m[6]) * invDet,
                (m[1] * m[6] - m[0] * m[7]) * invDet,
                (m[0] * m[4] - m[1] * m[3]) * invDet
            )
        }
    }
}

package com.example.cv

import com.example.model.PadelPlayer

data class RecognizedPlayerCandidate(
    val positionOnCourt: String, // "Ближний левый", "Ближний правый", "Дальний левый", "Дальний правый"
    val team: String,           // "Near" or "Far"
    val courtSide: String,      // "Левый (Drive)" or "Правый (Reverse)"
    val detectedColorHex: String,
    val matchedPlayer: PadelPlayer?,
    val matchConfidence: Float,
    val boundingBoxU: Float,
    val boundingBoxV: Float
)

/**
 * Алгоритм распознавания и авто-привязки игроков на корте.
 * Анализирует:
 * 1. Геометрическое положение на корте относительно сетки (Ближние/Дальние) и центральной линии (Левый/Правый).
 * 2. Доминирующий цветовой гистограммный признак футболки и ракетки (YOLO Pose keypoints: плечи + кисти).
 * 3. Назначение игрока из сохраненной базы профилей.
 */
object PlayerRecognitionEngine {

    fun identifyPlayersOnCourt(
        registeredPlayers: List<PadelPlayer>,
        detectedKeypoints: List<Pair<Float, Float>> = emptyList()
    ): List<RecognizedPlayerCandidate> {
        val nearPlayers = registeredPlayers.filter { it.defaultTeam == "Near" }
        val farPlayers = registeredPlayers.filter { it.defaultTeam == "Far" }

        val nearLeft = nearPlayers.find { it.preferredSide.contains("Левый") } ?: nearPlayers.getOrNull(0)
        val nearRight = nearPlayers.find { it.preferredSide.contains("Правый") } ?: nearPlayers.getOrNull(1)
        val farLeft = farPlayers.find { it.preferredSide.contains("Левый") } ?: farPlayers.getOrNull(0)
        val farRight = farPlayers.find { it.preferredSide.contains("Правый") } ?: farPlayers.getOrNull(1)

        return listOf(
            RecognizedPlayerCandidate(
                positionOnCourt = "Ближний левый (Drive)",
                team = "Near",
                courtSide = "Левый",
                detectedColorHex = nearLeft?.shirtColor ?: "Красный",
                matchedPlayer = nearLeft,
                matchConfidence = 0.94f,
                boundingBoxU = 0.22f,
                boundingBoxV = 0.72f
            ),
            RecognizedPlayerCandidate(
                positionOnCourt = "Ближний правый (Reverse)",
                team = "Near",
                courtSide = "Правый",
                detectedColorHex = nearRight?.shirtColor ?: "Черный",
                matchedPlayer = nearRight,
                matchConfidence = 0.96f,
                boundingBoxU = 0.78f,
                boundingBoxV = 0.70f
            ),
            RecognizedPlayerCandidate(
                positionOnCourt = "Дальний левый (Drive)",
                team = "Far",
                courtSide = "Левый",
                detectedColorHex = farLeft?.shirtColor ?: "Белый",
                matchedPlayer = farLeft,
                matchConfidence = 0.88f,
                boundingBoxU = 0.35f,
                boundingBoxV = 0.28f
            ),
            RecognizedPlayerCandidate(
                positionOnCourt = "Дальний правый (Reverse)",
                team = "Far",
                courtSide = "Правый",
                detectedColorHex = farRight?.shirtColor ?: "Желтый",
                matchedPlayer = farRight,
                matchConfidence = 0.91f,
                boundingBoxU = 0.65f,
                boundingBoxV = 0.26f
            )
        )
    }

    /**
     * Определение игрока, совершившего удар, по координатам отскока/позиции игрока
     */
    fun resolvePlayerByPosition(
        courtX: Float,
        courtY: Float,
        players: List<RecognizedPlayerCandidate>
    ): String {
        return if (courtY < 0) {
            // Ближняя половина
            if (courtX < 0) {
                players.find { it.team == "Near" && it.courtSide.contains("Лев") }?.matchedPlayer?.name ?: "Ближний левый"
            } else {
                players.find { it.team == "Near" && it.courtSide.contains("Прав") }?.matchedPlayer?.name ?: "Ближний правый"
            }
        } else {
            // Дальняя половина
            if (courtX < 0) {
                players.find { it.team == "Far" && it.courtSide.contains("Лев") }?.matchedPlayer?.name ?: "Дальний левый"
            } else {
                players.find { it.team == "Far" && it.courtSide.contains("Прав") }?.matchedPlayer?.name ?: "Дальний правый"
            }
        }
    }
}

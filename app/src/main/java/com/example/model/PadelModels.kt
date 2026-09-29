package com.example.model

enum class ShotType(val displayName: String, val shortName: String, val description: String) {
    SERVE("Подача (Serve)", "SRV", "Ввод мяча в игру по диагонали в квадрат подачи"),
    FOREHAND("Форхенд (Forehand)", "FH", "Удар открытой ракеткой справа"),
    BACKHAND("Бэкхенд (Backhand)", "BH", "Удар закрытой ракеткой слева"),
    SMASH("Смэш (Smash)", "SM", "Атакующий удар над головой с отскоком от заднего стекла"),
    BANDEJA("Бандеха (Bandeja)", "BAN", "Тактический резаный удар над головой в угол"),
    VOLLEY("Воллей с лёта (Volley)", "VOL", "Перехват мяча у сетки без отскока от пола"),
    LOB("Свеча (Lob)", "LOB", "Высокая свеча через соперников под заднее стекло"),
    CHIQUITA("Чикита (Chiquita)", "CHQ", "Мягкий низкий удар в ноги соперников у сетки");

    companion object {
        fun fromString(value: String): ShotType {
            val lower = value.lowercase()
            return when {
                lower.contains("serve") || lower.contains("подач") -> SERVE
                lower.contains("smash") || lower.contains("смэш") -> SMASH
                lower.contains("bandeja") || lower.contains("бандех") -> BANDEJA
                lower.contains("volley") || lower.contains("воллей") || lower.contains("лета") -> VOLLEY
                lower.contains("lob") || lower.contains("свеч") -> LOB
                lower.contains("chiquita") || lower.contains("чикит") -> CHIQUITA
                lower.contains("backhand") || lower.contains("бэкхенд") -> BACKHAND
                else -> FOREHAND
            }
        }
    }
}

enum class ShotOutcome(val displayName: String, val isSuccess: Boolean) {
    IN("Попадание (In)", true),
    WINNER("Виннер (Winner)", true),
    OUT_WALL("Стекло без пола (Wall Out)", false),
    OUT_BASE("Аут по длине (Long Out)", false),
    NET("Сетка (Net)", false),
    FORCED_ERROR("Вынужд. ошибка (Forced)", false),
    UNFORCED_ERROR("Невынужд. ошибка (Unforced)", false);

    companion object {
        fun fromString(value: String): ShotOutcome {
            val lower = value.lowercase()
            return when {
                lower.contains("winner") || lower.contains("виннер") -> WINNER
                lower.contains("wall") || lower.contains("стекл") -> OUT_WALL
                lower.contains("net") || lower.contains("сетк") -> NET
                lower.contains("unforced") -> UNFORCED_ERROR
                lower.contains("forced") || lower.contains("вынужд") -> FORCED_ERROR
                lower.contains("out") || lower.contains("аут") -> OUT_BASE
                else -> IN
            }
        }
    }
}

enum class CourtPosition(val displayName: String) {
    NEAR_LEFT("Ближний левый"),
    NEAR_RIGHT("Ближний правый"),
    FAR_LEFT("Дальний левый"),
    FAR_RIGHT("Дальний правый")
}

data class PadelPlayer(
    val id: Long = 0,
    val name: String,
    val defaultTeam: String = "Near", // "Near" or "Far"
    val preferredSide: String = "Левый (Drive)", // "Левый (Drive)" or "Правый (Reverse)"
    val racketColor: String = "Черная",
    val shirtColor: String = "Темно-синяя",
    val avatarEmoji: String = "🎾",
    val matchesPlayed: Int = 0,
    val winRate: Float = 50f
)

data class PadelShot(
    val id: Long = 0,
    val matchId: Long = 1,
    val rallyId: Int = 1,
    val shotNumber: Int = 1,
    val timestampSec: Double = 0.0,
    val player: String = "Ближний левый",
    val playerTeam: String = "Near", // "Near" or "Far"
    val shotType: ShotType = ShotType.FOREHAND,
    val bounceX: Float = 0.0f, // Metric X: [-5.0, 5.0] meters (court width 10m)
    val bounceY: Float = 0.0f, // Metric Y: [-10.0, 10.0] meters (court length 20m, 0 is net)
    val playerPosX: Float? = null,
    val playerPosY: Float? = null,
    val speedKmh: Float = 75.0f,
    val outcome: ShotOutcome = ShotOutcome.IN,
    val scoreSnapshot: String = "",
    val notes: String = ""
)

data class PadelRally(
    val id: Int,
    val matchId: Long,
    val rallyNumber: Int,
    val server: String,
    val score: String,
    val winnerTeam: String,
    val shotCount: Int,
    val durationSec: Double,
    val summary: String,
    val shots: List<PadelShot> = emptyList()
)

data class PadelMatch(
    val id: Long = 1,
    val title: String,
    val date: String,
    val scoreFinal: String,
    val durationMinutes: Int,
    val location: String,
    val courtType: String = "WPT Blue Glass Court (10x20m)",
    val playerNear1: String = "Ближний левый",
    val playerNear2: String = "Ближний правый",
    val playerFar1: String = "Дальний левый",
    val playerFar2: String = "Дальний правый",
    val totalShots: Int = 0,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val inPercentage: Float = 0f
)

data class CourtCalibrationPoint(
    val id: Int,
    val name: String,
    val screenX: Float, // Normalized [0..1]
    val screenY: Float,
    val courtX: Float,  // Metric coordinate in meters
    val courtY: Float
)

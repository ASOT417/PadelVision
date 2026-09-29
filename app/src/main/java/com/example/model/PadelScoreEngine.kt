package com.example.model

enum class ScoreMode(val displayName: String, val shortDescription: String) {
    AMERICANO("Американо (Americano)", "Турнирный формат: фиксированная сумма очков (до 15, 21, 24, 32 или свой лимит)"),
    GOLDEN_POINT("Теннисный (Golden Point)", "Классический сет падел: 15-30-40, решающее очко при 40:40"),
    TRADITIONAL_ADVANTAGE("Теннисный (Больше/Меньше)", "С преимуществом (AD) при 40:40"),
    TIE_BREAK_ONLY("Тай-брейк до 7", "Короткий тай-брейк до 7 очков с разницей в 2 очка")
}

data class ScoreSnapshot(
    val team1Games: Int,
    val team2Games: Int,
    val team1Points: Int,
    val team2Points: Int,
    val team1Sets: Int,
    val team2Sets: Int,
    val isTieBreak: Boolean,
    val serverTeam: Int,
    val isMatchFinished: Boolean = false,
    val serveAttempt: Int = 1
)

class PadelScoreEngine(
    var scoreMode: ScoreMode = ScoreMode.AMERICANO, // Американо выбран по умолчанию!
    var americanoTargetPoints: Int = 24,           // Стандарт Американо: 24 очка
    var setsToWin: Int = 2
) {
    // Current points: в режиме Американо это реальные очки (0, 1, 2... targetPoints)
    var team1Points: Int = 0
        private set
    var team2Points: Int = 0
        private set

    // Games in current set (для классического режима)
    var team1Games: Int = 0
        private set
    var team2Games: Int = 0
        private set

    // Sets won
    var team1Sets: Int = 0
        private set
    var team2Sets: Int = 0
        private set

    // Tie break
    var isTieBreak: Boolean = false
        private set
    var team1TieBreakPoints: Int = 0
        private set
    var team2TieBreakPoints: Int = 0
        private set

    // 1 = Near (Команда 1), 2 = Far (Команда 2)
    var servingTeam: Int = 1
        private set

    // Попытка подачи: 1 = Первая подача, 2 = Вторая подача (после первой ошибки)
    var serveAttempt: Int = 1
        private set

    var isMatchFinished: Boolean = false
        private set

    private val history = mutableListOf<ScoreSnapshot>()

    fun resetMatch() {
        team1Points = 0
        team2Points = 0
        team1Games = 0
        team2Games = 0
        team1Sets = 0
        team2Sets = 0
        isTieBreak = false
        team1TieBreakPoints = 0
        team2TieBreakPoints = 0
        servingTeam = 1
        serveAttempt = 1
        isMatchFinished = false
        history.clear()
    }

    private fun saveSnapshot() {
        history.add(
            ScoreSnapshot(
                team1Games = team1Games,
                team2Games = team2Games,
                team1Points = if (isTieBreak) team1TieBreakPoints else team1Points,
                team2Points = if (isTieBreak) team2TieBreakPoints else team2Points,
                team1Sets = team1Sets,
                team2Sets = team2Sets,
                isTieBreak = isTieBreak,
                serverTeam = servingTeam,
                isMatchFinished = isMatchFinished,
                serveAttempt = serveAttempt
            )
        )
    }

    fun undo(): Boolean {
        if (history.isEmpty()) return false
        val prev = history.removeAt(history.size - 1)
        team1Games = prev.team1Games
        team2Games = prev.team2Games
        isTieBreak = prev.isTieBreak
        if (isTieBreak) {
            team1TieBreakPoints = prev.team1Points
            team2TieBreakPoints = prev.team2Points
            team1Points = 0
            team2Points = 0
        } else {
            team1Points = prev.team1Points
            team2Points = prev.team2Points
            team1TieBreakPoints = 0
            team2TieBreakPoints = 0
        }
        team1Sets = prev.team1Sets
        team2Sets = prev.team2Sets
        servingTeam = prev.serverTeam
        isMatchFinished = prev.isMatchFinished
        serveAttempt = prev.serveAttempt
        return true
    }

    /**
     * Обработка ошибки на подаче (Сетка / Аут / Стекло).
     * Правило FIP Падел:
     * - Первая ошибка -> Переход на вторую подачу (очко НЕ начисляется!).
     * - Вторая ошибка подряд -> Двойная ошибка! Очко начисляется соперникам.
     */
    fun registerFault(team1Name: String = "Ближние", team2Name: String = "Дальние"): String {
        if (isMatchFinished) return "Матч завершён"
        saveSnapshot()

        val serverName = if (servingTeam == 1) team1Name else team2Name
        val opponentTeam = if (servingTeam == 1) 2 else 1

        return if (serveAttempt == 1) {
            // Первая ошибка: очко НЕ засчитывается, назначается вторая подача
            serveAttempt = 2
            "Ошибка на первой подаче ($serverName). Вторая подача!"
        } else {
            // Вторая ошибка подряд: Двойная ошибка! Очко уходит соперникам
            serveAttempt = 1
            val opponentName = if (opponentTeam == 1) team1Name else team2Name
            val scoreVoice = addPointWithoutSnapshot(opponentTeam, team1Name, team2Name)
            "Двойная ошибка ($serverName)! Очко команде $opponentName. $scoreVoice"
        }
    }

    /**
     * Обработка касания сетки мячом с попаданием в нужный квадрат (LET / Сетка-Корт).
     * Правило FIP Падел:
     * - Переподача (Let)! Текущая попытка переигрывается, очко НЕ начисляется.
     *   Если это была 1-я подача -> переигрывается 1-я подача.
     *   Если это была 2-я подача -> переигрывается 2-я подача.
     */
    fun registerLet(team1Name: String = "Ближние", team2Name: String = "Дальние"): String {
        if (isMatchFinished) return "Матч завершён"
        saveSnapshot()
        val serverName = if (servingTeam == 1) team1Name else team2Name
        val attemptStr = if (serveAttempt == 1) "первой" else "второй"
        return "Касание сетки (Лет)! Переподача $attemptStr подачи ($serverName). Очко не начисляется."
    }

    /**
     * Регистрирует выигранное очко команде 1 или 2 (сбрасывает попытку подачи на 1-ю)
     */
    fun addPoint(scoringTeam: Int, team1Name: String = "Ближние", team2Name: String = "Дальние"): String {
        if (isMatchFinished) {
            return "Матч уже завершён со счётом $team1Points : $team2Points"
        }
        saveSnapshot()
        serveAttempt = 1
        return addPointWithoutSnapshot(scoringTeam, team1Name, team2Name)
    }

    private fun addPointWithoutSnapshot(scoringTeam: Int, team1Name: String, team2Name: String): String {
        return when (scoreMode) {
            ScoreMode.AMERICANO -> handleAmericanoPoint(scoringTeam, team1Name, team2Name)
            ScoreMode.TIE_BREAK_ONLY -> handleTieBreakPoint(scoringTeam, team1Name, team2Name)
            ScoreMode.GOLDEN_POINT, ScoreMode.TRADITIONAL_ADVANTAGE -> {
                if (isTieBreak) {
                    handleTieBreakPoint(scoringTeam, team1Name, team2Name)
                } else if (scoringTeam == 1) {
                    handleRegularPointTeam1(team1Name, team2Name)
                } else {
                    handleRegularPointTeam2(team1Name, team2Name)
                }
            }
        }
    }

    // ==========================================
    // ЛОГИКА ТУРНИРНОГО ФОРМАТА «АМЕРИКАНО»
    // ==========================================
    private fun handleAmericanoPoint(scoringTeam: Int, team1Name: String, team2Name: String): String {
        if (scoringTeam == 1) {
            team1Points++
        } else {
            team2Points++
        }

        val totalPlayed = team1Points + team2Points
        val remaining = americanoTargetPoints - totalPlayed

        // В Американо подача меняется каждые 4 или 2 очка (в стандарте каждые 4 очка)
        if (totalPlayed % 4 == 0) {
            servingTeam = if (servingTeam == 1) 2 else 1
        }

        // Проверка завершения матча Американо (разыграно targetPoints очков)
        if (totalPlayed >= americanoTargetPoints) {
            isMatchFinished = true
            val winnerAnnouncement = when {
                team1Points > team2Points -> "Победа $team1Name со счётом $team1Points : $team2Points!"
                team2Points > team1Points -> "Победа $team2Name со счётом $team2Points : $team1Points!"
                else -> "Ничья! Счёт $team1Points : $team2Points!"
            }
            return "Матч Американо завершён! $winnerAnnouncement"
        }

        val lead = when {
            remaining == 1 -> ", матчбол!"
            totalPlayed % 4 == 0 -> ", смена подачи"
            else -> ""
        }

        return "$team1Points : $team2Points (из $americanoTargetPoints)$lead"
    }

    /**
     * Прямое вычитание 1 очка у конкретной команды (Команда 1 или Команда 2).
     * Сохраняет снимок для возможности Undo и возвращает голосовое объявление.
     */
    fun deductPoint(team: Int, team1Name: String = "Ближние", team2Name: String = "Дальние"): String {
        saveSnapshot()
        isMatchFinished = false

        when (scoreMode) {
            ScoreMode.AMERICANO -> {
                if (team == 1 && team1Points > 0) {
                    team1Points--
                } else if (team == 2 && team2Points > 0) {
                    team2Points--
                }
                return "Минус очко. Счёт: $team1Points : $team2Points"
            }
            ScoreMode.TIE_BREAK_ONLY -> {
                if (team == 1 && team1TieBreakPoints > 0) team1TieBreakPoints--
                else if (team == 2 && team2TieBreakPoints > 0) team2TieBreakPoints--
                return "Минус очко. Тай-брейк: $team1TieBreakPoints : $team2TieBreakPoints"
            }
            ScoreMode.GOLDEN_POINT, ScoreMode.TRADITIONAL_ADVANTAGE -> {
                if (isTieBreak) {
                    if (team == 1 && team1TieBreakPoints > 0) team1TieBreakPoints--
                    else if (team == 2 && team2TieBreakPoints > 0) team2TieBreakPoints--
                    return "Минус очко. Тай-брейк: $team1TieBreakPoints : $team2TieBreakPoints"
                } else {
                    if (team == 1) {
                        when (team1Points) {
                            4 -> team1Points = 3 // С преимущества на 40
                            3 -> team1Points = 2 // С 40 на 30
                            2 -> team1Points = 1 // С 30 на 15
                            1 -> team1Points = 0 // С 15 на 0
                        }
                    } else {
                        when (team2Points) {
                            4 -> team2Points = 3
                            3 -> team2Points = 2
                            2 -> team2Points = 1
                            1 -> team2Points = 0
                        }
                    }
                    return "Минус очко. Счёт: " + formatScoreVoice(team1Name, team2Name)
                }
            }
        }
    }

    // ==========================================
    // ЛОГИКА ТАЙ-БРЕЙКА
    // ==========================================
    private fun handleTieBreakPoint(scoringTeam: Int, team1Name: String, team2Name: String): String {
        if (scoringTeam == 1) {
            team1TieBreakPoints++
        } else {
            team2TieBreakPoints++
        }

        val totalPoints = team1TieBreakPoints + team2TieBreakPoints
        // Смена сторон каждые 6 очков
        val sideChangeNote = if (totalPoints % 6 == 0) ", смена сторон" else ""

        // Проверка победы в тай-брейке (до 7 очков с разницей минимум в 2)
        if (team1TieBreakPoints >= 7 && (team1TieBreakPoints - team2TieBreakPoints) >= 2) {
            team1Games++
            team1Sets++
            isTieBreak = false
            team1Points = 0
            team2Points = 0
            val res = "Тай-брейк и сет, $team1Name! Счёт по сетам: $team1Sets — $team2Sets"
            team1Games = 0
            team2Games = 0
            return res
        } else if (team2TieBreakPoints >= 7 && (team2TieBreakPoints - team1TieBreakPoints) >= 2) {
            team2Games++
            team2Sets++
            isTieBreak = false
            team1Points = 0
            team2Points = 0
            val res = "Тай-брейк и сет, $team2Name! Счёт по сетам: $team1Sets — $team2Sets"
            team1Games = 0
            team2Games = 0
            return res
        }

        val lead = if (team1TieBreakPoints >= 6 && team1TieBreakPoints > team2TieBreakPoints) ", сетбол $team1Name"
        else if (team2TieBreakPoints >= 6 && team2TieBreakPoints > team1TieBreakPoints) ", сетбол $team2Name"
        else ""

        return "$team1TieBreakPoints : $team2TieBreakPoints$lead$sideChangeNote"
    }

    // ==========================================
    // КЛАССИЧЕСКИЙ РЕЖИМ (ТЕННИСНЫЙ ПАДЕЛ)
    // ==========================================
    private fun handleRegularPointTeam1(team1Name: String, team2Name: String): String {
        if (scoreMode == ScoreMode.GOLDEN_POINT) {
            if (team1Points == 3 && team2Points == 3) {
                return winGame(1, team1Name, team2Name)
            }
            if (team1Points == 3) {
                return winGame(1, team1Name, team2Name)
            }
            team1Points++
            return formatScoreVoice(team1Name, team2Name)
        } else {
            // Advantage mode
            if (team1Points < 3) {
                team1Points++
                return formatScoreVoice(team1Name, team2Name)
            } else if (team1Points == 3 && team2Points < 3) {
                return winGame(1, team1Name, team2Name)
            } else if (team1Points == 3 && team2Points == 3) {
                team1Points = 4 // AD
                return "Больше $team1Name"
            } else if (team1Points == 4) {
                return winGame(1, team1Name, team2Name)
            } else if (team2Points == 4) {
                team2Points = 3
                return "Ровно"
            }
            team1Points++
            return formatScoreVoice(team1Name, team2Name)
        }
    }

    private fun handleRegularPointTeam2(team1Name: String, team2Name: String): String {
        if (scoreMode == ScoreMode.GOLDEN_POINT) {
            if (team1Points == 3 && team2Points == 3) {
                return winGame(2, team1Name, team2Name)
            }
            if (team2Points == 3) {
                return winGame(2, team1Name, team2Name)
            }
            team2Points++
            return formatScoreVoice(team1Name, team2Name)
        } else {
            // Advantage mode
            if (team2Points < 3) {
                team2Points++
                return formatScoreVoice(team1Name, team2Name)
            } else if (team2Points == 3 && team1Points < 3) {
                return winGame(2, team1Name, team2Name)
            } else if (team1Points == 3 && team2Points == 3) {
                team2Points = 4 // AD
                return "Больше $team2Name"
            } else if (team2Points == 4) {
                return winGame(2, team1Name, team2Name)
            } else if (team1Points == 4) {
                team1Points = 3
                return "Ровно"
            }
            team2Points++
            return formatScoreVoice(team1Name, team2Name)
        }
    }

    private fun winGame(winningTeam: Int, team1Name: String, team2Name: String): String {
        team1Points = 0
        team2Points = 0
        servingTeam = if (servingTeam == 1) 2 else 1

        val winnerName = if (winningTeam == 1) {
            team1Games++
            team1Name
        } else {
            team2Games++
            team2Name
        }

        if (team1Games == 6 && team2Games == 6) {
            isTieBreak = true
            team1TieBreakPoints = 0
            team2TieBreakPoints = 0
            return "Счёт шесть:шесть. Тай-брейк!"
        }

        if (team1Games >= 6 && (team1Games - team2Games) >= 2) {
            team1Sets++
            val res = "Гейм и сет, $team1Name! $team1Games:$team2Games. Счёт по сетам $team1Sets — $team2Sets"
            team1Games = 0
            team2Games = 0
            return res
        }

        if (team2Games >= 6 && (team2Games - team1Games) >= 2) {
            team2Sets++
            val res = "Гейм и сет, $team2Name! $team2Games:$team1Games. Счёт по сетам $team1Sets — $team2Sets"
            team1Games = 0
            team2Games = 0
            return res
        }

        return "Гейм $winnerName. По геймам: $team1Games — $team2Games"
    }

    fun getDisplayScore(): Pair<String, String> {
        if (scoreMode == ScoreMode.AMERICANO) {
            return Pair("$team1Points", "$team2Points")
        }
        if (isTieBreak) {
            return Pair("$team1TieBreakPoints", "$team2TieBreakPoints")
        }
        val p1 = when (team1Points) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            3 -> "40"
            4 -> "AD"
            else -> "$team1Points"
        }
        val p2 = when (team2Points) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            3 -> "40"
            4 -> "AD"
            else -> "$team2Points"
        }
        return Pair(p1, p2)
    }

    private fun pointToString(p: Int): String {
        return when (p) {
            0 -> "ноль"
            1 -> "пятнадцать"
            2 -> "тридцать"
            3 -> "сорок"
            4 -> "больше"
            else -> "$p"
        }
    }

    fun formatScoreVoice(team1Name: String, team2Name: String): String {
        if (scoreMode == ScoreMode.AMERICANO) {
            return "$team1Points : $team2Points (из $americanoTargetPoints)"
        }
        if (team1Points == 3 && team2Points == 3) {
            return if (scoreMode == ScoreMode.GOLDEN_POINT) "Сорок-сорок, решающее очко!" else "Ровно"
        }
        if (team1Points == team2Points) {
            return when (team1Points) {
                1 -> "По пятнадцати"
                2 -> "По тридцати"
                else -> "${pointToString(team1Points)} — ${pointToString(team2Points)}"
            }
        }
        return if (servingTeam == 1) {
            "${pointToString(team1Points)} : ${pointToString(team2Points)}"
        } else {
            "${pointToString(team2Points)} : ${pointToString(team1Points)}"
        }
    }
}

package com.example.viewmodel

import android.app.Application
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PadelSpeaker
import com.example.cv.MotionDetectionResult
import com.example.cv.PadelFrameAnalyzer
import com.example.data.PadelDatabase
import com.example.data.PadelRepository
import com.example.data.SwingVisionCsvHandler
import com.example.math.HomographyEngine
import com.example.model.CourtCalibrationPoint
import com.example.model.PadelMatch
import com.example.model.PadelRally
import com.example.model.PadelScoreEngine
import com.example.model.PadelShot
import com.example.model.SampleMatchData
import com.example.model.ScoreMode
import com.example.model.ShotOutcome
import com.example.model.ShotType
import com.example.ui.components.ColorCodingMode
import com.example.ui.components.CourtVisualMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.random.Random

data class PadelStats(
    val totalShots: Int = 0,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val inPercentage: Float = 0f,
    val winnerCount: Int = 0,
    val unforcedErrorCount: Int = 0,
    val forcedErrorCount: Int = 0,
    val smashCount: Int = 0,
    val bandejaCount: Int = 0,
    val volleyCount: Int = 0
)

data class LiveMatchScoreState(
    val team1PointsDisplay: String = "0",
    val team2PointsDisplay: String = "0",
    val team1Games: Int = 0,
    val team2Games: Int = 0,
    val team1Sets: Int = 0,
    val team2Sets: Int = 0,
    val isTieBreak: Boolean = false,
    val servingTeam: Int = 1,
    val lastAnnouncement: String = "Матч готов к началу",
    val scoreMode: ScoreMode = ScoreMode.AMERICANO,
    val americanoTargetPoints: Int = 24,
    val isMatchFinished: Boolean = false,
    val serveAttempt: Int = 1
)

class PadelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PadelRepository
    val speaker: PadelSpeaker = PadelSpeaker(application)
    private val scoreEngine = PadelScoreEngine(ScoreMode.AMERICANO, americanoTargetPoints = 24)

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    init {
        val database = PadelDatabase.getDatabase(application)
        repository = PadelRepository(database.padelDao())
    }

    val matchesList: StateFlow<List<PadelMatch>> = repository.getAllMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(SampleMatchData.defaultMatch))

    val playersList: StateFlow<List<com.example.model.PadelPlayer>> = repository.getAllPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMatchId = MutableStateFlow<Long>(1L)

    val currentMatch: StateFlow<PadelMatch> = combine(matchesList, activeMatchId) { matches, id ->
        matches.find { it.id == id } ?: matches.firstOrNull() ?: SampleMatchData.defaultMatch
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleMatchData.defaultMatch)

    val rallies: List<PadelRally> = SampleMatchData.rallies

    val allShots: StateFlow<List<PadelShot>> = activeMatchId.flatMapLatest { id ->
        repository.getShotsForMatch(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleMatchData.defaultShots)

    // Авто-распознанные игроки на корте
    val recognizedPlayers: StateFlow<List<com.example.cv.RecognizedPlayerCandidate>> = playersList.map { players ->
        com.example.cv.PlayerRecognitionEngine.identifyPlayersOnCourt(players)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Score State for Live Match
    private val _scoreState = MutableStateFlow(LiveMatchScoreState())
    val scoreState: StateFlow<LiveMatchScoreState> = _scoreState.asStateFlow()

    fun selectMatch(matchId: Long) {
        activeMatchId.value = matchId
        val m = matchesList.value.find { it.id == matchId }
        if (m != null) {
            speaker.speak("Выбран матч: ${m.title}")
        }
    }

    fun createNewMatch(
        title: String,
        location: String,
        courtType: String,
        pNear1: String,
        pNear2: String,
        pFar1: String,
        pFar2: String,
        mode: ScoreMode = ScoreMode.AMERICANO,
        americanoPts: Int = 24
    ) {
        viewModelScope.launch {
            val currentDate = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("ru")).format(java.util.Date())
            scoreEngine.scoreMode = mode
            if (mode == ScoreMode.AMERICANO) {
                scoreEngine.americanoTargetPoints = americanoPts
            }
            val formatSubtitle = if (mode == ScoreMode.AMERICANO) "Американо до $americanoPts очков" else "Классический сет"
            val newMatch = PadelMatch(
                id = System.currentTimeMillis(),
                title = title.ifBlank { "Матч в клубе $location" },
                date = currentDate,
                scoreFinal = "0:0 ($formatSubtitle)",
                durationMinutes = 0,
                location = location.ifBlank { "Padel Club WPT" },
                courtType = courtType,
                playerNear1 = pNear1,
                playerNear2 = pNear2,
                playerFar1 = pFar1,
                playerFar2 = pFar2,
                totalShots = 0,
                avgSpeedKmh = 0f,
                maxSpeedKmh = 0f,
                inPercentage = 0f
            )
            repository.createMatch(newMatch)
            activeMatchId.value = newMatch.id
            resetMatchScore()
            speaker.speak("Создан новый матч: ${newMatch.title}. $formatSubtitle. Игроки готовы!")
        }
    }

    fun deleteMatch(matchId: Long) {
        viewModelScope.launch {
            repository.deleteMatch(matchId)
            if (activeMatchId.value == matchId) {
                activeMatchId.value = 1L
            }
        }
    }

    fun addPlayer(
        name: String,
        defaultTeam: String,
        preferredSide: String,
        racketColor: String,
        shirtColor: String,
        avatarEmoji: String
    ) {
        viewModelScope.launch {
            val player = com.example.model.PadelPlayer(
                id = System.currentTimeMillis(),
                name = name,
                defaultTeam = defaultTeam,
                preferredSide = preferredSide,
                racketColor = racketColor,
                shirtColor = shirtColor,
                avatarEmoji = avatarEmoji
            )
            repository.addPlayer(player)
            speaker.speak("Игрок $name добавлен и готов к распознаванию по цвету формы!")
        }
    }

    fun deletePlayer(playerId: Long) {
        viewModelScope.launch {
            repository.deletePlayer(playerId)
        }
    }

    // Filter States
    val selectedTeam = MutableStateFlow("All") // "All", "Near", "Far"
    val selectedShotType = MutableStateFlow<ShotType?>(null)
    val selectedOutcome = MutableStateFlow<ShotOutcome?>(null)
    val minSpeedKmh = MutableStateFlow(0f)
    val selectedRallyId = MutableStateFlow<Int?>(null)

    // UI Visualization State
    val visualMode = MutableStateFlow(CourtVisualMode.SCATTER)
    val colorCodingMode = MutableStateFlow(ColorCodingMode.BY_SHOT_TYPE)
    val selectedShot = MutableStateFlow<PadelShot?>(null)

    // Calibration points
    val calibrationPoints = MutableStateFlow(SampleMatchData.initialCalibrationPoints)
    val homographyEngine = MutableStateFlow(HomographyEngine())

    // Live Tracking & Camera State
    val isTrackingActive = MutableStateFlow(false)
    val liveFps = MutableStateFlow(60)
    val detectedBallScreenPos = MutableStateFlow<Pair<Float, Float>?>(null)
    val currentShotSpeedLive = MutableStateFlow(0f)
    val recentDetectionsCount = MutableStateFlow(0)
    val isCameraPermissionGranted = MutableStateFlow(false)

    // Voice announcement toggle
    val isVoiceEnabled = MutableStateFlow(true)
    val useAliceVoice = MutableStateFlow(true) // Озвучка через Yandex SpeechKit
    val currentSpeechKitVoice = MutableStateFlow(com.example.audio.SpeechKitVoice.LERA) // По умолчанию «Лера»
    val isAlicePrewarming = MutableStateFlow(false)
    val autoCvScoreEnabled = MutableStateFlow(false) // Авто-очки по отскокам

    // Hexagon NPU / GPU Inference Engine (Snapdragon 8 Elite)
    val npuEngine = com.example.cv.PadelNpuEngine(application)
    val npuStats = MutableStateFlow(com.example.cv.NpuInferenceStats())

    // Real Camera Frame Analyzer
    val frameAnalyzer = PadelFrameAnalyzer { detection ->
        onCameraFrameDetected(detection)
    }

    fun setAccelerationBackend(backend: com.example.cv.AccelerationBackend) {
        npuEngine.setBackend(backend)
        speaker.speak("Бэкенд ускорения: ${backend.title}")
    }

    fun switchActiveModel(isPoseModel: Boolean) {
        val modelPath = if (isPoseModel) {
            "models/${com.example.cv.PadelNpuEngine.MODEL_YOLO_POSE}"
        } else {
            "models/${com.example.cv.PadelNpuEngine.MODEL_BALL_YOLO}"
        }
        npuEngine.loadModelFromAssets(modelPath)
        val name = if (isPoseModel) "YOLO 11 позы игроков" else "Трекер мяча Падел"
        speaker.speak("Загружена модель: $name")
    }

    private data class FilterCriteria(
        val team: String,
        val type: ShotType?,
        val outcome: ShotOutcome?,
        val minSpeed: Float,
        val rallyId: Int?
    )

    private val filtersFlow = combine(
        combine(selectedTeam, selectedShotType, ::Pair),
        combine(selectedOutcome, minSpeedKmh, ::Pair),
        selectedRallyId
    ) { p1, p2, rallyId ->
        FilterCriteria(
            team = p1.first,
            type = p1.second,
            outcome = p2.first,
            minSpeed = p2.second,
            rallyId = rallyId
        )
    }

    val filteredShots: StateFlow<List<PadelShot>> = combine(allShots, filtersFlow) { shots, f ->
        shots.filter { shot ->
            (f.team == "All" || shot.playerTeam == f.team) &&
            (f.type == null || shot.shotType == f.type) &&
            (f.outcome == null || shot.outcome == f.outcome) &&
            (shot.speedKmh >= f.minSpeed) &&
            (f.rallyId == null || shot.rallyId == f.rallyId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<PadelStats> = combine(filteredShots, filteredShots) { shots, _ ->
        calculateStats(shots)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PadelStats())

    private fun calculateStats(shots: List<PadelShot>): PadelStats {
        if (shots.isEmpty()) return PadelStats()
        val total = shots.size
        val avgSpeed = shots.map { it.speedKmh }.average().toFloat()
        val maxSpeed = shots.maxOfOrNull { it.speedKmh } ?: 0f
        val inCount = shots.count { it.outcome == ShotOutcome.IN || it.outcome == ShotOutcome.WINNER }
        val inPct = (inCount.toFloat() / total) * 100f

        return PadelStats(
            totalShots = total,
            avgSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
            inPercentage = inPct,
            winnerCount = shots.count { it.outcome == ShotOutcome.WINNER },
            unforcedErrorCount = shots.count { it.outcome == ShotOutcome.UNFORCED_ERROR || it.outcome == ShotOutcome.NET },
            forcedErrorCount = shots.count { it.outcome == ShotOutcome.FORCED_ERROR },
            smashCount = shots.count { it.shotType == ShotType.SMASH },
            bandejaCount = shots.count { it.shotType == ShotType.BANDEJA },
            volleyCount = shots.count { it.shotType == ShotType.VOLLEY }
        )
    }

    // ==========================================
    // SCORE & BLUETOOTH VOICE CONTROLS
    // ==========================================

    fun addPointTeam1(triggerVoice: Boolean = true) {
        val announcement = scoreEngine.addPoint(1, "Ближние", "Дальние")
        updateScoreState(announcement, triggerVoice)
    }

    fun addPointTeam2(triggerVoice: Boolean = true) {
        val announcement = scoreEngine.addPoint(2, "Ближние", "Дальние")
        updateScoreState(announcement, triggerVoice)
    }

    /**
     * Ошибка на подаче (сетка/аут/стекло).
     * 1-я ошибка -> Вторая подача (очко НЕ начисляется).
     * 2-я ошибка -> Двойная ошибка (очко соперникам).
     */
    fun registerFault() {
        val announcement = scoreEngine.registerFault("Ближние", "Дальние")
        updateScoreState(announcement, triggerVoice = true)
    }

    /**
     * Переподача (LET): мяч коснулся сетки и попал в квадрат подачи.
     * Очко НЕ начисляется, текущая попытка переигрывается.
     */
    fun registerLet() {
        val announcement = scoreEngine.registerLet("Ближние", "Дальние")
        updateScoreState(announcement, triggerVoice = true)
    }

    fun deductPointTeam1() {
        val announcement = scoreEngine.deductPoint(1, "Ближние", "Дальние")
        updateScoreState(announcement, triggerVoice = true)
    }

    fun deductPointTeam2() {
        val announcement = scoreEngine.deductPoint(2, "Ближние", "Дальние")
        updateScoreState(announcement, triggerVoice = true)
    }

    fun undoScore() {
        if (scoreEngine.undo()) {
            val announcement = "Отмена очка. Счёт: " + scoreEngine.formatScoreVoice("Ближние", "Дальние")
            updateScoreState(announcement, triggerVoice = true)
        }
    }

    fun resetMatchScore() {
        scoreEngine.resetMatch()
        updateScoreState("Матч сброшен. Счёт ноль:ноль", triggerVoice = true)
    }

    fun repeatScoreVoice() {
        val voice = scoreEngine.formatScoreVoice("Ближние", "Дальние")
        speaker.speakScore(voice)
    }

    fun toggleVoiceEnabled() {
        val next = !isVoiceEnabled.value
        isVoiceEnabled.value = next
        speaker.isVoiceEnabled = next
        if (next) {
            speaker.speak("Озвучка счёта включена")
        }
    }

    fun toggleAliceVoice() {
        val next = !useAliceVoice.value
        useAliceVoice.value = next
        speaker.useAliceVoice = next
        val voiceName = currentSpeechKitVoice.value.title
        val msg = if (next) "Включен фирменный голос $voiceName" else "Включен стандартный системный голос"
        speaker.speak(msg)
    }

    fun selectSpeechKitVoice(voice: com.example.audio.SpeechKitVoice) {
        currentSpeechKitVoice.value = voice
        speaker.speechKitVoice = voice
        speaker.useAliceVoice = true
        useAliceVoice.value = true
        val readyWord = if (voice.gender == "Мужской") "готов" else "готова"
        speaker.speak("Выбран голос ${voice.title}. Я $readyWord вести счёт матча.")
    }

    fun prewarmAliceVoiceCache() {
        if (isAlicePrewarming.value) return
        isAlicePrewarming.value = true
        val voice = currentSpeechKitVoice.value
        viewModelScope.launch(Dispatchers.IO) {
            speaker.aliceTts.prewarmCorePhrases(voice) { completed, total ->
                if (completed >= total) {
                    isAlicePrewarming.value = false
                    speaker.speak("Оффлайн кэш голоса ${voice.title} успешно подготовлен")
                }
            }
        }
    }

    fun toggleAutoCvScore() {
        autoCvScoreEnabled.value = !autoCvScoreEnabled.value
    }

    fun setScoreMode(mode: ScoreMode) {
        scoreEngine.scoreMode = mode
        resetMatchScore()
        val desc = when (mode) {
            ScoreMode.AMERICANO -> "Выбран формат Американо до ${scoreEngine.americanoTargetPoints} очков"
            ScoreMode.GOLDEN_POINT -> "Выбран теннисный формат с решающим очком (Golden Point)"
            ScoreMode.TRADITIONAL_ADVANTAGE -> "Выбран классический формат (Больше/Меньше)"
            ScoreMode.TIE_BREAK_ONLY -> "Выбран формат тай-брейка до 7 очков"
        }
        speaker.speak(desc)
    }

    fun setAmericanoTargetPoints(points: Int) {
        val validPoints = points.coerceIn(8, 100)
        scoreEngine.americanoTargetPoints = validPoints
        resetMatchScore()
        speaker.speak("Американо: играем ровно до $validPoints очков")
    }

    private fun updateScoreState(announcement: String, triggerVoice: Boolean) {
        val display = scoreEngine.getDisplayScore()
        _scoreState.value = LiveMatchScoreState(
            team1PointsDisplay = display.first,
            team2PointsDisplay = display.second,
            team1Games = scoreEngine.team1Games,
            team2Games = scoreEngine.team2Games,
            team1Sets = scoreEngine.team1Sets,
            team2Sets = scoreEngine.team2Sets,
            isTieBreak = scoreEngine.isTieBreak,
            servingTeam = scoreEngine.servingTeam,
            lastAnnouncement = announcement,
            scoreMode = scoreEngine.scoreMode,
            americanoTargetPoints = scoreEngine.americanoTargetPoints,
            isMatchFinished = scoreEngine.isMatchFinished,
            serveAttempt = scoreEngine.serveAttempt
        )
        if (triggerVoice && isVoiceEnabled.value) {
            speaker.speakScore(announcement)
        }
    }

    // ==========================================
    // CAMERA CV ANALYSIS & TRACKING
    // ==========================================

    private fun onCameraFrameDetected(detection: MotionDetectionResult) {
        if (!isTrackingActive.value) return

        // Выполняем инференс через NPU движок
        val stats = npuEngine.runInference(
            detection.ballCandidateU,
            detection.ballCandidateV,
            detection.motionIntensity
        )
        npuStats.value = stats
        liveFps.value = stats.fps.toInt()

        detectedBallScreenPos.value = Pair(detection.ballCandidateU, detection.ballCandidateV)
        recentDetectionsCount.value = recentDetectionsCount.value + 1

        if (detection.isSuddenDirectionChange) {
            // Зафиксирован отскок мяча от покрытия через камеру
            val courtPt = homographyEngine.value.pixelToCourt(
                detection.ballCandidateU,
                detection.ballCandidateV
            )
            val speedEst = (detection.motionIntensity * 4.5f).coerceIn(60f, 125f)
            currentShotSpeedLive.value = speedEst

            viewModelScope.launch {
                addManualBounce(courtPt.x, courtPt.y, speedEst)

                if (autoCvScoreEnabled.value) {
                    // Если мяч приземлился в корт соперника
                    val scoringTeam = if (courtPt.y > 0) 1 else 2
                    if (scoringTeam == 1) addPointTeam1(triggerVoice = true)
                    else addPointTeam2(triggerVoice = true)
                }
            }
        }
    }

    fun toggleTracking() {
        val current = isTrackingActive.value
        isTrackingActive.value = !current
        if (!current) {
            startTrackingSimulation()
        }
    }

    private fun startTrackingSimulation() {
        viewModelScope.launch {
            var counter = 0
            while (isTrackingActive.value) {
                kotlinx.coroutines.delay(120)
                val screenU = Random.nextDouble(0.2, 0.8).toFloat()
                val screenV = Random.nextDouble(0.3, 0.85).toFloat()
                detectedBallScreenPos.value = Pair(screenU, screenV)
                counter++
                recentDetectionsCount.value = counter

                if (counter % 14 == 0) {
                    val courtPt = homographyEngine.value.pixelToCourt(screenU, screenV)
                    val speed = Random.nextDouble(75.0, 118.0).toFloat()
                    currentShotSpeedLive.value = speed
                    addManualBounce(courtPt.x, courtPt.y, speed)
                }
            }
            detectedBallScreenPos.value = null
        }
    }

    val colorMode: StateFlow<ColorCodingMode> = colorCodingMode.asStateFlow()

    fun setColorMode(mode: ColorCodingMode) {
        colorCodingMode.value = mode
    }

    fun resetCalibrationToDefault() {
        calibrationPoints.value = SampleMatchData.initialCalibrationPoints
        recomputeHomography(SampleMatchData.initialCalibrationPoints)
    }

    fun updateCalibrationPoint(id: Int, newX: Float, newY: Float) {
        val current = calibrationPoints.value.map { pt ->
            if (pt.id == id) pt.copy(screenX = newX, screenY = newY) else pt
        }
        calibrationPoints.value = current
        recomputeHomography(current)
    }

    private fun recomputeHomography(pts: List<CourtCalibrationPoint>) {
        if (pts.size >= 4) {
            val src = pts.map { HomographyEngine.Point2D(it.screenX, it.screenY) }
            val dst = pts.map { HomographyEngine.Point2D(it.courtX, it.courtY) }
            homographyEngine.value = HomographyEngine.computeHomography(src, dst)
        }
    }

    fun setVisualMode(mode: CourtVisualMode) {
        visualMode.value = mode
    }

    fun selectShot(shot: PadelShot?) {
        selectedShot.value = shot
    }

    fun setTeamFilter(team: String) {
        selectedTeam.value = team
    }

    fun setShotTypeFilter(type: ShotType?) {
        selectedShotType.value = type
    }

    fun setOutcomeFilter(outcome: ShotOutcome?) {
        selectedOutcome.value = outcome
    }

    fun setMinSpeed(speed: Float) {
        minSpeedKmh.value = speed
    }

    fun setRallyFilter(rallyId: Int?) {
        selectedRallyId.value = rallyId
    }

    fun clearFilters() {
        selectedTeam.value = "All"
        selectedShotType.value = null
        selectedOutcome.value = null
        minSpeedKmh.value = 0f
        selectedRallyId.value = null
        selectedShot.value = null
    }

    fun addManualBounce(courtX: Float, courtY: Float, speedParam: Float? = null) {
        viewModelScope.launch {
            val speed = speedParam ?: Random.nextDouble(65.0, 115.0).toFloat()
            val types = listOf(ShotType.FOREHAND, ShotType.BACKHAND, ShotType.VOLLEY, ShotType.SMASH, ShotType.BANDEJA)
            val randomType = types.random()
            val outcome = if (HomographyEngine().isInsideCourt(courtX, courtY)) {
                if (speed > 105f) ShotOutcome.WINNER else ShotOutcome.IN
            } else ShotOutcome.OUT_BASE

            val playerName = com.example.cv.PlayerRecognitionEngine.resolvePlayerByPosition(
                courtX,
                courtY,
                recognizedPlayers.value
            )

            val currentMatchId = activeMatchId.value
            val newShot = PadelShot(
                id = System.currentTimeMillis(),
                matchId = currentMatchId,
                rallyId = selectedRallyId.value ?: 1,
                shotNumber = (allShots.value.size + 1),
                timestampSec = (allShots.value.lastOrNull()?.timestampSec ?: 0.0) + 3.0,
                player = playerName,
                playerTeam = if (courtY < 0) "Near" else "Far",
                shotType = randomType,
                bounceX = courtX,
                bounceY = courtY,
                speedKmh = speed,
                outcome = outcome,
                notes = "Удар игрока: $playerName"
            )
            repository.addShot(newShot)
            selectedShot.value = newShot

            if (outcome == ShotOutcome.WINNER && isVoiceEnabled.value) {
                speaker.speakWinnerSpeed(speed, "$playerName: ${randomType.displayName}")
            }
        }
    }

    fun exportToCsv(): String {
        return SwingVisionCsvHandler.exportToCsv(allShots.value)
    }

    fun importCsv(csvContent: String) {
        viewModelScope.launch {
            val parsed = SwingVisionCsvHandler.parseCsv(csvContent, 1L)
            if (parsed.isNotEmpty()) {
                repository.importShots(parsed, 1L)
            }
        }
    }

    fun resetData() {
        viewModelScope.launch {
            repository.resetToDefault()
            clearFilters()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speaker.shutdown()
        cameraExecutor.shutdown()
    }
}

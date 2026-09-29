package com.example

import com.example.math.HomographyEngine
import com.example.model.PadelScoreEngine
import com.example.model.ScoreMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PadelCoreUnitTest {

    @Test
    fun testHomographyPointInsideCourt() {
        val engine = HomographyEngine()
        assertTrue("Point (0, 0) is court center and must be inside", engine.isInsideCourt(0f, 0f))
        assertTrue("Point (-4.5, 9.5) is inside court boundaries", engine.isInsideCourt(-4.5f, 9.5f))
        assertFalse("Point (6.0, 0.0) is outside 10m width", engine.isInsideCourt(6.0f, 0f))
        assertFalse("Point (0.0, 11.5) is outside 20m length", engine.isInsideCourt(0.0f, 11.5f))
    }

    @Test
    fun testScoreEngineGoldenPoint() {
        val engine = PadelScoreEngine(ScoreMode.GOLDEN_POINT)

        // 15:0
        var voice = engine.addPoint(1)
        assertEquals("пятнадцать : ноль", voice)
        assertEquals("15", engine.getDisplayScore().first)
        assertEquals("0", engine.getDisplayScore().second)

        // 15:15
        voice = engine.addPoint(2)
        assertEquals("По пятнадцати", voice)

        // 30:15
        engine.addPoint(1)
        // 30:30
        engine.addPoint(2)
        // 40:30
        engine.addPoint(1)
        // 40:40 -> Golden Point!
        voice = engine.addPoint(2)
        assertTrue(voice.contains("решающее очко"))

        // Победное очко в гейме
        voice = engine.addPoint(1)
        assertTrue("Expected game win announcement, got: $voice", voice.contains("Гейм"))
        assertEquals(1, engine.team1Games)
        assertEquals(0, engine.team2Games)
        assertEquals("0", engine.getDisplayScore().first)
        assertEquals("0", engine.getDisplayScore().second)
    }

    @Test
    fun testScoreEngineUndo() {
        val engine = PadelScoreEngine(ScoreMode.AMERICANO, americanoTargetPoints = 24)
        engine.addPoint(1)
        assertEquals("1", engine.getDisplayScore().first)

        assertTrue(engine.undo())
        assertEquals("0", engine.getDisplayScore().first)
    }

    @Test
    fun testAmericanoScoring() {
        val engine = PadelScoreEngine(ScoreMode.AMERICANO, americanoTargetPoints = 24)
        assertEquals("0", engine.getDisplayScore().first)
        assertEquals("0", engine.getDisplayScore().second)

        // 1 очко Ближним
        val v1 = engine.addPoint(1)
        assertTrue(v1.contains("1 : 0"))
        assertEquals("1", engine.getDisplayScore().first)

        // Играем до 24 очков
        for (i in 0 until 11) {
            engine.addPoint(1)
            engine.addPoint(2)
        }
        // Сейчас счёт 12:11, разыграно 23 очка. Следующее очко - матчбол
        val lastPoint = engine.addPoint(1)
        assertTrue("Expected match finished for Americano, got: $lastPoint", lastPoint.contains("завершён") || engine.isMatchFinished)
        assertTrue(engine.isMatchFinished)
        assertEquals("13", engine.getDisplayScore().first)
        assertEquals("11", engine.getDisplayScore().second)
    }

    @Test
    fun testTieBreakTransition() {
        val engine = PadelScoreEngine(ScoreMode.GOLDEN_POINT)
        // Симулируем 6:6
        for (i in 0 until 6) {
            engine.addPoint(1) // win game
            engine.addPoint(1)
            engine.addPoint(1)
            engine.addPoint(1)

            engine.addPoint(2)
            engine.addPoint(2)
            engine.addPoint(2)
            engine.addPoint(2)
        }

        assertTrue("Must be tiebreak at 6:6", engine.isTieBreak)
        val voice = engine.addPoint(1)
        assertEquals("1 : 0", voice)
        assertEquals("1", engine.getDisplayScore().first)
        assertEquals("0", engine.getDisplayScore().second)
    }

    @Test
    fun testPlayerRecognitionEngine() {
        val players = listOf(
            com.example.model.PadelPlayer(id = 1, name = "Александр", defaultTeam = "Near", preferredSide = "Левый (Drive)"),
            com.example.model.PadelPlayer(id = 2, name = "Никита", defaultTeam = "Near", preferredSide = "Правый (Reverse)"),
            com.example.model.PadelPlayer(id = 3, name = "Марк", defaultTeam = "Far", preferredSide = "Левый (Drive)"),
            com.example.model.PadelPlayer(id = 4, name = "Денис", defaultTeam = "Far", preferredSide = "Правый (Reverse)")
        )

        val recognized = com.example.cv.PlayerRecognitionEngine.identifyPlayersOnCourt(players)
        assertEquals(4, recognized.size)

        // Мяч на ближней левой стороне (-2.5, -4.0) должен быть распознан как Александр (Near, Drive)
        val player = com.example.cv.PlayerRecognitionEngine.resolvePlayerByPosition(-2.5f, -4.0f, recognized)
        assertEquals("Александр", player)

        // Мяч на дальней правой стороне (3.0, 5.0) должен быть распознан как Денис (Far, Reverse)
        val farPlayer = com.example.cv.PlayerRecognitionEngine.resolvePlayerByPosition(3.0f, 5.0f, recognized)
        assertEquals("Денис", farPlayer)
    }

    @Test
    fun testServeFaultAndLet() {
        val engine = PadelScoreEngine(ScoreMode.AMERICANO, americanoTargetPoints = 24)
        assertEquals(1, engine.serveAttempt)
        assertEquals("0", engine.getDisplayScore().first)
        assertEquals("0", engine.getDisplayScore().second)

        // 1. Первая ошибка подачи -> Вторая подача, очко НЕ начисляется
        val vFault1 = engine.registerFault("Ближние", "Дальние")
        assertTrue(vFault1.contains("Вторая подача"))
        assertEquals(2, engine.serveAttempt)
        assertEquals("0", engine.getDisplayScore().first)
        assertEquals("0", engine.getDisplayScore().second)

        // 2. Переподача (Let) на второй подаче -> Вторая подача переигрывается, очко НЕ начисляется
        val vLet = engine.registerLet("Ближние", "Дальние")
        assertTrue(vLet.contains("Переподача") && vLet.contains("не начисляется"))
        assertEquals(2, engine.serveAttempt)
        assertEquals("0", engine.getDisplayScore().first)
        assertEquals("0", engine.getDisplayScore().second)

        // 3. Вторая ошибка подряд -> Двойная ошибка! Очко уходит соперникам (Дальним)
        val vDoubleFault = engine.registerFault("Ближние", "Дальние")
        assertTrue(vDoubleFault.contains("Двойная ошибка"))
        assertEquals(1, engine.serveAttempt)
        assertEquals("0", engine.getDisplayScore().first)
        assertEquals("1", engine.getDisplayScore().second)
    }

    @Test
    fun testDeductPoint() {
        val engine = PadelScoreEngine(ScoreMode.AMERICANO, americanoTargetPoints = 24)
        engine.addPoint(1)
        engine.addPoint(1)
        engine.addPoint(2)
        assertEquals("2", engine.getDisplayScore().first)
        assertEquals("1", engine.getDisplayScore().second)

        // Отнимаем очко у команды 1
        val voice = engine.deductPoint(1)
        assertTrue(voice.contains("Минус очко"))
        assertEquals("1", engine.getDisplayScore().first)
        assertEquals("1", engine.getDisplayScore().second)

        // Отнимаем очко у команды 2
        engine.deductPoint(2)
        assertEquals("1", engine.getDisplayScore().first)
        assertEquals("0", engine.getDisplayScore().second)
    }
}

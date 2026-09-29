package com.example.data

import com.example.model.PadelMatch
import com.example.model.PadelPlayer
import com.example.model.PadelShot
import com.example.model.SampleMatchData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PadelRepository(private val dao: PadelDao) {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = dao.getMatchById(1L)
            if (existing == null) {
                seedInitialData()
            }
        }
    }

    suspend fun seedInitialData() {
        val match = SampleMatchData.defaultMatch
        dao.insertMatch(MatchEntity.fromDomain(match))

        val shotEntities = SampleMatchData.defaultShots.map { ShotEntity.fromDomain(it, match.id) }
        dao.insertShots(shotEntities)

        // Seed 4 initial players for recognition
        val defaultPlayers = listOf(
            PadelPlayer(
                id = 1,
                name = "Александр К.",
                defaultTeam = "Near",
                preferredSide = "Левый (Drive)",
                racketColor = "Красная (Bullpadel Hack)",
                shirtColor = "Красная",
                avatarEmoji = "🔴",
                matchesPlayed = 14,
                winRate = 71.4f
            ),
            PadelPlayer(
                id = 2,
                name = "Никита Ф.",
                defaultTeam = "Near",
                preferredSide = "Правый (Reverse)",
                racketColor = "Черная (Babolat Air Viper)",
                shirtColor = "Черная",
                avatarEmoji = "⚫",
                matchesPlayed = 20,
                winRate = 80.0f
            ),
            PadelPlayer(
                id = 3,
                name = "Марк Д.",
                defaultTeam = "Far",
                preferredSide = "Левый (Drive)",
                racketColor = "Синяя (Head Speed Pro)",
                shirtColor = "Белая",
                avatarEmoji = "🔵",
                matchesPlayed = 11,
                winRate = 54.5f
            ),
            PadelPlayer(
                id = 4,
                name = "Денис В.",
                defaultTeam = "Far",
                preferredSide = "Правый (Reverse)",
                racketColor = "Желтая (Nox AT10)",
                shirtColor = "Желтая",
                avatarEmoji = "🟡",
                matchesPlayed = 9,
                winRate = 44.4f
            )
        )
        defaultPlayers.forEach { dao.insertPlayer(PlayerEntity.fromDomain(it)) }

        // Seed default calibration preset
        dao.insertCalibration(
            CalibrationPresetEntity(
                id = 1L,
                name = "Камера на заднем стекле (2.2м)",
                p1x = 0.25f, p1y = 0.20f,
                p2x = 0.75f, p2y = 0.20f,
                p3x = 0.92f, p3y = 0.88f,
                p4x = 0.08f, p4y = 0.88f,
                mountHeightMeters = 2.2f
            )
        )
    }

    // Matches
    fun getAllMatches(): Flow<List<PadelMatch>> {
        return dao.getAllMatches().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getMatchById(id: Long): PadelMatch? {
        return dao.getMatchById(id)?.toDomain()
    }

    suspend fun createMatch(match: PadelMatch): Long {
        return dao.insertMatch(MatchEntity.fromDomain(match))
    }

    suspend fun updateMatch(match: PadelMatch) {
        dao.insertMatch(MatchEntity.fromDomain(match))
    }

    suspend fun deleteMatch(matchId: Long) {
        dao.deleteMatch(matchId)
        dao.deleteShotsForMatch(matchId)
    }

    // Players
    fun getAllPlayers(): Flow<List<PadelPlayer>> {
        return dao.getAllPlayers().map { list -> list.map { it.toDomain() } }
    }

    suspend fun addPlayer(player: PadelPlayer): Long {
        return dao.insertPlayer(PlayerEntity.fromDomain(player))
    }

    suspend fun deletePlayer(playerId: Long) {
        dao.deletePlayer(playerId)
    }

    // Shots
    fun getShotsForMatch(matchId: Long): Flow<List<PadelShot>> {
        return dao.getShotsForMatch(matchId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun addShot(shot: PadelShot): Long {
        return dao.insertShot(ShotEntity.fromDomain(shot, shot.matchId))
    }

    suspend fun importShots(shots: List<PadelShot>, matchId: Long) {
        dao.insertShots(shots.map { ShotEntity.fromDomain(it, matchId) })
    }

    suspend fun resetToDefault() {
        seedInitialData()
    }
}

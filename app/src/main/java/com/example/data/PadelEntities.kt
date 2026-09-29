package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.PadelMatch
import com.example.model.PadelPlayer
import com.example.model.PadelShot
import com.example.model.ShotOutcome
import com.example.model.ShotType

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val scoreFinal: String,
    val durationMinutes: Int,
    val location: String,
    val courtType: String,
    val playerNear1: String = "Ближний левый",
    val playerNear2: String = "Ближний правый",
    val playerFar1: String = "Дальний левый",
    val playerFar2: String = "Дальний правый",
    val totalShots: Int,
    val avgSpeedKmh: Float,
    val maxSpeedKmh: Float,
    val inPercentage: Float
) {
    fun toDomain() = PadelMatch(
        id = id,
        title = title,
        date = date,
        scoreFinal = scoreFinal,
        durationMinutes = durationMinutes,
        location = location,
        courtType = courtType,
        playerNear1 = playerNear1,
        playerNear2 = playerNear2,
        playerFar1 = playerFar1,
        playerFar2 = playerFar2,
        totalShots = totalShots,
        avgSpeedKmh = avgSpeedKmh,
        maxSpeedKmh = maxSpeedKmh,
        inPercentage = inPercentage
    )

    companion object {
        fun fromDomain(m: PadelMatch) = MatchEntity(
            id = m.id,
            title = m.title,
            date = m.date,
            scoreFinal = m.scoreFinal,
            durationMinutes = m.durationMinutes,
            location = m.location,
            courtType = m.courtType,
            playerNear1 = m.playerNear1,
            playerNear2 = m.playerNear2,
            playerFar1 = m.playerFar1,
            playerFar2 = m.playerFar2,
            totalShots = m.totalShots,
            avgSpeedKmh = m.avgSpeedKmh,
            maxSpeedKmh = m.maxSpeedKmh,
            inPercentage = m.inPercentage
        )
    }
}

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultTeam: String = "Near",
    val preferredSide: String = "Левый (Drive)",
    val racketColor: String = "Черная",
    val shirtColor: String = "Темно-синяя",
    val avatarEmoji: String = "🎾",
    val matchesPlayed: Int = 0,
    val winRate: Float = 50f
) {
    fun toDomain() = PadelPlayer(
        id = id,
        name = name,
        defaultTeam = defaultTeam,
        preferredSide = preferredSide,
        racketColor = racketColor,
        shirtColor = shirtColor,
        avatarEmoji = avatarEmoji,
        matchesPlayed = matchesPlayed,
        winRate = winRate
    )

    companion object {
        fun fromDomain(p: PadelPlayer) = PlayerEntity(
            id = p.id,
            name = p.name,
            defaultTeam = p.defaultTeam,
            preferredSide = p.preferredSide,
            racketColor = p.racketColor,
            shirtColor = p.shirtColor,
            avatarEmoji = p.avatarEmoji,
            matchesPlayed = p.matchesPlayed,
            winRate = p.winRate
        )
    }
}

@Entity(tableName = "shots")
data class ShotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val rallyId: Int,
    val shotNumber: Int,
    val timestampSec: Double,
    val player: String,
    val playerTeam: String,
    val shotType: String,
    val bounceX: Float,
    val bounceY: Float,
    val playerPosX: Float?,
    val playerPosY: Float?,
    val speedKmh: Float,
    val outcome: String,
    val scoreSnapshot: String,
    val notes: String
) {
    fun toDomain() = PadelShot(
        id = id,
        matchId = matchId,
        rallyId = rallyId,
        shotNumber = shotNumber,
        timestampSec = timestampSec,
        player = player,
        playerTeam = playerTeam,
        shotType = ShotType.fromString(shotType),
        bounceX = bounceX,
        bounceY = bounceY,
        playerPosX = playerPosX,
        playerPosY = playerPosY,
        speedKmh = speedKmh,
        outcome = ShotOutcome.fromString(outcome),
        scoreSnapshot = scoreSnapshot,
        notes = notes
    )

    companion object {
        fun fromDomain(shot: PadelShot, matchId: Long = shot.matchId) = ShotEntity(
            id = shot.id,
            matchId = matchId,
            rallyId = shot.rallyId,
            shotNumber = shot.shotNumber,
            timestampSec = shot.timestampSec,
            player = shot.player,
            playerTeam = shot.playerTeam,
            shotType = shot.shotType.name,
            bounceX = shot.bounceX,
            bounceY = shot.bounceY,
            playerPosX = shot.playerPosX,
            playerPosY = shot.playerPosY,
            speedKmh = shot.speedKmh,
            outcome = shot.outcome.name,
            scoreSnapshot = shot.scoreSnapshot,
            notes = shot.notes
        )
    }
}

@Entity(tableName = "calibrations")
data class CalibrationPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val p1x: Float, val p1y: Float,
    val p2x: Float, val p2y: Float,
    val p3x: Float, val p3y: Float,
    val p4x: Float, val p4y: Float,
    val mountHeightMeters: Float
)

package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PadelDao {

    // Matches
    @Query("SELECT * FROM matches ORDER BY id DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchById(matchId: Long): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatch(matchId: Long)

    // Players
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deletePlayer(playerId: Long)

    // Shots
    @Query("SELECT * FROM shots WHERE matchId = :matchId ORDER BY rallyId ASC, shotNumber ASC")
    fun getShotsForMatch(matchId: Long): Flow<List<ShotEntity>>

    @Query("SELECT * FROM shots WHERE matchId = :matchId AND rallyId = :rallyId ORDER BY shotNumber ASC")
    fun getShotsForRally(matchId: Long, rallyId: Int): Flow<List<ShotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShot(shot: ShotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShots(shots: List<ShotEntity>)

    @Query("DELETE FROM shots WHERE matchId = :matchId")
    suspend fun deleteShotsForMatch(matchId: Long)

    // Calibrations
    @Query("SELECT * FROM calibrations ORDER BY id DESC")
    fun getAllCalibrations(): Flow<List<CalibrationPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalibration(calibration: CalibrationPresetEntity): Long
}

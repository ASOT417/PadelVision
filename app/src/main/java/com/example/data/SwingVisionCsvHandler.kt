package com.example.data

import com.example.model.PadelShot
import com.example.model.ShotOutcome
import com.example.model.ShotType
import java.util.Locale

object SwingVisionCsvHandler {

    private const val HEADER = "shot_id,rally_id,timestamp_sec,player,player_team,shot_type,bounce_x,bounce_y,player_pos_x,player_pos_y,speed_kmh,outcome,notes"

    /**
     * Generates a CSV string conforming to the Left Join & SwingVision format.
     */
    fun exportToCsv(shots: List<PadelShot>): String {
        val sb = StringBuilder()
        sb.append(HEADER).append("\n")
        shots.forEach { shot ->
            val line = String.format(
                Locale.US,
                "%d,%d,%.2f,\"%s\",\"%s\",\"%s\",%.2f,%.2f,%s,%s,%.1f,\"%s\",\"%s\"",
                shot.id,
                shot.rallyId,
                shot.timestampSec,
                shot.player,
                shot.playerTeam,
                shot.shotType.displayName,
                shot.bounceX,
                shot.bounceY,
                shot.playerPosX?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                shot.playerPosY?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                shot.speedKmh,
                shot.outcome.displayName,
                shot.notes.replace("\"", "\"\"")
            )
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    /**
     * Parses a CSV string back into a list of PadelShot models.
     */
    fun parseCsv(csvText: String, matchId: Long = 1L): List<PadelShot> {
        val lines = csvText.lines()
        val result = mutableListOf<PadelShot>()

        lines.drop(1).forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                val tokens = trimmed.split(",")
                if (tokens.size >= 8) {
                    try {
                        val shotId = tokens[0].trim().toLongOrNull() ?: (index + 1).toLong()
                        val rallyId = tokens[1].trim().toIntOrNull() ?: 1
                        val timestamp = tokens[2].trim().toDoubleOrNull() ?: 0.0
                        val player = tokens[3].trim().removeSurrounding("\"")
                        val playerTeam = if (tokens.size > 4) tokens[4].trim().removeSurrounding("\"") else "Near"
                        val shotTypeStr = if (tokens.size > 5) tokens[5].trim().removeSurrounding("\"") else ""
                        val bx = if (tokens.size > 6) tokens[6].trim().toFloatOrNull() ?: 0f else 0f
                        val by = if (tokens.size > 7) tokens[7].trim().toFloatOrNull() ?: 0f else 0f
                        val speed = if (tokens.size > 10) tokens[10].trim().toFloatOrNull() ?: 75f else 75f
                        val outcomeStr = if (tokens.size > 11) tokens[11].trim().removeSurrounding("\"") else "In"
                        val notes = if (tokens.size > 12) tokens[12].trim().removeSurrounding("\"") else ""

                        result.add(
                            PadelShot(
                                id = shotId,
                                matchId = matchId,
                                rallyId = rallyId,
                                shotNumber = index + 1,
                                timestampSec = timestamp,
                                player = player,
                                playerTeam = playerTeam,
                                shotType = ShotType.fromString(shotTypeStr),
                                bounceX = bx,
                                bounceY = by,
                                speedKmh = speed,
                                outcome = ShotOutcome.fromString(outcomeStr),
                                notes = notes
                            )
                        )
                    } catch (e: Exception) {
                        // Skip corrupted row
                    }
                }
            }
        }
        return result
    }
}

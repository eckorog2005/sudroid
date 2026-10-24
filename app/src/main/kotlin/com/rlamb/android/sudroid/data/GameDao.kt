/*
 *  Copyright 2026 Roger Lamb
 *
 *  This file is part of Sudroid.
 *
 *  Sudroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Sudroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Sudroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rlamb.android.sudroid.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rlamb.android.sudroid.game.Difficulty
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "puzzles")
data class PuzzleEntity(
    @PrimaryKey val id: Int,
    val difficulty: Difficulty,
    val digits: String,
)

/** Holds at most one row per slot: the regular game in progress and the daily challenge in progress. */
@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey val slot: Int,
    @ColumnInfo(name = "puzzle_id") val puzzleId: Int,
    val entries: String,
    @ColumnInfo(name = "elapsed_seconds") val elapsedSeconds: Int,
    @ColumnInfo(defaultValue = "") val notes: String,
    @ColumnInfo(name = "daily_day") val dailyDay: Long? = null,
    @ColumnInfo(defaultValue = "0") val hints: Int = 0,
)

@Entity(tableName = "completed_games")
data class CompletedGameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "puzzle_id") val puzzleId: Int,
    val difficulty: Difficulty,
    @ColumnInfo(name = "elapsed_seconds") val elapsedSeconds: Int,
    /** Epoch milliseconds. */
    @ColumnInfo(name = "completed_at") val completedAt: Long,
    @ColumnInfo(name = "daily_day") val dailyDay: Long? = null,
    @ColumnInfo(defaultValue = "0") val hints: Int = 0,
)

private const val SAVED_GAME = "SELECT p.*, s.entries, s.elapsed_seconds, s.notes, s.daily_day, s.hints FROM saved_game s " +
    "JOIN puzzles p ON p.id = s.puzzle_id WHERE s.slot = :slot"

data class SavedGameWithPuzzle(
    @Embedded val puzzle: PuzzleEntity,
    val entries: String,
    @ColumnInfo(name = "elapsed_seconds") val elapsedSeconds: Int,
    val notes: String,
    @ColumnInfo(name = "daily_day") val dailyDay: Long?,
    val hints: Int,
)

@Dao
interface GameDao {
    @Query("SELECT * FROM puzzles WHERE difficulty = :difficulty ORDER BY random() LIMIT 1")
    suspend fun randomPuzzle(difficulty: Difficulty): PuzzleEntity?

    @Query("SELECT count(*) FROM puzzles")
    suspend fun puzzleCount(): Int

    @Query("SELECT * FROM puzzles ORDER BY id LIMIT 1 OFFSET :index")
    suspend fun puzzleAt(index: Int): PuzzleEntity?

    @Query(SAVED_GAME)
    suspend fun savedGame(slot: Int): SavedGameWithPuzzle?

    @Query(SAVED_GAME)
    fun observeSavedGame(slot: Int): Flow<SavedGameWithPuzzle?>

    @Upsert
    suspend fun save(game: SavedGameEntity)

    @Query("DELETE FROM saved_game WHERE slot = :slot")
    suspend fun deleteSavedGame(slot: Int)

    @Insert
    suspend fun insertCompletedGame(game: CompletedGameEntity)

    @Transaction
    suspend fun completeGame(game: CompletedGameEntity, slot: Int) {
        insertCompletedGame(game)
        deleteSavedGame(slot)
    }

    @Query("SELECT * FROM completed_games")
    fun completedGames(): Flow<List<CompletedGameEntity>>
}

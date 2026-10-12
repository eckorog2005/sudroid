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
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.rlamb.android.sudroid.game.Difficulty
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "puzzles")
data class PuzzleEntity(
    @PrimaryKey val id: Int,
    val difficulty: Difficulty,
    val digits: String,
)

/** Holds at most one row: the game in progress. */
@Entity(tableName = "saved_game")
data class SavedGameEntity(
    @PrimaryKey val slot: Int = 0,
    @ColumnInfo(name = "puzzle_id") val puzzleId: Int,
    val entries: String,
    @ColumnInfo(name = "elapsed_seconds") val elapsedSeconds: Int,
)

data class SavedGameWithPuzzle(
    @Embedded val puzzle: PuzzleEntity,
    val entries: String,
    @ColumnInfo(name = "elapsed_seconds") val elapsedSeconds: Int,
)

@Dao
interface GameDao {
    @Query("SELECT * FROM puzzles WHERE difficulty = :difficulty ORDER BY random() LIMIT 1")
    suspend fun randomPuzzle(difficulty: Difficulty): PuzzleEntity?

    @Query(
        "SELECT p.*, s.entries, s.elapsed_seconds FROM saved_game s " +
            "JOIN puzzles p ON p.id = s.puzzle_id LIMIT 1",
    )
    suspend fun savedGame(): SavedGameWithPuzzle?

    @Query("SELECT EXISTS(SELECT * FROM saved_game)")
    fun hasSavedGame(): Flow<Boolean>

    @Upsert
    suspend fun save(game: SavedGameEntity)

    @Query("DELETE FROM saved_game")
    suspend fun deleteSavedGame()
}

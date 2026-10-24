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

package com.rlamb.android.sudroid.game

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

enum class Difficulty { EASY, MEDIUM, HARD }

/** [digits] is 81 digits in row-major order, with `0` for an empty cell. */
data class Puzzle(val id: Int, val difficulty: Difficulty, val digits: String)

/** [entries] and [notes] use the formats of `Board.entriesString()` and `Board.notesString()`. */
data class SavedGame(val puzzle: Puzzle, val entries: String, val elapsedSeconds: Int, val notes: String = "")

@Serializable
sealed interface GameRequest {
    @Serializable
    data class New(val difficulty: Difficulty) : GameRequest

    @Serializable
    data object Resume : GameRequest
}

interface GameRepository {
    suspend fun randomPuzzle(difficulty: Difficulty): Puzzle
    suspend fun savedGame(): SavedGame?
    fun hasSavedGame(): Flow<Boolean>
    suspend fun save(game: SavedGame)
    suspend fun deleteSavedGame()

    /** Records a solved game for the stats and deletes the saved game, together. */
    suspend fun completeGame(puzzle: Puzzle, elapsedSeconds: Int)
}

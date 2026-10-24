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

/**
 * [entries] and [notes] use the formats of `Board.entriesString()` and `Board.notesString()`.
 * [dailyDay] is the epoch day of the daily challenge this game belongs to, or null for a regular game.
 */
data class SavedGame(
    val puzzle: Puzzle,
    val entries: String,
    val elapsedSeconds: Int,
    val notes: String = "",
    val dailyDay: Long? = null,
    val hints: Int = 0,
)

@Serializable
sealed interface GameRequest {
    @Serializable
    data class New(val difficulty: Difficulty) : GameRequest

    @Serializable
    data object Resume : GameRequest

    /** Today's challenge, resumed if it was already started. */
    @Serializable
    data object Daily : GameRequest
}

interface GameRepository {
    suspend fun randomPuzzle(difficulty: Difficulty): Puzzle

    /** The same puzzle on every device for a given [epochDay]. */
    suspend fun dailyPuzzle(epochDay: Long): Puzzle

    /** The local calendar day, counted from 1970-01-01. */
    fun today(): Long

    /** The regular game in progress. The daily challenge is saved separately, so neither overwrites the other. */
    suspend fun savedGame(): SavedGame?
    suspend fun savedDailyGame(epochDay: Long): SavedGame?
    /** [savedGame], updated as it's saved and deleted. */
    fun observeSavedGame(): Flow<SavedGame?>

    /** Saves to the daily challenge's slot when [SavedGame.dailyDay] is set, and to the regular one otherwise. */
    suspend fun save(game: SavedGame)
    suspend fun deleteSavedGame()

    /** Records a solved game for the stats and deletes its save, together. */
    suspend fun completeGame(puzzle: Puzzle, elapsedSeconds: Int, dailyDay: Long? = null, hints: Int = 0)
}

/**
 * Steps through the puzzles by a prime stride, so consecutive days get unrelated puzzles and every
 * puzzle comes up once before any repeats, as long as [puzzleCount] is not a multiple of the stride.
 */
fun dailyPuzzleIndex(epochDay: Long, puzzleCount: Int): Int =
    Math.floorMod(epochDay * DAILY_STRIDE, puzzleCount.toLong()).toInt()

private const val DAILY_STRIDE = 7919L

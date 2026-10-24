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

import android.content.Context
import com.rlamb.android.sudroid.game.Difficulty
import com.rlamb.android.sudroid.game.GameRepository
import com.rlamb.android.sudroid.game.Puzzle
import com.rlamb.android.sudroid.game.SavedGame
import com.rlamb.android.sudroid.game.dailyPuzzleIndex
import com.rlamb.android.sudroid.stats.Completion
import com.rlamb.android.sudroid.stats.Stats
import com.rlamb.android.sudroid.stats.StatsRepository
import com.rlamb.android.sudroid.stats.statsOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.TimeZone

class RoomGameRepository(
    private val dao: GameDao,
    private val now: () -> Long = System::currentTimeMillis,
    private val timeZone: () -> TimeZone = TimeZone::getDefault,
) : GameRepository, StatsRepository {
    override suspend fun randomPuzzle(difficulty: Difficulty): Puzzle =
        checkNotNull(dao.randomPuzzle(difficulty)) { "No $difficulty puzzles in the database" }.toPuzzle()

    override suspend fun dailyPuzzle(epochDay: Long): Puzzle {
        val index = dailyPuzzleIndex(epochDay, dao.puzzleCount())
        return checkNotNull(dao.puzzleAt(index)) { "No puzzle at $index" }.toPuzzle()
    }

    override fun today(): Long = epochDay(now())

    override suspend fun savedGame(): SavedGame? = dao.savedGame(REGULAR_SLOT)?.toSavedGame()

    override suspend fun savedDailyGame(epochDay: Long): SavedGame? =
        dao.savedGame(DAILY_SLOT)?.takeIf { it.dailyDay == epochDay }?.toSavedGame()

    override fun hasSavedGame(): Flow<Boolean> = dao.hasSavedGame(REGULAR_SLOT)

    override suspend fun save(game: SavedGame) = dao.save(
        SavedGameEntity(
            slot = slotOf(game.dailyDay),
            puzzleId = game.puzzle.id,
            entries = game.entries,
            elapsedSeconds = game.elapsedSeconds,
            notes = game.notes,
            dailyDay = game.dailyDay,
        ),
    )

    override suspend fun deleteSavedGame() = dao.deleteSavedGame(REGULAR_SLOT)

    override suspend fun completeGame(puzzle: Puzzle, elapsedSeconds: Int, dailyDay: Long?) = dao.completeGame(
        CompletedGameEntity(
            puzzleId = puzzle.id,
            difficulty = puzzle.difficulty,
            elapsedSeconds = elapsedSeconds,
            completedAt = now(),
            dailyDay = dailyDay,
        ),
        slotOf(dailyDay),
    )

    // Days come from the time zone in effect now, so a streak follows the player when they travel.
    override fun stats(): Flow<Stats> = dao.completedGames().map { games ->
        val completions = games.map { Completion(it.difficulty, it.elapsedSeconds, epochDay(it.completedAt), it.dailyDay) }
        statsOf(completions, today())
    }

    private fun epochDay(millis: Long): Long = (millis + timeZone().getOffset(millis)).floorDiv(MILLIS_PER_DAY)

    private fun PuzzleEntity.toPuzzle() = Puzzle(id, difficulty, digits)

    private fun SavedGameWithPuzzle.toSavedGame() = SavedGame(puzzle.toPuzzle(), entries, elapsedSeconds, notes, dailyDay)

    private fun slotOf(dailyDay: Long?) = if (dailyDay == null) REGULAR_SLOT else DAILY_SLOT

    companion object {
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
        private const val REGULAR_SLOT = 0
        private const val DAILY_SLOT = 1

        @Volatile
        private var instance: RoomGameRepository? = null

        // One open database for the whole process; ViewModels come and go with each game.
        fun get(context: Context): RoomGameRepository =
            instance ?: synchronized(this) {
                instance ?: RoomGameRepository(SudroidDatabase.build(context.applicationContext).gameDao())
                    .also { instance = it }
            }
    }
}

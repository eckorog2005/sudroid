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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rlamb.android.sudroid.game.Difficulty
import com.rlamb.android.sudroid.game.GameRepository
import com.rlamb.android.sudroid.game.Puzzle
import com.rlamb.android.sudroid.game.SavedGame
import kotlinx.coroutines.launch
import java.util.function.Consumer

class RoomGameRepository(private val dao: GameDao) : GameRepository {
    override suspend fun randomPuzzle(difficulty: Difficulty): Puzzle =
        checkNotNull(dao.randomPuzzle(difficulty)) { "No $difficulty puzzles in the database" }.toPuzzle()

    override suspend fun savedGame(): SavedGame? =
        dao.savedGame()?.let { SavedGame(it.puzzle.toPuzzle(), it.entries, it.elapsedSeconds) }

    override suspend fun save(game: SavedGame) =
        dao.save(SavedGameEntity(puzzleId = game.puzzle.id, entries = game.entries, elapsedSeconds = game.elapsedSeconds))

    override suspend fun deleteSavedGame() = dao.deleteSavedGame()

    /** Reports whether a game can be resumed, while [owner] is started; callable from the Java menu. */
    fun observeHasSavedGame(owner: LifecycleOwner, onChange: Consumer<Boolean>) {
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.hasSavedGame().collect { onChange.accept(it) }
            }
        }
    }

    private fun PuzzleEntity.toPuzzle() = Puzzle(id, difficulty, digits)

    companion object {
        @Volatile
        private var instance: RoomGameRepository? = null

        // One open database for the whole process; ViewModels come and go with each game.
        @JvmStatic
        fun get(context: Context): RoomGameRepository =
            instance ?: synchronized(this) {
                instance ?: RoomGameRepository(SudroidDatabase.build(context.applicationContext).gameDao())
                    .also { instance = it }
            }
    }
}

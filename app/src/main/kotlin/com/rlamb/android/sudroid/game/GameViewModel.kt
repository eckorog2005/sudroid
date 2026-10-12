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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rlamb.android.sudroid.engine.Board
import com.rlamb.android.sudroid.engine.Cell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

data class GameUiState(
    /** Null until the puzzle has loaded. */
    val board: Board? = null,
    val selected: Cell? = null,
    val elapsedSeconds: Int = 0,
) {
    val isSolved: Boolean = board?.isSolved == true
}

/** [saveScope] must outlive the ViewModel, so the last save or delete lands after the screen closes. */
class GameViewModel(
    private val repository: GameRepository,
    request: GameRequest,
    saveScope: CoroutineScope,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private lateinit var puzzle: Puzzle

    // A conflated channel drained by one coroutine keeps writes in order and skips stale ones;
    // null means the game is over and the save should be deleted.
    private val pendingSaves = Channel<SavedGame?>(Channel.CONFLATED)

    init {
        // After process death the screen comes back with its original request; a game that had
        // already started must resume from its autosave instead of starting over.
        val resume = request == GameRequest.Resume || savedState.get<Boolean>(STARTED) == true
        viewModelScope.launch {
            val saved = if (resume) repository.savedGame() else null
            puzzle = saved?.puzzle ?: repository.randomPuzzle((request as? GameRequest.New)?.difficulty ?: Difficulty.EASY)
            val board = Board.fromPuzzle(puzzle.digits).let { if (saved != null) it.withEntries(saved.entries) else it }
            _state.value = GameUiState(board = board, elapsedSeconds = saved?.elapsedSeconds ?: 0)
            savedState[STARTED] = true
        }
        saveScope.launch {
            for (game in pendingSaves) {
                if (game == null) repository.deleteSavedGame() else repository.save(game)
            }
        }
    }

    override fun onCleared() {
        pendingSaves.close()
    }

    fun select(cell: Cell) {
        _state.update { it.copy(selected = cell) }
    }

    fun enter(digit: Int) = moveOnSelected { board, cell -> board.place(cell, digit) }

    fun erase() = moveOnSelected { board, cell -> board.clear(cell) }

    fun reset() = move { it.reset() }

    /** Counts up once a second until the puzzle is solved; run it only while the screen is visible. */
    suspend fun runClock() {
        state.first { it.board != null }
        try {
            while (!state.value.isSolved) {
                delay(1.seconds)
                _state.update { if (it.isSolved) it else it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        } finally {
            save()
        }
    }

    private fun moveOnSelected(change: (Board, Cell) -> Board) {
        val cell = state.value.selected ?: return
        move { change(it, cell) }
    }

    private fun move(change: (Board) -> Board) {
        val current = state.value
        val board = current.board ?: return
        if (current.isSolved) return
        val next = change(board)
        if (next === board) return
        _state.value = current.copy(board = next)
        if (_state.value.isSolved) pendingSaves.trySend(null) else save()
    }

    private fun save() {
        val current = state.value
        val board = current.board ?: return
        if (current.isSolved) return
        pendingSaves.trySend(SavedGame(puzzle, board.entriesString(), current.elapsedSeconds))
    }

    private companion object {
        const val STARTED = "started"
    }
}

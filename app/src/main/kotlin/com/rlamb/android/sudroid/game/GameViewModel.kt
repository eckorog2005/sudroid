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
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    /** When on, the number pad toggles pencil marks instead of placing digits. */
    val notesMode: Boolean = false,
    val mistakeMode: MistakeMode = MistakeMode.ALWAYS,
    /** Set by a check request under [MistakeMode.ON_REQUEST]; the next move, undo or redo clears it. */
    val checking: Boolean = false,
) {
    val isSolved: Boolean = board?.isSolved == true
    val showMistakes: Boolean = mistakeMode == MistakeMode.ALWAYS || (mistakeMode == MistakeMode.ON_REQUEST && checking)
}

/** [saveScope] must outlive the ViewModel, so the last save or delete lands after the screen closes. */
class GameViewModel(
    private val repository: GameRepository,
    request: GameRequest,
    saveScope: CoroutineScope,
    private val savedState: SavedStateHandle,
    mistakeMode: MistakeMode = MistakeMode.ALWAYS,
) : ViewModel() {
    private val _state = MutableStateFlow(GameUiState(mistakeMode = mistakeMode))
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private lateinit var puzzle: Puzzle
    private val undoStack = ArrayDeque<Board>()
    private val redoStack = ArrayDeque<Board>()

    // A conflated channel drained by one coroutine keeps writes in order and skips stale ones.
    private val pendingWrites = Channel<PendingWrite>(Channel.CONFLATED)

    init {
        // After process death the screen comes back with its original request; a game that had
        // already started must resume from its autosave instead of starting over.
        val resume = request == GameRequest.Resume || savedState.get<Boolean>(STARTED) == true
        viewModelScope.launch {
            val saved = if (resume) repository.savedGame() else null
            puzzle = saved?.puzzle ?: repository.randomPuzzle((request as? GameRequest.New)?.difficulty ?: Difficulty.EASY)
            val board = Board.fromPuzzle(puzzle.digits).let {
                if (saved != null) it.withEntries(saved.entries).withNotes(saved.notes) else it
            }
            _state.update { it.copy(board = board, elapsedSeconds = saved?.elapsedSeconds ?: 0) }
            savedState[STARTED] = true
        }
        saveScope.launch {
            for (write in pendingWrites) {
                when (write) {
                    is PendingWrite.Save -> repository.save(write.game)
                    is PendingWrite.Complete -> repository.completeGame(write.puzzle, write.elapsedSeconds)
                }
            }
        }
    }

    override fun onCleared() {
        pendingWrites.close()
    }

    fun select(cell: Cell) {
        _state.update { it.copy(selected = cell) }
    }

    fun enter(digit: Int) = moveOnSelected { board, cell ->
        if (state.value.notesMode) board.toggleNote(cell, digit) else board.place(cell, digit)
    }

    fun toggleNotesMode() {
        _state.update { it.copy(notesMode = !it.notesMode) }
    }

    fun checkMistakes() {
        _state.update { it.copy(checking = true) }
    }

    fun erase() = moveOnSelected { board, cell -> board.clear(cell) }

    fun reset() = move { it.reset() }

    fun undo() = step(from = undoStack, to = redoStack)

    fun redo() = step(from = redoStack, to = undoStack)

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
        undoStack.addLast(board)
        redoStack.clear()
        show(next)
    }

    private fun step(from: ArrayDeque<Board>, to: ArrayDeque<Board>) {
        val board = state.value.board ?: return
        if (state.value.isSolved || from.isEmpty()) return
        to.addLast(board)
        show(from.removeLast())
    }

    private fun show(board: Board) {
        _state.update {
            it.copy(board = board, canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty(), checking = false)
        }
        val current = _state.value
        if (current.isSolved) pendingWrites.trySend(PendingWrite.Complete(puzzle, current.elapsedSeconds)) else save()
    }

    private fun save() {
        val current = state.value
        val board = current.board ?: return
        if (current.isSolved) return
        pendingWrites.trySend(PendingWrite.Save(SavedGame(puzzle, board.entriesString(), current.elapsedSeconds, board.notesString())))
    }

    private sealed interface PendingWrite {
        data class Save(val game: SavedGame) : PendingWrite
        data class Complete(val puzzle: Puzzle, val elapsedSeconds: Int) : PendingWrite
    }

    private companion object {
        const val STARTED = "started"
    }
}

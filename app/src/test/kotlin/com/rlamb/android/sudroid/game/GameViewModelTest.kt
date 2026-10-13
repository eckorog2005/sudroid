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

import com.rlamb.android.sudroid.engine.Board
import com.rlamb.android.sudroid.engine.Cell
import com.rlamb.android.sudroid.engine.Solver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val repository = FakeGameRepository(PUZZLE)
    private lateinit var saveScope: CoroutineScope

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        saveScope = CoroutineScope(Dispatchers.Main)
    }

    @AfterTest
    fun tearDown() {
        saveScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `a new game starts the requested difficulty with no entries and no time`() = runTest {
        val state = newGame().state.value
        val board = assertNotNull(state.board)

        assertEquals(listOf(Difficulty.EASY), repository.requestedDifficulties)
        assertTrue(board.isGiven(Cell(0, 1)))
        assertEquals(EMPTY_ENTRIES, board.entriesString())
        assertEquals(0, state.elapsedSeconds)
    }

    @Test
    fun `resuming restores the saved entries and time`() = runTest {
        val entries = EMPTY_ENTRIES.replaceRange(0, 1, "4")
        repository.saved = SavedGame(PUZZLE, entries, elapsedSeconds = 75)

        val state = GameViewModel(repository, GameRequest.Resume, saveScope, SavedStateHandle()).state.value

        assertEquals(entries, assertNotNull(state.board).entriesString())
        assertEquals(75, state.elapsedSeconds)
    }

    @Test
    fun `resuming without a saved game starts a new easy game`() = runTest {
        val state = GameViewModel(repository, GameRequest.Resume, saveScope, SavedStateHandle()).state.value

        assertEquals(EMPTY_ENTRIES, assertNotNull(state.board).entriesString())
        assertEquals(listOf(Difficulty.EASY), repository.requestedDifficulties)
    }

    @Test
    fun `a new game restored after process death resumes its autosave`() = runTest {
        val savedState = SavedStateHandle()
        val first = GameViewModel(repository, GameRequest.New(Difficulty.HARD), saveScope, savedState)
        first.select(Cell(0, 0))
        first.enter(4)

        val restored = GameViewModel(repository, GameRequest.New(Difficulty.HARD), saveScope, savedState)

        assertEquals("4" + EMPTY_ENTRIES.drop(1), restored.state.value.board?.entriesString())
        assertEquals(listOf(Difficulty.HARD), repository.requestedDifficulties)
    }

    @Test
    fun `entering a digit fills the selected cell and saves the game`() = runTest {
        val viewModel = newGame()

        viewModel.select(Cell(0, 0))
        viewModel.enter(4)

        assertEquals(4, viewModel.state.value.board?.digitAt(Cell(0, 0)))
        assertEquals(Cell(0, 0), viewModel.state.value.selected)
        assertEquals("4" + EMPTY_ENTRIES.drop(1), repository.saved?.entries)
        assertEquals(PUZZLE, repository.saved?.puzzle)
    }

    @Test
    fun `entering a digit without a selection does nothing`() = runTest {
        val viewModel = newGame()

        viewModel.enter(4)

        assertEquals(EMPTY_ENTRIES, viewModel.state.value.board?.entriesString())
        assertNull(repository.saved)
    }

    @Test
    fun `erasing clears the selected cell and saves the game`() = runTest {
        val viewModel = newGame()
        viewModel.select(Cell(0, 0))
        viewModel.enter(4)

        viewModel.erase()

        assertEquals(0, viewModel.state.value.board?.digitAt(Cell(0, 0)))
        assertEquals(EMPTY_ENTRIES, repository.saved?.entries)
    }

    @Test
    fun `reset removes every entry and saves the game`() = runTest {
        val viewModel = newGame()
        viewModel.select(Cell(0, 0))
        viewModel.enter(4)
        viewModel.select(Cell(0, 4))
        viewModel.enter(5)

        viewModel.reset()

        assertEquals(EMPTY_ENTRIES, viewModel.state.value.board?.entriesString())
        assertEquals(EMPTY_ENTRIES, repository.saved?.entries)
    }

    @Test
    fun `in notes mode the number pad toggles pencil marks and saves them`() = runTest {
        val viewModel = newGame()
        viewModel.select(Cell(0, 0))

        viewModel.toggleNotesMode()
        viewModel.enter(4)
        viewModel.enter(7)
        viewModel.enter(4)

        val board = assertNotNull(viewModel.state.value.board)
        assertEquals(setOf(7), board.notesAt(Cell(0, 0)))
        assertEquals(0, board.digitAt(Cell(0, 0)))
        assertEquals(board.notesString(), repository.saved?.notes)

        viewModel.toggleNotesMode()
        viewModel.enter(4)
        assertEquals(4, viewModel.state.value.board?.digitAt(Cell(0, 0)))
        assertEquals(emptySet(), viewModel.state.value.board?.notesAt(Cell(0, 0)))
    }

    @Test
    fun `resuming restores the saved notes`() = runTest {
        val notes = Board.fromPuzzle(PUZZLE.digits).toggleNote(Cell(0, 0), 3).notesString()
        repository.saved = SavedGame(PUZZLE, EMPTY_ENTRIES, elapsedSeconds = 5, notes = notes)

        val state = GameViewModel(repository, GameRequest.Resume, saveScope, SavedStateHandle()).state.value

        assertEquals(setOf(3), assertNotNull(state.board).notesAt(Cell(0, 0)))
    }

    @Test
    fun `undo brings back notes a placed digit cleared`() = runTest {
        val viewModel = newGame()
        viewModel.select(Cell(0, 0))
        viewModel.toggleNotesMode()
        viewModel.enter(4)
        viewModel.select(Cell(0, 4))
        viewModel.toggleNotesMode()
        viewModel.enter(4)

        viewModel.undo()

        assertEquals(setOf(4), viewModel.state.value.board?.notesAt(Cell(0, 0)))
    }

    @Test
    fun `undo restores the board before the last move and saves it`() = runTest {
        val viewModel = newGame()
        viewModel.select(Cell(0, 0))
        viewModel.enter(4)
        viewModel.enter(7)

        viewModel.undo()

        assertEquals(4, viewModel.state.value.board?.digitAt(Cell(0, 0)))
        assertEquals("4" + EMPTY_ENTRIES.drop(1), repository.saved?.entries)
        assertTrue(viewModel.state.value.canUndo)
        assertTrue(viewModel.state.value.canRedo)
    }

    @Test
    fun `undo brings back the entries a reset removed`() = runTest {
        val viewModel = newGame()
        viewModel.select(Cell(0, 0))
        viewModel.enter(4)
        viewModel.reset()

        viewModel.undo()

        assertEquals(4, viewModel.state.value.board?.digitAt(Cell(0, 0)))
    }

    @Test
    fun `redo replays undone moves until a new move discards them`() = runTest {
        val viewModel = newGame()
        viewModel.select(Cell(0, 0))
        viewModel.enter(4)
        viewModel.enter(7)
        viewModel.undo()
        viewModel.undo()

        viewModel.redo()
        assertEquals(4, viewModel.state.value.board?.digitAt(Cell(0, 0)))

        viewModel.enter(9)
        viewModel.redo()
        assertEquals(9, viewModel.state.value.board?.digitAt(Cell(0, 0)))
        assertFalse(viewModel.state.value.canRedo)
    }

    @Test
    fun `undo and redo do nothing without history`() = runTest {
        val viewModel = newGame()
        viewModel.select(Cell(0, 0))
        viewModel.enter(4)
        viewModel.enter(4)

        viewModel.redo()
        viewModel.undo()
        viewModel.undo()

        assertEquals(EMPTY_ENTRIES, viewModel.state.value.board?.entriesString())
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `the clock counts seconds while it runs and saves the time when it stops`() = runTest {
        val viewModel = newGame()

        val clock = launch { viewModel.runClock() }
        advanceTimeBy(3_500)
        clock.cancel()
        advanceTimeBy(5_000)

        assertEquals(3, viewModel.state.value.elapsedSeconds)
        assertEquals(3, repository.saved?.elapsedSeconds)
    }

    @Test
    fun `the clock resumes from the saved time`() = runTest {
        repository.saved = SavedGame(PUZZLE, EMPTY_ENTRIES, elapsedSeconds = 75)
        val viewModel = GameViewModel(repository, GameRequest.Resume, saveScope, SavedStateHandle())

        val clock = launch { viewModel.runClock() }
        advanceTimeBy(2_500)
        clock.cancel()

        assertEquals(77, viewModel.state.value.elapsedSeconds)
    }

    @Test
    fun `solving the puzzle stops the clock and deletes the saved game`() = runTest {
        val (viewModel, lastCell) = oneMoveFromSolved()
        val clock = launch { viewModel.runClock() }

        viewModel.select(lastCell)
        viewModel.enter(SOLUTION.digitAt(lastCell))
        advanceTimeBy(5_000)

        assertTrue(viewModel.state.value.isSolved)
        assertEquals(10, viewModel.state.value.elapsedSeconds)
        assertFalse(clock.isActive)
        assertNull(repository.saved)
    }

    @Test
    fun `a solved board ignores further input`() = runTest {
        val (viewModel, lastCell) = oneMoveFromSolved()
        viewModel.select(lastCell)
        viewModel.enter(SOLUTION.digitAt(lastCell))

        viewModel.erase()
        viewModel.reset()
        viewModel.undo()

        assertTrue(viewModel.state.value.isSolved)
        assertNull(repository.saved)
    }

    @Test
    fun `a save still lands after the screen closes`() {
        // Queued work, so the save is still pending when the ViewModel is cleared.
        Dispatchers.setMain(StandardTestDispatcher())
        saveScope = CoroutineScope(Dispatchers.Main)
        runTest { saveAndCloseScreen() }
    }

    private suspend fun TestScope.saveAndCloseScreen() {
        val store = ViewModelStore()
        val viewModel = ViewModelProvider.create(
            store,
            viewModelFactory { initializer { newGame() } },
        )[GameViewModel::class]
        advanceUntilIdle()
        viewModel.select(Cell(0, 0))
        viewModel.enter(4)

        store.clear()
        advanceUntilIdle()

        assertEquals("4" + EMPTY_ENTRIES.drop(1), repository.saved?.entries)
    }

    private fun newGame() = GameViewModel(repository, GameRequest.New(Difficulty.EASY), saveScope, SavedStateHandle())

    private fun oneMoveFromSolved(): Pair<GameViewModel, Cell> {
        val lastCell = Cell.ALL.first { !SOLUTION.isGiven(it) }
        repository.saved = SavedGame(PUZZLE, SOLUTION.clear(lastCell).entriesString(), elapsedSeconds = 10)
        return GameViewModel(repository, GameRequest.Resume, saveScope, SavedStateHandle()) to lastCell
    }

    private companion object {
        val PUZZLE = Puzzle(
            id = 1,
            difficulty = Difficulty.EASY,
            digits = "012604830700800052806003000280065000030000020000380046000900105950008007068507290",
        )
        val SOLUTION: Board = checkNotNull(Solver.solve(Board.fromPuzzle(PUZZLE.digits)))
        val EMPTY_ENTRIES = "0".repeat(81)
    }
}

private class FakeGameRepository(private val puzzle: Puzzle) : GameRepository {
    var saved: SavedGame? = null
    val requestedDifficulties = mutableListOf<Difficulty>()

    override suspend fun randomPuzzle(difficulty: Difficulty): Puzzle {
        requestedDifficulties += difficulty
        return puzzle
    }

    override suspend fun savedGame(): SavedGame? = saved

    override fun hasSavedGame(): Flow<Boolean> = flowOf(saved != null)

    override suspend fun save(game: SavedGame) {
        saved = game
    }

    override suspend fun deleteSavedGame() {
        saved = null
    }
}

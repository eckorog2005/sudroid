package com.rlamb.android.sudroid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BoardTest {
    private val board = Board.fromPuzzle(PUZZLE)

    @Test
    fun `givens are read in row-major order`() {
        assertEquals(5, board.digitAt(Cell(0, 0)))
        assertEquals(3, board.digitAt(Cell(0, 1)))
        assertEquals(6, board.digitAt(Cell(1, 0)))
        assertEquals(9, board.digitAt(Cell(8, 8)))
        assertTrue(board.isGiven(Cell(0, 0)))
        assertFalse(board.isGiven(Cell(0, 2)))
        assertEquals(0, board.digitAt(Cell(0, 2)))
    }

    @Test
    fun `fromPuzzle rejects malformed input`() {
        assertFailsWith<IllegalArgumentException> { Board.fromPuzzle(PUZZLE.dropLast(1)) }
        assertFailsWith<IllegalArgumentException> { Board.fromPuzzle(PUZZLE.replaceFirst('0', '.')) }
    }

    @Test
    fun `cells outside the grid are rejected`() {
        assertFailsWith<IllegalArgumentException> { Cell(-1, 0) }
        assertFailsWith<IllegalArgumentException> { Cell(0, 9) }
    }

    @Test
    fun `place fills an empty cell without changing the original board`() {
        val placed = board.place(Cell(0, 2), 4)

        assertEquals(4, placed.digitAt(Cell(0, 2)))
        assertFalse(placed.isGiven(Cell(0, 2)))
        assertEquals(0, board.digitAt(Cell(0, 2)))
    }

    @Test
    fun `place overwrites an earlier entry`() {
        assertEquals(1, board.place(Cell(0, 2), 4).place(Cell(0, 2), 1).digitAt(Cell(0, 2)))
    }

    @Test
    fun `givens cannot be changed`() {
        assertSame(board, board.place(Cell(0, 0), 1))
        assertSame(board, board.clear(Cell(0, 0)))
    }

    @Test
    fun `place rejects digits outside 1 to 9`() {
        assertFailsWith<IllegalArgumentException> { board.place(Cell(0, 2), 0) }
        assertFailsWith<IllegalArgumentException> { board.place(Cell(0, 2), 10) }
    }

    @Test
    fun `clear empties an entry`() {
        assertEquals(0, board.place(Cell(0, 2), 4).clear(Cell(0, 2)).digitAt(Cell(0, 2)))
    }

    @Test
    fun `duplicate digits conflict across a row, column, and box`() {
        val rowClash = board.place(Cell(0, 2), 5)
        assertTrue(rowClash.hasConflict(Cell(0, 2)))
        assertTrue(rowClash.hasConflict(Cell(0, 0)))

        val colClash = board.place(Cell(2, 0), 6)
        assertTrue(colClash.hasConflict(Cell(2, 0)))
        assertTrue(colClash.hasConflict(Cell(1, 0)))

        val boxClash = board.place(Cell(1, 1), 8)
        assertTrue(boxClash.hasConflict(Cell(1, 1)))
        assertTrue(boxClash.hasConflict(Cell(2, 2)))
    }

    @Test
    fun `cells without duplicates do not conflict`() {
        val placed = board.place(Cell(0, 2), 4)

        assertFalse(placed.hasConflict(Cell(0, 2)))
        assertFalse(placed.hasConflict(Cell(0, 0)))
        assertFalse(placed.hasConflict(Cell(4, 4)))
    }

    @Test
    fun `clearing a clashing entry resolves the conflict`() {
        val cleared = board.place(Cell(0, 2), 5).clear(Cell(0, 2))

        assertFalse(cleared.hasConflict(Cell(0, 0)))
    }

    @Test
    fun `replacing one clashing digit with another releases the first clash`() {
        val board = board
            .place(Cell(0, 2), 4)
            .place(Cell(0, 3), 4)
            .place(Cell(0, 3), 3)

        assertFalse(board.hasConflict(Cell(0, 2)))
        assertTrue(board.hasConflict(Cell(0, 3)))
        assertTrue(board.hasConflict(Cell(0, 1)))
    }

    @Test
    fun `board is solved only when full and conflict free`() {
        assertFalse(board.isSolved)
        assertTrue(board.withEntries(SOLUTION).isSolved)
        assertFalse(board.withEntries(SOLUTION).clear(Cell(0, 2)).isSolved)

        val wrong = board.withEntries(SOLUTION).place(Cell(0, 2), 6).place(Cell(0, 3), 4)
        assertFalse(wrong.isSolved)
    }

    @Test
    fun `reset clears entries but keeps givens`() {
        val reset = board.withEntries(SOLUTION).reset()

        assertEquals(board.entriesString(), reset.entriesString())
        assertEquals(5, reset.digitAt(Cell(0, 0)))
    }

    @Test
    fun `entries round trip through entriesString`() {
        val played = board.place(Cell(0, 2), 4).place(Cell(8, 0), 3)
        val entries = played.entriesString()

        assertEquals(81, entries.length)
        assertEquals('0', entries[0])
        assertEquals('4', entries[2])
        assertEquals('3', entries[72])
        assertEquals(entries, board.withEntries(entries).entriesString())
    }

    @Test
    fun `restored entries never override givens`() {
        val restored = board.withEntries(SOLUTION)

        assertTrue(restored.isGiven(Cell(0, 0)))
        assertEquals('0', restored.entriesString()[0])
        assertEquals('4', restored.entriesString()[2])
    }

    companion object {
        const val PUZZLE = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        const val SOLUTION = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    }
}

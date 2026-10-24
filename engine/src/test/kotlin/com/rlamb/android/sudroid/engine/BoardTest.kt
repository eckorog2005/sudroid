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
    fun `remaining counts givens and entries and stops at zero`() {
        assertEquals(6, board.remaining(5))
        assertEquals(5, board.place(Cell(0, 2), 5).remaining(5))

        val full = Cell.ALL.fold(Board.EMPTY) { b, cell -> b.place(cell, 3) }
        assertEquals(0, full.remaining(3))
        assertEquals(9, full.remaining(4))
        assertFailsWith<IllegalArgumentException> { board.remaining(0) }
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

    @Test
    fun `toggleNote adds and removes a pencil mark on an empty cell`() {
        val noted = board.toggleNote(Cell(0, 2), 4).toggleNote(Cell(0, 2), 1)

        assertEquals(setOf(1, 4), noted.notesAt(Cell(0, 2)))
        assertEquals(0, noted.digitAt(Cell(0, 2)))
        assertEquals(setOf(1), noted.toggleNote(Cell(0, 2), 4).notesAt(Cell(0, 2)))
        assertEquals(emptySet(), board.notesAt(Cell(0, 2)))
    }

    @Test
    fun `cells holding a digit take no notes`() {
        assertSame(board, board.toggleNote(Cell(0, 0), 1))
        val placed = board.place(Cell(0, 2), 4)
        assertSame(placed, placed.toggleNote(Cell(0, 2), 1))
        assertFailsWith<IllegalArgumentException> { board.toggleNote(Cell(0, 2), 0) }
    }

    @Test
    fun `placing a digit clears its cell's notes and that digit from its peers`() {
        val noted = board
            .toggleNote(Cell(0, 2), 1).toggleNote(Cell(0, 2), 4)
            .toggleNote(Cell(0, 3), 4).toggleNote(Cell(0, 3), 2)
            .toggleNote(Cell(4, 2), 4)
            .toggleNote(Cell(1, 1), 4)
            .toggleNote(Cell(8, 0), 4)

        val placed = noted.place(Cell(0, 2), 4)

        assertEquals(emptySet(), placed.notesAt(Cell(0, 2)))
        assertEquals(setOf(2), placed.notesAt(Cell(0, 3)))
        assertEquals(emptySet(), placed.notesAt(Cell(4, 2)))
        assertEquals(emptySet(), placed.notesAt(Cell(1, 1)))
        assertEquals(setOf(4), placed.notesAt(Cell(8, 0)))
    }

    @Test
    fun `clearing an entry leaves notes alone and reset removes them`() {
        val noted = board.toggleNote(Cell(0, 3), 2).place(Cell(0, 2), 4)

        assertEquals(setOf(2), noted.clear(Cell(0, 2)).notesAt(Cell(0, 3)))
        assertSame(board, board.clear(Cell(0, 2)))
        assertEquals(emptySet(), noted.reset().notesAt(Cell(0, 3)))
    }

    @Test
    fun `notes round trip through notesString`() {
        val noted = board.toggleNote(Cell(0, 2), 9).toggleNote(Cell(0, 2), 1).toggleNote(Cell(8, 0), 3)
        val notes = noted.notesString()

        assertEquals("19", notes.split(',')[2])
        assertEquals("3", notes.split(',')[72])
        assertEquals(notes, board.withNotes(notes).notesString())
        assertEquals(",".repeat(80), board.notesString())
        assertEquals(board.notesString(), noted.withNotes("").notesString())
    }

    @Test
    fun `restored notes skip filled cells and reject malformed input`() {
        val notes = Array(81) { "" }.also { it[0] = "1"; it[2] = "4" }.joinToString(",")

        val restored = board.place(Cell(0, 2), 7).withNotes(notes)

        assertEquals(emptySet(), restored.notesAt(Cell(0, 0)))
        assertEquals(emptySet(), restored.notesAt(Cell(0, 2)))
        assertFailsWith<IllegalArgumentException> { board.withNotes(",,") }
        assertFailsWith<IllegalArgumentException> { board.withNotes(notes.replaceFirst("4", "0")) }
    }

    @Test
    fun `a house is complete once it is full with no repeated digit`() {
        val firstRow = Cell.HOUSES.first()
        val nearly = Board.fromPuzzle(PUZZLE).withEntries(SOLUTION.take(8) + "0".repeat(73))

        assertFalse(nearly.isComplete(firstRow))
        assertTrue(nearly.place(Cell(0, 8), 2).isComplete(firstRow))
        assertFalse(nearly.place(Cell(0, 8), 1).isComplete(firstRow))
    }

    companion object {
        const val PUZZLE = "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
        const val SOLUTION = "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    }
}

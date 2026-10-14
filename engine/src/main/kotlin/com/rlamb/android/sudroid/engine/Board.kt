/*
 *  Copyright 2010,2012,2026 Roger Lamb
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

package com.rlamb.android.sudroid.engine

/**
 * An immutable Sudoku grid: the puzzle's givens plus the player's entries and pencil marks.
 *
 * Conflicting entries are allowed and reported through [hasConflict], so the player can see
 * their mistake rather than having the move rejected.
 */
class Board private constructor(
    private val givens: IntArray,
    private val entries: IntArray,
    /** A bitmask per cell, with bit `d` set when `d` is noted. */
    private val notes: IntArray,
) {
    fun digitAt(cell: Cell): Int = givens[cell.index].takeIf { it != BLANK } ?: entries[cell.index]

    fun isGiven(cell: Cell): Boolean = givens[cell.index] != BLANK

    fun notesAt(cell: Cell): Set<Int> = (1..SIZE).filterTo(mutableSetOf()) { notes[cell.index] and (1 shl it) != 0 }

    /** How many more of [digit] the grid takes; conflicting entries count, so it never goes below 0. */
    fun remaining(digit: Int): Int {
        requireDigit(digit)
        return (SIZE - Cell.ALL.count { digitAt(it) == digit }).coerceAtLeast(0)
    }

    fun hasConflict(cell: Cell): Boolean {
        val digit = digitAt(cell)
        return digit != BLANK && cell.peers.any { digitAt(it) == digit }
    }

    val isSolved: Boolean
        get() = Cell.ALL.all { digitAt(it) != BLANK && !hasConflict(it) }

    /**
     * Fills [cell], clears its notes, and removes [digit] from the notes of its row, column and box.
     * Returns this board unchanged if [cell] is a given or already holds [digit].
     */
    fun place(cell: Cell, digit: Int): Board {
        requireDigit(digit)
        if (isGiven(cell) || entries[cell.index] == digit) return this
        val newNotes = notes.copyOf()
        newNotes[cell.index] = 0
        for (peer in cell.peers) newNotes[peer.index] = newNotes[peer.index] and (1 shl digit).inv()
        return Board(givens, entries.copyOf().also { it[cell.index] = digit }, newNotes)
    }

    /** Empties [cell]'s entry, leaving notes alone. Returns this board unchanged if there is no entry. */
    fun clear(cell: Cell): Board {
        if (entries[cell.index] == BLANK) return this
        return Board(givens, entries.copyOf().also { it[cell.index] = BLANK }, notes)
    }

    /** Returns this board unchanged unless [cell] is empty. */
    fun toggleNote(cell: Cell, digit: Int): Board {
        requireDigit(digit)
        if (digitAt(cell) != BLANK) return this
        return Board(givens, entries, notes.copyOf().also { it[cell.index] = it[cell.index] xor (1 shl digit) })
    }

    fun reset(): Board = Board(givens, IntArray(CELL_COUNT), IntArray(CELL_COUNT))

    /** Restores entries saved with [entriesString]; digits at given cells are ignored. */
    fun withEntries(entries: String): Board {
        val parsed = parseDigits(entries)
        return Board(givens, IntArray(CELL_COUNT) { if (givens[it] != BLANK) BLANK else parsed[it] }, notes)
    }

    /** The player's entries as 81 digits in row-major order, with `0` for givens and empty cells. */
    fun entriesString(): String = entries.joinToString("")

    /**
     * Restores notes saved with [notesString]; an empty string means no notes. Notes on cells that
     * hold a digit are ignored.
     */
    fun withNotes(notes: String): Board {
        if (notes.isEmpty()) return Board(givens, entries, IntArray(CELL_COUNT))
        val cells = notes.split(',')
        require(cells.size == CELL_COUNT) { "Expected $CELL_COUNT cells of notes, got ${cells.size}" }
        return Board(
            givens,
            entries,
            IntArray(CELL_COUNT) { i ->
                cells[i].fold(0) { mask, char ->
                    require(char in '1'..'9') { "'$char' in cell $i is not a digit from 1 to 9" }
                    mask or (1 shl (char - '0'))
                }.takeIf { digitAt(Cell.ALL[i]) == BLANK } ?: 0
            },
        )
    }

    /** The notes as 81 comma-separated groups in row-major order, each listing its digits in order. */
    fun notesString(): String = Cell.ALL.joinToString(",") { notesAt(it).sorted().joinToString("") }

    internal fun digits(): IntArray = IntArray(CELL_COUNT) { digitAt(Cell.ALL[it]) }

    internal fun withDigits(digits: IntArray): Board =
        Board(givens, IntArray(CELL_COUNT) { if (givens[it] != BLANK) BLANK else digits[it] }, IntArray(CELL_COUNT))

    private fun requireDigit(digit: Int) = require(digit in 1..SIZE) { "Digit must be in 1..$SIZE, was $digit" }

    companion object {
        const val SIZE = 9
        internal const val CELL_COUNT = SIZE * SIZE
        private const val BLANK = 0

        @JvmField
        val EMPTY: Board = Board(IntArray(CELL_COUNT), IntArray(CELL_COUNT), IntArray(CELL_COUNT))

        /** Parses 81 digits in row-major order, with `0` for empty cells. */
        @JvmStatic
        fun fromPuzzle(puzzle: String): Board = Board(parseDigits(puzzle), IntArray(CELL_COUNT), IntArray(CELL_COUNT))

        private fun parseDigits(text: String): IntArray {
            require(text.length == CELL_COUNT) { "Expected $CELL_COUNT digits, got ${text.length}" }
            return IntArray(CELL_COUNT) { i ->
                val char = text[i]
                require(char in '0'..'9') { "'$char' at index $i is not a digit" }
                char - '0'
            }
        }
    }
}

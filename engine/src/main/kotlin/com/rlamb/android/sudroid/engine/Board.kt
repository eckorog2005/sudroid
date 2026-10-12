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
 * An immutable Sudoku grid: the puzzle's givens plus the player's entries.
 *
 * Conflicting entries are allowed and reported through [hasConflict], so the player can see
 * their mistake rather than having the move rejected.
 */
class Board private constructor(
    private val givens: IntArray,
    private val entries: IntArray,
) {
    fun digitAt(cell: Cell): Int = givens[cell.index].takeIf { it != BLANK } ?: entries[cell.index]

    fun isGiven(cell: Cell): Boolean = givens[cell.index] != BLANK

    fun hasConflict(cell: Cell): Boolean {
        val digit = digitAt(cell)
        return digit != BLANK && cell.peers.any { digitAt(it) == digit }
    }

    val isSolved: Boolean
        get() = Cell.ALL.all { digitAt(it) != BLANK && !hasConflict(it) }

    /** Returns this board unchanged if [cell] is a given. */
    fun place(cell: Cell, digit: Int): Board {
        require(digit in 1..SIZE) { "Digit must be in 1..$SIZE, was $digit" }
        return withEntry(cell, digit)
    }

    /** Returns this board unchanged if [cell] is a given. */
    fun clear(cell: Cell): Board = withEntry(cell, BLANK)

    fun reset(): Board = Board(givens, IntArray(CELL_COUNT))

    /** Restores entries saved with [entriesString]; digits at given cells are ignored. */
    fun withEntries(entries: String): Board {
        val parsed = parseDigits(entries)
        return Board(givens, IntArray(CELL_COUNT) { if (givens[it] != BLANK) BLANK else parsed[it] })
    }

    /** The player's entries as 81 digits in row-major order, with `0` for givens and empty cells. */
    fun entriesString(): String = entries.joinToString("")

    internal fun digits(): IntArray = IntArray(CELL_COUNT) { digitAt(Cell.ALL[it]) }

    internal fun withDigits(digits: IntArray): Board =
        Board(givens, IntArray(CELL_COUNT) { if (givens[it] != BLANK) BLANK else digits[it] })

    private fun withEntry(cell: Cell, digit: Int): Board {
        if (isGiven(cell) || entries[cell.index] == digit) return this
        return Board(givens, entries.copyOf().also { it[cell.index] = digit })
    }

    companion object {
        const val SIZE = 9
        internal const val CELL_COUNT = SIZE * SIZE
        private const val BLANK = 0

        @JvmField
        val EMPTY: Board = Board(IntArray(CELL_COUNT), IntArray(CELL_COUNT))

        /** Parses 81 digits in row-major order, with `0` for empty cells. */
        @JvmStatic
        fun fromPuzzle(puzzle: String): Board = Board(parseDigits(puzzle), IntArray(CELL_COUNT))

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

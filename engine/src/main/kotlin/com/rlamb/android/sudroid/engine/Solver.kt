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

object Solver {
    /** Fills every empty cell, or returns null if the board has no solution. */
    @JvmStatic
    fun solve(board: Board): Board? {
        var solution: IntArray? = null
        Search(board.digits(), limit = 1) { solution = it.copyOf() }.run()
        return solution?.let(board::withDigits)
    }

    /** Counts solutions, stopping once [limit] is reached. */
    @JvmStatic
    @JvmOverloads
    fun countSolutions(board: Board, limit: Int = 2): Int {
        require(limit >= 1) { "Limit must be at least 1, was $limit" }
        return Search(board.digits(), limit) {}.run()
    }

    @JvmStatic
    fun hasUniqueSolution(board: Board): Boolean = countSolutions(board, limit = 2) == 1

    /**
     * The solution's digit for one cell and why it goes there, checked against the givens alone so
     * wrong entries can be corrected. See [Hint] for which cell is chosen. Null when nothing is left
     * to reveal or the givens have no solution.
     */
    @JvmStatic
    @JvmOverloads
    fun hint(board: Board, preferred: Cell? = null): Hint? = solve(board.reset())?.let { findHint(board, it, preferred) }
}

/** Backtracking over bitmasks of used digits, always branching on the most constrained cell. */
private class Search(
    private val digits: IntArray,
    private val limit: Int,
    private val onSolution: (IntArray) -> Unit,
) {
    private val rows = IntArray(Board.SIZE)
    private val cols = IntArray(Board.SIZE)
    private val boxes = IntArray(Board.SIZE)
    private var found = 0

    fun run(): Int {
        for (i in digits.indices) {
            val digit = digits[i]
            if (digit == 0) continue
            val bit = 1 shl digit
            if (used(i) and bit != 0) return 0
            toggle(i, bit)
        }
        descend()
        return found
    }

    private fun descend() {
        var best = -1
        var bestOptions = 0
        var bestCount = Int.MAX_VALUE
        for (i in digits.indices) {
            if (digits[i] != 0) continue
            val options = ALL_DIGITS and used(i).inv()
            val count = Integer.bitCount(options)
            if (count == 0) return
            if (count < bestCount) {
                best = i
                bestOptions = options
                bestCount = count
            }
        }
        if (best == -1) {
            found++
            onSolution(digits)
            return
        }

        var options = bestOptions
        while (options != 0 && found < limit) {
            val bit = options and -options
            options = options xor bit
            digits[best] = Integer.numberOfTrailingZeros(bit)
            toggle(best, bit)
            descend()
            toggle(best, bit)
            digits[best] = 0
        }
    }

    private fun used(i: Int): Int = rows[row(i)] or cols[col(i)] or boxes[box(i)]

    private fun toggle(i: Int, bit: Int) {
        rows[row(i)] = rows[row(i)] xor bit
        cols[col(i)] = cols[col(i)] xor bit
        boxes[box(i)] = boxes[box(i)] xor bit
    }

    private fun row(i: Int) = i / Board.SIZE
    private fun col(i: Int) = i % Board.SIZE
    private fun box(i: Int) = (row(i) / Cell.BOX_SIZE) * Cell.BOX_SIZE + col(i) / Cell.BOX_SIZE

    private companion object {
        const val ALL_DIGITS = 0b11_1111_1110
    }
}

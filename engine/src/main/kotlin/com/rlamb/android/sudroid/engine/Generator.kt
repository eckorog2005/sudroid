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

package com.rlamb.android.sudroid.engine

import kotlin.random.Random

object Generator {
    /**
     * A puzzle with a unique solution and [clues] givens that [Grader] grades as exactly [technique].
     * Keeps trying fresh grids until one fits, so a combination no grid can meet never returns.
     */
    @JvmStatic
    fun generate(technique: Technique, clues: Int, random: Random): Board {
        require(clues in MIN_CLUES..Board.CELL_COUNT) { "Clues must be in $MIN_CLUES..${Board.CELL_COUNT}, was $clues" }
        while (true) {
            val puzzle = dig(fullGrid(random), technique, clues, random)
            if (puzzle != null) return puzzle
        }
    }

    /** Removes givens in random order, keeping each removal only if the puzzle stays unique and no harder than [technique]. */
    private fun dig(grid: IntArray, technique: Technique, clues: Int, random: Random): Board? {
        val digits = grid.copyOf()
        var remaining = Board.CELL_COUNT
        for (i in digits.indices.shuffled(random)) {
            if (remaining == clues) break
            val digit = digits[i]
            digits[i] = 0
            val board = puzzleOf(digits)
            val grade = Grader.grade(board)
            if (grade != null && grade <= technique && Solver.hasUniqueSolution(board)) remaining-- else digits[i] = digit
        }
        val puzzle = puzzleOf(digits)
        return puzzle.takeIf { remaining == clues && Grader.grade(it) == technique }
    }

    private fun fullGrid(random: Random): IntArray {
        val digits = IntArray(Board.CELL_COUNT)
        fun fill(i: Int): Boolean {
            if (i == Board.CELL_COUNT) return true
            val cell = Cell.ALL[i]
            val used = cell.peers.mapTo(mutableSetOf()) { digits[it.index] }
            for (digit in (1..Board.SIZE).shuffled(random)) {
                if (digit in used) continue
                digits[i] = digit
                if (fill(i + 1)) return true
            }
            digits[i] = 0
            return false
        }
        check(fill(0))
        return digits
    }

    private fun puzzleOf(digits: IntArray) = Board.fromPuzzle(digits.joinToString(""))

    /** No 9x9 Sudoku with fewer givens has a unique solution. */
    private const val MIN_CLUES = 17
}

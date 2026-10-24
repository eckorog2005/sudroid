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

/**
 * A cell's solution digit and why it goes there. [Solver.hint] picks the preferred cell if it is
 * empty or wrong, then the first wrong entry, then the first naked single, then the first hidden
 * single, and otherwise the empty cell with the most digits ruled out.
 */
data class Hint(val cell: Cell, val digit: Int, val reason: Reason) {
    sealed interface Reason {
        /** The cell held [wrongDigit]. */
        data class Mistake(val wrongDigit: Int) : Reason

        /** The cell's row, column and box already hold every other digit. */
        data object NakedSingle : Reason

        /** No other empty cell in the cell's [house] can take the digit. */
        data class HiddenSingle(val house: House) : Reason

        /** No single technique finds the digit, so it comes straight from the solution. */
        data object FromSolution : Reason
    }

    enum class House { ROW, COLUMN, BOX }
}

internal fun findHint(board: Board, solution: Board, preferred: Cell?): Hint? {
    fun isWrong(cell: Cell) = board.digitAt(cell).let { it != 0 && it != solution.digitAt(cell) }
    fun hintAt(cell: Cell, reason: Hint.Reason) = Hint(cell, solution.digitAt(cell), reason)
    fun mistakeAt(cell: Cell) = hintAt(cell, Hint.Reason.Mistake(board.digitAt(cell)))
    fun explained(cell: Cell): Hint? = explain(board, cell, solution.digitAt(cell))?.let { hintAt(cell, it) }

    if (preferred != null) {
        if (isWrong(preferred)) return mistakeAt(preferred)
        if (board.digitAt(preferred) == 0) return explained(preferred) ?: hintAt(preferred, Hint.Reason.FromSolution)
    }
    Cell.ALL.firstOrNull(::isWrong)?.let { return mistakeAt(it) }
    val empty = Cell.ALL.filter { board.digitAt(it) == 0 }
    empty.firstOrNull { candidates(board, it).size == 1 && candidates(board, it).single() == solution.digitAt(it) }
        ?.let { return hintAt(it, Hint.Reason.NakedSingle) }
    empty.firstNotNullOfOrNull(::explained)?.let { return it }
    return empty.minByOrNull { candidates(board, it).size }?.let { hintAt(it, Hint.Reason.FromSolution) }
}

private fun explain(board: Board, cell: Cell, digit: Int): Hint.Reason? {
    val options = candidates(board, cell)
    if (options == setOf(digit)) return Hint.Reason.NakedSingle
    if (digit !in options) return null
    return Hint.House.entries.firstOrNull { house ->
        Cell.ALL.filter { it != cell && house.contains(it, cell) && board.digitAt(it) == 0 }
            .none { digit in candidates(board, it) }
    }?.let { Hint.Reason.HiddenSingle(it) }
}

private fun Hint.House.contains(cell: Cell, of: Cell): Boolean = when (this) {
    Hint.House.ROW -> cell.row == of.row
    Hint.House.COLUMN -> cell.col == of.col
    Hint.House.BOX -> cell.box == of.box
}

private fun candidates(board: Board, cell: Cell): Set<Int> {
    if (board.digitAt(cell) != 0) return emptySet()
    val used = cell.peers.mapTo(mutableSetOf(), board::digitAt)
    return (1..Board.SIZE).filterTo(mutableSetOf()) { it !in used }
}

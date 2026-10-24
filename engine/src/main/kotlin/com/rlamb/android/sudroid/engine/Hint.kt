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
 * single, then the first single that locked-candidates steps reveal, and otherwise the empty cell
 * with the most digits ruled out.
 */
data class Hint(val cell: Cell, val digit: Int, val reason: Reason) {
    sealed interface Reason {
        /** The cell held [wrongDigit]. */
        data class Mistake(val wrongDigit: Int) : Reason

        /** The cell's row, column and box already hold every other digit. */
        data object NakedSingle : Reason

        /** No other empty cell in the cell's [house] can take the digit. */
        data class HiddenSingle(val house: House) : Reason

        /** Once each of [locks] is ruled out, in order, [then] finds the hint's digit. */
        data class LockedCandidates(val locks: List<Lock>, val then: Reason) : Reason

        /** No technique here finds the digit, so it comes straight from the solution. */
        data object FromSolution : Reason
    }

    enum class House { ROW, COLUMN, BOX }

    /** One particular row, column or box; [index] is 0-based, with boxes numbered across then down. */
    data class Area(val house: House, val index: Int)

    /** Every spot for [digit] in [within] lies in [confinedTo], so no other cell of [confinedTo] can take it. */
    data class Lock(val digit: Int, val within: Area, val confinedTo: Area)
}

internal fun findHint(board: Board, solution: Board, preferred: Cell?): Hint? {
    val candidates = candidateMasks(board.digits())
    fun isWrong(cell: Cell) = board.digitAt(cell).let { it != 0 && it != solution.digitAt(cell) }
    fun hintAt(cell: Cell, reason: Hint.Reason) = Hint(cell, solution.digitAt(cell), reason)
    fun mistakeAt(cell: Cell) = hintAt(cell, Hint.Reason.Mistake(board.digitAt(cell)))
    fun explained(cell: Cell): Hint? = singleAt(candidates, cell, solution.digitAt(cell))?.let { hintAt(cell, it) }

    if (preferred != null) {
        if (isWrong(preferred)) return mistakeAt(preferred)
        if (board.digitAt(preferred) == 0) {
            return explained(preferred)
                ?: afterLockedCandidates(candidates, solution, listOf(preferred))
                ?: hintAt(preferred, Hint.Reason.FromSolution)
        }
    }
    Cell.ALL.firstOrNull(::isWrong)?.let { return mistakeAt(it) }
    val empty = Cell.ALL.filter { board.digitAt(it) == 0 }
    empty.firstOrNull { candidates[it.index] == 1 shl solution.digitAt(it) }?.let { return hintAt(it, Hint.Reason.NakedSingle) }
    empty.firstNotNullOfOrNull(::explained)?.let { return it }
    afterLockedCandidates(candidates, solution, empty)?.let { return it }
    return empty.minByOrNull { Integer.bitCount(candidates[it.index]) }?.let { hintAt(it, Hint.Reason.FromSolution) }
}

/**
 * The first of [cells] that becomes a single after locked-candidates eliminations. Applies them one
 * at a time until one reveals a single, then keeps only the steps that single depends on.
 */
private fun afterLockedCandidates(candidates: IntArray, solution: Board, cells: List<Cell>): Hint? {
    val steps = mutableListOf<Elimination>()
    val current = candidates.copyOf()
    while (true) {
        for (elimination in lockedCandidates(current)) {
            val after = current.copyOf().also(elimination::applyTo)
            for (cell in cells) {
                val digit = solution.digitAt(cell)
                if (singleAt(after, cell, digit) == null) continue
                val needed = trim(candidates, steps + elimination, cell, digit)
                val locks = needed.map { Hint.Lock(it.digit, area(it.within), area(it.confinedTo)) }
                val then = checkNotNull(replay(candidates, needed)?.let { singleAt(it, cell, digit) })
                return Hint(cell, digit, Hint.Reason.LockedCandidates(locks, then))
            }
        }
        val next = lockedCandidates(current).firstOrNull() ?: return null
        next.applyTo(current)
        steps += next
    }
}

/** Drops each step whose removal still leaves the other steps valid and [digit] a single at [cell]. */
private fun trim(candidates: IntArray, steps: List<Elimination>, cell: Cell, digit: Int): List<Elimination> {
    var kept = steps
    for (step in steps.dropLast(1)) {
        val without = kept - step
        val after = replay(candidates, without) ?: continue
        if (singleAt(after, cell, digit) != null) kept = without
    }
    return kept
}

/** Applies [steps] in order, or returns null if one of them no longer follows from the steps before it. */
private fun replay(candidates: IntArray, steps: List<Elimination>): IntArray? {
    val current = candidates.copyOf()
    for (step in steps) {
        val bit = 1 shl step.digit
        val spots = HOUSE_INDICES[step.within].filter { current[it] and bit != 0 }
        if (spots.size < 2 || !HOUSE_INDICES[step.confinedTo].containsAll(spots)) return null
        val outside = HOUSE_INDICES[step.confinedTo].filter { it !in HOUSE_INDICES[step.within] && current[it] and bit != 0 }
        if (outside.isEmpty()) return null
        outside.forEach { current[it] = current[it] and bit.inv() }
    }
    return current
}

/** How [digit] is a naked or hidden single at [cell] under [candidates], or null if it is neither. */
private fun singleAt(candidates: IntArray, cell: Cell, digit: Int): Hint.Reason? {
    val bit = 1 shl digit
    val mask = candidates[cell.index]
    if (mask == bit) return Hint.Reason.NakedSingle
    if (mask and bit == 0) return null
    return Hint.House.entries.firstOrNull { house ->
        HOUSE_INDICES[houseIndex(house, cell)].none { it != cell.index && candidates[it] and bit != 0 }
    }?.let { Hint.Reason.HiddenSingle(it) }
}

private fun houseIndex(house: Hint.House, cell: Cell): Int = when (house) {
    Hint.House.ROW -> cell.row
    Hint.House.COLUMN -> Board.SIZE + cell.col
    Hint.House.BOX -> 2 * Board.SIZE + cell.box
}

private fun area(houseIndex: Int) = Hint.Area(Hint.House.entries[houseIndex / Board.SIZE], houseIndex % Board.SIZE)

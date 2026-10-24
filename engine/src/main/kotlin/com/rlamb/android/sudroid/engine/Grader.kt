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

/** The human solving techniques [Grader] knows, from easiest to hardest. */
enum class Technique {
    /** A cell whose row, column and box rule out every digit but one. */
    NAKED_SINGLE,

    /** A digit that only one cell of a row, column or box can take. */
    HIDDEN_SINGLE,

    /**
     * A digit confined to one row or column inside a box, or to one box inside a row or column, which
     * rules it out of the rest of that line or box.
     */
    LOCKED_CANDIDATES,
}

object Grader {
    /**
     * The hardest technique a person needs to solve [board], always trying easier ones first. Null
     * when these techniques alone cannot finish it, including when it has no solution.
     */
    @JvmStatic
    fun grade(board: Board): Technique? = Grading(board.digits()).run()
}

private class Grading(private val digits: IntArray) {
    private val candidates = IntArray(Board.CELL_COUNT)
    private var hardest = Technique.NAKED_SINGLE

    fun run(): Technique? {
        for (i in digits.indices) {
            if (digits[i] != 0) continue
            val used = PEERS[i].fold(0) { mask, peer -> mask or (1 shl digits[peer]) }
            candidates[i] = ALL_DIGITS and used.inv()
        }
        while (digits.any { it == 0 }) {
            when {
                placeNakedSingle() -> {}
                placeHiddenSingle() -> use(Technique.HIDDEN_SINGLE)
                eliminateLockedCandidates() -> use(Technique.LOCKED_CANDIDATES)
                else -> return null
            }
        }
        return hardest
    }

    private fun use(technique: Technique) {
        if (technique > hardest) hardest = technique
    }

    private fun placeNakedSingle(): Boolean {
        val i = digits.indices.firstOrNull { digits[it] == 0 && Integer.bitCount(candidates[it]) == 1 } ?: return false
        place(i, Integer.numberOfTrailingZeros(candidates[i]))
        return true
    }

    private fun placeHiddenSingle(): Boolean {
        for (house in HOUSES) {
            for (digit in 1..Board.SIZE) {
                val spots = house.filter { candidates[it] and (1 shl digit) != 0 }
                if (spots.size == 1) {
                    place(spots.single(), digit)
                    return true
                }
            }
        }
        return false
    }

    private fun eliminateLockedCandidates(): Boolean {
        for (house in HOUSES) {
            for (digit in 1..Board.SIZE) {
                val bit = 1 shl digit
                val spots = house.filter { candidates[it] and bit != 0 }
                if (spots.isEmpty()) continue
                for (other in HOUSES) {
                    if (other === house || !other.containsAll(spots)) continue
                    val outside = other.filter { it !in house && candidates[it] and bit != 0 }
                    if (outside.isEmpty()) continue
                    outside.forEach { candidates[it] = candidates[it] and bit.inv() }
                    return true
                }
            }
        }
        return false
    }

    private fun place(i: Int, digit: Int) {
        digits[i] = digit
        candidates[i] = 0
        PEERS[i].forEach { candidates[it] = candidates[it] and (1 shl digit).inv() }
    }

    private companion object {
        const val ALL_DIGITS = 0b11_1111_1110
        val PEERS: List<List<Int>> = Cell.ALL.map { cell -> cell.peers.map(Cell::index) }
        val HOUSES: List<Set<Int>> = Cell.HOUSES.map { house -> house.mapTo(linkedSetOf(), Cell::index) }
    }
}

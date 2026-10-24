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

/** [Cell.HOUSES] as cell indices: rows are 0 to 8, columns 9 to 17 and boxes 18 to 26. */
internal val HOUSE_INDICES: List<Set<Int>> = Cell.HOUSES.map { house -> house.mapTo(linkedSetOf(), Cell::index) }

internal val PEER_INDICES: List<List<Int>> = Cell.ALL.map { cell -> cell.peers.map(Cell::index) }

private const val ALL_DIGITS = 0b11_1111_1110

/** A bitmask per cell of the digits its row, column and box still allow, with bit `d` for `d`; 0 for filled cells. */
internal fun candidateMasks(digits: IntArray): IntArray = IntArray(Board.CELL_COUNT) { i ->
    if (digits[i] != 0) 0 else ALL_DIGITS and PEER_INDICES[i].fold(0) { mask, peer -> mask or (1 shl digits[peer]) }.inv()
}

/**
 * Locked candidates: [digit]'s two or more spots in house [within] all lie in house [confinedTo], so
 * it is ruled out of [cells], the rest of [confinedTo]. Houses are indices into [HOUSE_INDICES].
 */
internal class Elimination(val digit: Int, val within: Int, val confinedTo: Int, val cells: List<Int>) {
    fun applyTo(candidates: IntArray) {
        cells.forEach { candidates[it] = candidates[it] and (1 shl digit).inv() }
    }
}

internal fun lockedCandidates(candidates: IntArray): Sequence<Elimination> = sequence {
    for ((within, house) in HOUSE_INDICES.withIndex()) {
        for (digit in 1..Board.SIZE) {
            val bit = 1 shl digit
            val spots = house.filter { candidates[it] and bit != 0 }
            if (spots.size < 2) continue
            for ((confinedTo, other) in HOUSE_INDICES.withIndex()) {
                if (confinedTo == within || !other.containsAll(spots)) continue
                val outside = other.filter { it !in house && candidates[it] and bit != 0 }
                if (outside.isNotEmpty()) yield(Elimination(digit, within, confinedTo, outside))
            }
        }
    }
}

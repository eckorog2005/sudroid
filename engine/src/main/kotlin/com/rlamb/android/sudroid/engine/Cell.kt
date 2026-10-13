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

data class Cell(val row: Int, val col: Int) {
    init {
        require(row in 0 until Board.SIZE && col in 0 until Board.SIZE) {
            "Cell ($row, $col) is outside the ${Board.SIZE}x${Board.SIZE} grid"
        }
    }

    internal val index: Int get() = row * Board.SIZE + col

    internal val box: Int get() = (row / BOX_SIZE) * BOX_SIZE + col / BOX_SIZE

    val peers: List<Cell> get() = PEERS[index]

    companion object {
        internal const val BOX_SIZE = 3

        @JvmField
        val ALL: List<Cell> = List(Board.CELL_COUNT) { Cell(it / Board.SIZE, it % Board.SIZE) }

        private val PEERS: List<List<Cell>> = ALL.map { cell ->
            ALL.filter { it != cell && (it.row == cell.row || it.col == cell.col || it.box == cell.box) }
        }
    }
}

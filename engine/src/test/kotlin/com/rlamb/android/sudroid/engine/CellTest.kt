package com.rlamb.android.sudroid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CellTest {
    @Test
    fun `a cell has 20 peers and is not its own peer`() {
        for (cell in Cell.ALL) {
            assertEquals(20, cell.peers.size)
            assertFalse(cell in cell.peers)
        }
    }

    @Test
    fun `peers are the cells in the same row, column or box`() {
        val peers = Cell(4, 4).peers
        assertTrue(Cell(4, 0) in peers)
        assertTrue(Cell(0, 4) in peers)
        assertTrue(Cell(3, 3) in peers)
        assertFalse(Cell(0, 0) in peers)
    }

    @Test
    fun `there are 27 houses of 9 cells and every cell is in one row, one column and one box`() {
        assertEquals(27, Cell.HOUSES.size)
        assertTrue(Cell.HOUSES.all { it.size == Board.SIZE })
        assertTrue(Cell.ALL.all { cell -> Cell.HOUSES.count { cell in it } == 3 })
    }
}

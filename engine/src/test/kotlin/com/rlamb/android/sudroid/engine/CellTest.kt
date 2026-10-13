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
}

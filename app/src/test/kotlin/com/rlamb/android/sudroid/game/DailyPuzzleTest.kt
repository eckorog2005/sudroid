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

package com.rlamb.android.sudroid.game

import kotlin.test.Test
import kotlin.test.assertEquals

class DailyPuzzleTest {
    @Test
    fun `every puzzle comes up once before any repeats`() {
        for (count in listOf(450, 451, 1000)) {
            val indices = (TODAY until TODAY + count).map { dailyPuzzleIndex(it, count) }

            assertEquals((0 until count).toSet(), indices.toSet(), "count $count")
        }
    }

    @Test
    fun `days before 1970 still give a valid index`() {
        assertEquals((0 until 450).toSet(), (-450L until 0L).map { dailyPuzzleIndex(it, 450) }.toSet())
    }

    private companion object {
        const val TODAY = 20_750L
    }
}

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

class ElapsedTimeTest {
    @Test
    fun `formats minutes and seconds with two digits each`() {
        assertEquals("00:00", formatElapsed(0))
        assertEquals("01:05", formatElapsed(65))
        assertEquals("125:00", formatElapsed(7_500))
    }

    @Test
    fun `parses what it formats`() {
        assertEquals(65, parseElapsed("01:05"))
        assertEquals(7_500, parseElapsed(formatElapsed(7_500)))
    }

    @Test
    fun `unreadable times from old saves count as zero`() {
        assertEquals(0, parseElapsed(null))
        assertEquals(0, parseElapsed(""))
        assertEquals(0, parseElapsed("00:0-1"))
    }
}

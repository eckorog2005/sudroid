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

package com.rlamb.android.sudroid.stats

import com.rlamb.android.sudroid.game.Difficulty
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {
    @Test
    fun `no completions gives empty stats for every difficulty`() {
        val stats = statsOf(emptyList(), TODAY)

        assertEquals(Stats(), stats)
        assertEquals(0, stats.completed)
        assertEquals(Difficulty.entries.toSet(), stats.byDifficulty.keys)
    }

    @Test
    fun `each difficulty gets its own count, best and rounded average`() {
        val stats = statsOf(
            listOf(easy(300), easy(200), easy(201), Completion(Difficulty.HARD, 900, TODAY)),
            TODAY,
        )

        assertEquals(DifficultyStats(3, bestSeconds = 200, averageSeconds = 234), stats.byDifficulty[Difficulty.EASY])
        assertEquals(DifficultyStats(), stats.byDifficulty[Difficulty.MEDIUM])
        assertEquals(DifficultyStats(1, bestSeconds = 900, averageSeconds = 900), stats.byDifficulty[Difficulty.HARD])
        assertEquals(4, stats.completed)
    }

    @Test
    fun `several games on one day count as one streak day`() {
        val stats = statsOf(listOf(easy(day = TODAY), easy(day = TODAY), easy(day = TODAY - 1)), TODAY)

        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun `a streak that ended yesterday is still current`() {
        val stats = statsOf(listOf(easy(day = TODAY - 2), easy(day = TODAY - 1)), TODAY)

        assertEquals(2, stats.currentStreak)
    }

    @Test
    fun `a missed day ends the current streak but keeps the longest`() {
        val days = listOf(TODAY - 10, TODAY - 9, TODAY - 8, TODAY - 5, TODAY - 2)
        val stats = statsOf(days.map { easy(day = it) }, TODAY)

        assertEquals(0, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    @Test
    fun `completions out of order still form streaks`() {
        val stats = statsOf(listOf(easy(day = TODAY), easy(day = TODAY - 2), easy(day = TODAY - 1)), TODAY)

        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    private fun easy(seconds: Int = 60, day: Long = TODAY) = Completion(Difficulty.EASY, seconds, day)

    private companion object {
        const val TODAY = 20_750L
    }
}

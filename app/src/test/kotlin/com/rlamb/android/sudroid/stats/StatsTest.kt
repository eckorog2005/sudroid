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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `hinted games count as completed and keep streaks but set no times`() {
        val stats = statsOf(
            listOf(easy(100).copy(hints = 1), easy(300), Completion(Difficulty.HARD, 50, TODAY - 1, hints = 3)),
            TODAY,
        )

        assertEquals(DifficultyStats(2, bestSeconds = 300, averageSeconds = 300, withoutHints = 1), stats.byDifficulty[Difficulty.EASY])
        assertEquals(DifficultyStats(1, withoutHints = 0), stats.byDifficulty[Difficulty.HARD])
        assertEquals(2, stats.currentStreak)
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

    @Test
    fun `daily streaks count challenge days, not the days they were finished on`() {
        val completions = listOf(
            easy(day = TODAY, dailyDay = TODAY),
            // Yesterday's challenge, finished just after midnight.
            easy(day = TODAY, dailyDay = TODAY - 1),
            easy(day = TODAY - 3, dailyDay = TODAY - 3),
            easy(day = TODAY - 2),
        )

        val stats = statsOf(completions, TODAY)

        assertEquals(2, stats.dailyCurrentStreak)
        assertEquals(2, stats.dailyLongestStreak)
        assertTrue(stats.dailySolvedToday)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `an unsolved daily challenge today leaves yesterday's streak current`() {
        val days = listOf(TODAY - 9, TODAY - 8, TODAY - 7, TODAY - 2, TODAY - 1)
        val stats = statsOf(days.map { easy(day = it, dailyDay = it) } + easy(day = TODAY), TODAY)

        assertEquals(2, stats.dailyCurrentStreak)
        assertEquals(3, stats.dailyLongestStreak)
        assertFalse(stats.dailySolvedToday)
    }

    private fun easy(seconds: Int = 60, day: Long = TODAY, dailyDay: Long? = null) =
        Completion(Difficulty.EASY, seconds, day, dailyDay)

    private companion object {
        const val TODAY = 20_750L
    }
}

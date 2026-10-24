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
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

/**
 * [epochDay] is the local calendar day the game was finished on, counted from 1970-01-01.
 * [dailyDay] is the day of the daily challenge it solved, or null for a regular game.
 */
data class Completion(val difficulty: Difficulty, val elapsedSeconds: Int, val epochDay: Long, val dailyDay: Long? = null)

/** The times are null until a game of that difficulty has been completed. */
data class DifficultyStats(val completed: Int = 0, val bestSeconds: Int? = null, val averageSeconds: Int? = null)

/**
 * Streaks count consecutive days with at least one completed game, of any difficulty. Daily streaks
 * count consecutive daily challenges solved, by the challenge's day.
 */
data class Stats(
    val byDifficulty: Map<Difficulty, DifficultyStats> = Difficulty.entries.associateWith { DifficultyStats() },
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val dailyCurrentStreak: Int = 0,
    val dailyLongestStreak: Int = 0,
    val dailySolvedToday: Boolean = false,
) {
    val completed: Int = byDifficulty.values.sumOf { it.completed }
}

interface StatsRepository {
    fun stats(): Flow<Stats>
}

/** A streak is still current if its last day is [today] or yesterday, since today's game may not be played yet. */
fun statsOf(completions: List<Completion>, today: Long): Stats {
    val byDifficulty = Difficulty.entries.associateWith { difficulty ->
        val times = completions.filter { it.difficulty == difficulty }.map { it.elapsedSeconds }
        DifficultyStats(times.size, times.minOrNull(), times.takeIf { it.isNotEmpty() }?.average()?.roundToInt())
    }
    val streak = streakOf(completions.map { it.epochDay }, today)
    val dailyDays = completions.mapNotNull { it.dailyDay }
    val dailyStreak = streakOf(dailyDays, today)
    return Stats(
        byDifficulty = byDifficulty,
        currentStreak = streak.current,
        longestStreak = streak.longest,
        dailyCurrentStreak = dailyStreak.current,
        dailyLongestStreak = dailyStreak.longest,
        dailySolvedToday = today in dailyDays,
    )
}

private data class Streak(val current: Int, val longest: Int)

private fun streakOf(days: Collection<Long>, today: Long): Streak {
    val runs = streaks(days.toSortedSet())
    val current = runs.lastOrNull()?.takeIf { it.last >= today - 1 }
    return Streak(
        current = current?.let { (it.last - it.first + 1).toInt() } ?: 0,
        longest = runs.maxOfOrNull { (it.last - it.first + 1).toInt() } ?: 0,
    )
}

private fun streaks(days: Collection<Long>): List<LongRange> = buildList {
    for (day in days) {
        val last = lastOrNull()
        if (last != null && day == last.last + 1) set(lastIndex, last.first..day) else add(day..day)
    }
}

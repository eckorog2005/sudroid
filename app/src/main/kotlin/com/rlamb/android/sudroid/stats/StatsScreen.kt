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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rlamb.android.sudroid.R
import com.rlamb.android.sudroid.game.Difficulty
import com.rlamb.android.sudroid.game.formatElapsed
import com.rlamb.android.sudroid.ui.SudroidTheme

@Composable
fun StatsScreen(stats: Stats, onBackToMenu: () -> Unit) {
    Scaffold { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.stats),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            StatsCard(null) {
                StatRow(stringResource(R.string.stats_completed), stats.completed.toString())
                StatRow(stringResource(R.string.stats_current_streak), days(stats.currentStreak))
                StatRow(stringResource(R.string.stats_longest_streak), days(stats.longestStreak))
            }
            StatsCard(stringResource(R.string.daily)) {
                StatRow(stringResource(R.string.stats_current_streak), days(stats.dailyCurrentStreak))
                StatRow(stringResource(R.string.stats_longest_streak), days(stats.dailyLongestStreak))
            }
            for ((difficulty, label) in DifficultyLabels) {
                val entry = stats.byDifficulty.getValue(difficulty)
                StatsCard(stringResource(label)) {
                    StatRow(stringResource(R.string.stats_completed), entry.completed.toString())
                    StatRow(stringResource(R.string.stats_best_time), time(entry.bestSeconds))
                    StatRow(stringResource(R.string.stats_average_time), time(entry.averageSeconds))
                }
            }
            Button(onClick = onBackToMenu) { Text(stringResource(R.string.backMenu)) }
        }
    }
}

@Composable
private fun StatsCard(title: String?, content: @Composable () -> Unit) {
    Card(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

// Merging each row into one node lets TalkBack read the label and value together.
@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(label, Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun days(count: Int) = pluralStringResource(R.plurals.stats_days, count, count)

@Composable
private fun time(seconds: Int?) = seconds?.let(::formatElapsed) ?: stringResource(R.string.stats_no_time)

private val DifficultyLabels = listOf(
    Difficulty.EASY to R.string.easy,
    Difficulty.MEDIUM to R.string.medium,
    Difficulty.HARD to R.string.hard,
)

@Preview(showBackground = true, widthDp = 360, heightDp = 1040)
@Composable
private fun StatsScreenPreview() {
    val stats = Stats(
        byDifficulty = mapOf(
            Difficulty.EASY to DifficultyStats(12, bestSeconds = 241, averageSeconds = 388),
            Difficulty.MEDIUM to DifficultyStats(3, bestSeconds = 610, averageSeconds = 745),
            Difficulty.HARD to DifficultyStats(),
        ),
        currentStreak = 4,
        longestStreak = 9,
        dailyCurrentStreak = 2,
        dailyLongestStreak = 5,
    )
    SudroidTheme { StatsScreen(stats, onBackToMenu = {}) }
}

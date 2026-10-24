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


package com.rlamb.android.sudroid.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rlamb.android.sudroid.R
import com.rlamb.android.sudroid.game.Difficulty
import com.rlamb.android.sudroid.game.GameRequest
import com.rlamb.android.sudroid.game.Puzzle
import com.rlamb.android.sudroid.game.SavedGame
import com.rlamb.android.sudroid.game.formatElapsed
import com.rlamb.android.sudroid.ui.SudroidTheme
import com.rlamb.android.sudroid.ui.label

@Composable
fun MenuScreen(
    savedGame: SavedGame?,
    dailySolvedToday: Boolean,
    dailyStreak: Int,
    onStart: (GameRequest) -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(24.dp))
                if (savedGame != null) {
                    Button(onClick = { onStart(GameRequest.Resume) }, ButtonModifier) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.resume))
                            Text(
                                stringResource(
                                    R.string.resume_details,
                                    stringResource(savedGame.puzzle.difficulty.label),
                                    formatElapsed(savedGame.elapsedSeconds),
                                ),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { onStart(GameRequest.Daily) }, ButtonModifier, enabled = !dailySolvedToday) {
                        Text(stringResource(R.string.daily))
                    }
                    val status = listOfNotNull(
                        stringResource(R.string.daily_solved).takeIf { dailySolvedToday },
                        pluralStringResource(R.plurals.daily_streak, dailyStreak, dailyStreak).takeIf { dailyStreak > 0 },
                    )
                    if (status.isNotEmpty()) {
                        Text(
                            status.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                for (difficulty in Difficulty.entries) {
                    FilledTonalButton(onClick = { onStart(GameRequest.New(difficulty)) }, ButtonModifier) {
                        Text(stringResource(difficulty.label))
                    }
                }
                OutlinedButton(onClick = onStats, ButtonModifier) { Text(stringResource(R.string.stats)) }
            }
            IconButton(onClick = onSettings, Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.settings))
            }
        }
    }
}

private val ButtonModifier = Modifier.widthIn(min = 200.dp)

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun MenuScreenPreview() {
    SudroidTheme {
        MenuScreen(
            savedGame = SavedGame(Puzzle(1, Difficulty.MEDIUM, "0".repeat(81)), "0".repeat(81), elapsedSeconds = 252),
            dailySolvedToday = false,
            dailyStreak = 3,
            onStart = {},
            onStats = {},
            onSettings = {},
        )
    }
}

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rlamb.android.sudroid.R
import com.rlamb.android.sudroid.game.Difficulty
import com.rlamb.android.sudroid.game.GameRequest
import com.rlamb.android.sudroid.game.MistakeMode
import com.rlamb.android.sudroid.ui.SudroidTheme

@Composable
fun MenuScreen(
    hasSavedGame: Boolean,
    mistakeMode: MistakeMode,
    onMistakeMode: (MistakeMode) -> Unit,
    onStart: (GameRequest) -> Unit,
) {
    Scaffold { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onStart(GameRequest.Resume) }, ButtonModifier, enabled = hasSavedGame) {
                Text(stringResource(R.string.resume))
            }
            for ((difficulty, label) in DifficultyLabels) {
                FilledTonalButton(onClick = { onStart(GameRequest.New(difficulty)) }, ButtonModifier) {
                    Text(stringResource(label))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.mistakes), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                MistakeLabels.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = mode == mistakeMode,
                        onClick = { onMistakeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, MistakeLabels.size),
                    ) {
                        Text(stringResource(label), maxLines = 1)
                    }
                }
            }
        }
    }
}

private val ButtonModifier = Modifier.width(200.dp)

private val DifficultyLabels = listOf(
    Difficulty.EASY to R.string.easy,
    Difficulty.MEDIUM to R.string.medium,
    Difficulty.HARD to R.string.hard,
)

private val MistakeLabels = listOf(
    MistakeMode.ALWAYS to R.string.mistakes_always,
    MistakeMode.ON_REQUEST to R.string.mistakes_on_request,
    MistakeMode.NEVER to R.string.mistakes_never,
)

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun MenuScreenPreview() {
    SudroidTheme { MenuScreen(hasSavedGame = true, mistakeMode = MistakeMode.ALWAYS, onMistakeMode = {}, onStart = {}) }
}

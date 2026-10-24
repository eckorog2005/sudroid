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


package com.rlamb.android.sudroid.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rlamb.android.sudroid.R
import com.rlamb.android.sudroid.game.MistakeMode
import com.rlamb.android.sudroid.ui.SudroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(mistakeMode: MistakeMode, onMistakeMode: (MistakeMode) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
                    }
                },
            )
        },
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                Text(
                    stringResource(R.string.mistakes),
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.selectableGroup()) {
                    for (option in MistakeOptions) {
                        val selected = option.mode == mistakeMode
                        ListItem(
                            headlineContent = { Text(stringResource(option.label)) },
                            modifier = Modifier.selectable(selected, role = Role.RadioButton, onClick = { onMistakeMode(option.mode) }),
                            supportingContent = { Text(stringResource(option.summary)) },
                            leadingContent = { RadioButton(selected, onClick = null) },
                        )
                    }
                }
            }
        }
    }
}

private class MistakeOption(val mode: MistakeMode, @StringRes val label: Int, @StringRes val summary: Int)

private val MistakeOptions = listOf(
    MistakeOption(MistakeMode.ALWAYS, R.string.mistakes_always, R.string.mistakes_always_summary),
    MistakeOption(MistakeMode.ON_REQUEST, R.string.mistakes_on_request, R.string.mistakes_on_request_summary),
    MistakeOption(MistakeMode.NEVER, R.string.mistakes_never, R.string.mistakes_never_summary),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun SettingsScreenPreview() {
    SudroidTheme { SettingsScreen(MistakeMode.ON_REQUEST, onMistakeMode = {}, onBack = {}) }
}

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
package com.rlamb.android.sudroid.data

import android.content.Context
import androidx.core.content.edit
import com.rlamb.android.sudroid.game.MistakeMode
import com.rlamb.android.sudroid.game.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _mistakeMode = MutableStateFlow(
        MistakeMode.entries.find { it.name == preferences.getString(MISTAKE_MODE, null) } ?: MistakeMode.ALWAYS,
    )
    override val mistakeMode: StateFlow<MistakeMode> = _mistakeMode.asStateFlow()

    override fun setMistakeMode(mode: MistakeMode) {
        preferences.edit { putString(MISTAKE_MODE, mode.name) }
        _mistakeMode.value = mode
    }

    private companion object {
        const val MISTAKE_MODE = "mistake_mode"
    }
}

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


package com.rlamb.android.sudroid.ui

import androidx.annotation.StringRes
import com.rlamb.android.sudroid.R
import com.rlamb.android.sudroid.game.Difficulty

@get:StringRes
val Difficulty.label: Int
    get() = when (this) {
        Difficulty.EASY -> R.string.easy
        Difficulty.MEDIUM -> R.string.medium
        Difficulty.HARD -> R.string.hard
    }

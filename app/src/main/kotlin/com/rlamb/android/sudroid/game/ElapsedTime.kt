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

/** Formats as `MM:SS`, the format the saved game and score screen have always used. */
fun formatElapsed(seconds: Int): String =
    "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"

/** Returns 0 for anything unreadable; the old timer could save values such as `00:0-1`. */
fun parseElapsed(text: String?): Int {
    val parts = text?.split(':')?.map { it.toIntOrNull() } ?: return 0
    val (minutes, seconds) = parts.takeIf { it.size == 2 } ?: return 0
    if (minutes == null || seconds == null || minutes < 0 || seconds !in 0..59) return 0
    return minutes * 60 + seconds
}

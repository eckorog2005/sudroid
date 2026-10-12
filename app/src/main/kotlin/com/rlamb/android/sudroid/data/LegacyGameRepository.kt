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

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.rlamb.android.sudroid.SudroidDatabaseHelper
import com.rlamb.android.sudroid.game.Difficulty
import com.rlamb.android.sudroid.game.GameRepository
import com.rlamb.android.sudroid.game.Puzzle
import com.rlamb.android.sudroid.game.SavedGame
import com.rlamb.android.sudroid.game.formatElapsed
import com.rlamb.android.sudroid.game.parseElapsed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads and writes the original version 1 SQLite schema, keeping old saved games resumable. */
class LegacyGameRepository private constructor(context: Context) : GameRepository {
    private val helper = SudroidDatabaseHelper(context)

    override suspend fun randomPuzzle(difficulty: Difficulty): Puzzle = withContext(Dispatchers.IO) {
        helper.readableDatabase.rawQuery(
            "SELECT _id, difficulty, pattern FROM Puzzle WHERE difficulty = ? ORDER BY random() LIMIT 1",
            arrayOf(difficulty.name.lowercase()),
        ).use { cursor ->
            check(cursor.moveToFirst()) { "No $difficulty puzzles in the database" }
            cursor.toPuzzle()
        }
    }

    override suspend fun savedGame(): SavedGame? = withContext(Dispatchers.IO) {
        helper.readableDatabase.rawQuery(
            "SELECT p._id, p.difficulty, p.pattern, c.userEntered, c.time " +
                "FROM Current c JOIN Puzzle p ON p._id = c._id LIMIT 1",
            null,
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val entries = cursor.getString(3)?.takeIf { it.length == 81 && it.all(Char::isDigit) }
                ?: return@use null
            SavedGame(cursor.toPuzzle(), entries, parseElapsed(cursor.getString(4)))
        }
    }

    override suspend fun save(game: SavedGame) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("_id", game.puzzle.id)
            put("time", formatElapsed(game.elapsedSeconds))
            put("difficulty", game.puzzle.difficulty.name.lowercase())
            put("userEntered", game.entries)
        }
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("Current", null, null)
            db.insertOrThrow("Current", null, values)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun deleteSavedGame() {
        withContext(Dispatchers.IO) { helper.writableDatabase.delete("Current", null, null) }
    }

    private fun Cursor.toPuzzle() = Puzzle(
        id = getInt(0),
        difficulty = Difficulty.valueOf(getString(1).uppercase()),
        digits = getString(2),
    )

    companion object {
        @Volatile
        private var instance: LegacyGameRepository? = null

        // One open connection for the whole process; ViewModels come and go with each game.
        fun get(context: Context): LegacyGameRepository =
            instance ?: synchronized(this) {
                instance ?: LegacyGameRepository(context.applicationContext).also { instance = it }
            }
    }
}

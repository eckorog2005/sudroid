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
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rlamb.android.sudroid.game.parseElapsed

@Database(
    entities = [PuzzleEntity::class, SavedGameEntity::class, CompletedGameEntity::class],
    version = 7,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 6, to = 7),
    ],
)
abstract class SudroidDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        /** The file name the original SQLiteOpenHelper used, so upgrades open the old database. */
        const val NAME = "SudroidDB"

        fun build(context: Context, name: String = NAME): SudroidDatabase =
            Room.databaseBuilder(context, SudroidDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, AddGeneratedPuzzles(context.applicationContext, from = 5, to = 6))
                .addCallback(SeedPuzzles(context.applicationContext))
                .build()
    }
}

/** Version 1 is the original hand-written schema, created by running `data.txt`. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `puzzles` (`id` INTEGER NOT NULL, `difficulty` TEXT NOT NULL, " +
                "`digits` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `saved_game` (`slot` INTEGER NOT NULL, `puzzle_id` INTEGER NOT NULL, " +
                "`entries` TEXT NOT NULL, `elapsed_seconds` INTEGER NOT NULL, PRIMARY KEY(`slot`))",
        )
        convertLegacyTables(db)
    }
}

// Fresh installs build the version 1 tables from data.txt and convert them the same way an
// upgrade does, so the puzzle IDs in both paths always agree.
private class SeedPuzzles(private val context: Context) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        context.assets.open("data.txt").bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).forEach(db::execSQL)
        }
        convertLegacyTables(db)
        insertGeneratedPuzzles(context, db)
    }
}

/**
 * Adds the puzzles in `puzzles.txt` that the database lacks. When more are appended to the file, a
 * new version adds another of these migrations.
 */
private class AddGeneratedPuzzles(private val context: Context, from: Int, to: Int) : Migration(from, to) {
    override fun migrate(db: SupportSQLiteDatabase) = insertGeneratedPuzzles(context, db)
}

private fun insertGeneratedPuzzles(context: Context, db: SupportSQLiteDatabase) {
    context.assets.open("puzzles.txt").bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).forEach { line ->
            val (id, difficulty, digits) = line.split(',')
            db.execSQL("INSERT OR IGNORE INTO puzzles (id, difficulty, digits) VALUES (?, ?, ?)", arrayOf<Any>(id.toInt(), difficulty, digits))
        }
    }
}

private fun convertLegacyTables(db: SupportSQLiteDatabase) {
    db.execSQL("INSERT INTO puzzles (id, difficulty, digits) SELECT _id, upper(difficulty), pattern FROM Puzzle")
    // The old game wrote `resume` into Current.difficulty, so the difficulty comes from Puzzle.
    db.query("SELECT c._id, c.userEntered, c.time FROM Current c JOIN Puzzle p ON p._id = c._id LIMIT 1").use { cursor ->
        if (!cursor.moveToFirst()) return@use
        val entries = cursor.getString(1)?.takeIf { it.length == 81 && it.all(Char::isDigit) } ?: return@use
        db.execSQL(
            "INSERT INTO saved_game (slot, puzzle_id, entries, elapsed_seconds) VALUES (0, ?, ?, ?)",
            arrayOf<Any>(cursor.getInt(0), entries, parseElapsed(cursor.getString(2))),
        )
    }
    for (table in listOf("Puzzle", "Current", "Weekly", "User")) db.execSQL("DROP TABLE IF EXISTS `$table`")
}

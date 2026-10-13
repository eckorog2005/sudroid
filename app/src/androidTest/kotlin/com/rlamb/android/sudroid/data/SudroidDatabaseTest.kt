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
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rlamb.android.sudroid.game.Difficulty
import com.rlamb.android.sudroid.game.Puzzle
import com.rlamb.android.sudroid.game.SavedGame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SudroidDatabaseTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private var database: SudroidDatabase? = null

    @Before
    @After
    fun deleteDatabase() {
        database?.close()
        context.deleteDatabase(NAME)
    }

    @Test
    fun upgradeKeepsTheGameInProgress() = runBlocking {
        val pattern = createVersion1(current = "('12', '03:25', 'resume', '$ENTRIES')")

        val saved = repository().savedGame()

        assertEquals(SavedGame(Puzzle(12, Difficulty.EASY, pattern), ENTRIES, elapsedSeconds = 205), saved)
    }

    @Test
    fun upgradeResumesAnUnreadableTimeFromZero() = runBlocking {
        createVersion1(current = "('160', '00:0-1', 'medium', '$ENTRIES')")

        val saved = repository().savedGame()

        assertEquals(Difficulty.MEDIUM, saved?.puzzle?.difficulty)
        assertEquals(0, saved?.elapsedSeconds)
    }

    @Test
    fun upgradeDropsACorruptSave() = runBlocking {
        createVersion1(current = "('12', '01:00', 'easy', 'garbage')")

        assertNull(repository().savedGame())
    }

    @Test
    fun upgradeKeepsEveryPuzzleAndDropsTheOldTables() = runBlocking {
        createVersion1(current = null)
        val repository = repository()

        assertNull(repository.savedGame())
        assertEquals(PUZZLE_COUNTS, puzzleCounts())
        assertEquals(listOf("puzzles", "saved_game"), userTables())
    }

    @Test
    fun freshInstallSeedsEveryPuzzleWithNoSavedGame() = runBlocking {
        val repository = repository()

        assertNull(repository.savedGame())
        assertEquals(PUZZLE_COUNTS, puzzleCounts())
        assertEquals(listOf("puzzles", "saved_game"), userTables())
        assertEquals(Difficulty.HARD, repository.randomPuzzle(Difficulty.HARD).difficulty)
    }

    @Test
    fun savingReplacesTheSavedGameAndDeletingClearsIt() = runBlocking {
        val repository = repository()
        val dao = checkNotNull(database).gameDao()
        val first = SavedGame(repository.randomPuzzle(Difficulty.EASY), ENTRIES, elapsedSeconds = 1)
        val second = SavedGame(repository.randomPuzzle(Difficulty.HARD), ENTRIES, elapsedSeconds = 2, notes = NOTES)

        repository.save(first)
        repository.save(second)
        assertEquals(second, repository.savedGame())
        assertTrue(dao.hasSavedGame().first())

        repository.deleteSavedGame()
        assertNull(repository.savedGame())
        assertFalse(dao.hasSavedGame().first())
    }

    @Test
    fun upgradeFromVersion2KeepsTheGameWithNoNotes() = runBlocking {
        SudroidDatabase.build(context, NAME).apply { openHelper.writableDatabase }.close()
        val pattern = SQLiteDatabase.openDatabase(context.getDatabasePath(NAME).path, null, 0).use { db ->
            db.execSQL("DROP TABLE saved_game")
            db.execSQL(
                "CREATE TABLE `saved_game` (`slot` INTEGER NOT NULL, `puzzle_id` INTEGER NOT NULL, " +
                    "`entries` TEXT NOT NULL, `elapsed_seconds` INTEGER NOT NULL, PRIMARY KEY(`slot`))",
            )
            db.execSQL("INSERT INTO saved_game VALUES (0, 12, '$ENTRIES', 30)")
            db.version = 2
            db.rawQuery("SELECT digits FROM puzzles WHERE id = 12", null).use { it.moveToFirst(); it.getString(0) }
        }

        val saved = repository().savedGame()

        assertEquals(SavedGame(Puzzle(12, Difficulty.EASY, pattern), ENTRIES, elapsedSeconds = 30, notes = ""), saved)
    }

    /** Builds the database the way the original SQLiteOpenHelper did and returns puzzle 12's pattern. */
    private fun createVersion1(current: String?): String {
        val statements = context.assets.open("data.txt").bufferedReader().readText().split("\r\n")
        return SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(NAME), null).use { db ->
            statements.filter(String::isNotBlank).forEach(db::execSQL)
            if (current != null) db.execSQL("INSERT INTO Current VALUES $current")
            db.version = 1
            db.rawQuery("SELECT pattern FROM Puzzle WHERE _id = 12", null).use { it.moveToFirst(); it.getString(0) }
        }
    }

    private fun repository(): RoomGameRepository {
        val db = SudroidDatabase.build(context, NAME).also { database = it }
        return RoomGameRepository(db.gameDao())
    }

    private fun puzzleCounts(): Map<String, Int> =
        checkNotNull(database).openHelper.readableDatabase
            .query("SELECT difficulty, count(*) FROM puzzles GROUP BY difficulty").use { cursor ->
                buildMap { while (cursor.moveToNext()) put(cursor.getString(0), cursor.getInt(1)) }
            }

    private fun userTables(): List<String> =
        checkNotNull(database).openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' " +
                "AND name NOT IN ('android_metadata', 'room_master_table') ORDER BY name",
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    private companion object {
        const val NAME = "sudroid-database-test"
        val ENTRIES = "4" + "0".repeat(80)
        val NOTES = "," + "13," + ",".repeat(78) + "9"
        val PUZZLE_COUNTS = mapOf("EASY" to 150, "MEDIUM" to 150, "HARD" to 150)
    }
}

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
import com.rlamb.android.sudroid.stats.DifficultyStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone
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
        assertEquals(listOf("completed_games", "puzzles", "saved_game"), userTables())
    }

    @Test
    fun freshInstallSeedsEveryPuzzleWithNoSavedGame() = runBlocking {
        val repository = repository()

        assertNull(repository.savedGame())
        assertEquals(PUZZLE_COUNTS, puzzleCounts())
        assertEquals(listOf("completed_games", "puzzles", "saved_game"), userTables())
        assertEquals(Difficulty.HARD, repository.randomPuzzle(Difficulty.HARD).difficulty)
    }

    @Test
    fun savingReplacesTheSavedGameAndDeletingClearsIt() = runBlocking {
        val repository = repository()
        val first = SavedGame(repository.randomPuzzle(Difficulty.EASY), ENTRIES, elapsedSeconds = 1)
        val second = SavedGame(repository.randomPuzzle(Difficulty.HARD), ENTRIES, elapsedSeconds = 2, notes = NOTES)

        repository.save(first)
        repository.save(second)
        assertEquals(second, repository.savedGame())
        assertTrue(repository.hasSavedGame().first())

        repository.deleteSavedGame()
        assertNull(repository.savedGame())
        assertFalse(repository.hasSavedGame().first())
    }

    @Test
    fun completingAGameRecordsItAndDeletesTheSave() = runBlocking {
        var now = OCT_23_UTC + 23 * HOUR
        val repository = repository(now = { now }, timeZone = TimeZone.getTimeZone("GMT-05:00"))
        val hard = repository.randomPuzzle(Difficulty.HARD)
        repository.save(SavedGame(hard, ENTRIES, elapsedSeconds = 1))

        repository.completeGame(hard, elapsedSeconds = 600)
        now += 2 * HOUR
        repository.completeGame(hard, elapsedSeconds = 500)

        assertNull(repository.savedGame())
        val stats = repository.stats().first()
        assertEquals(DifficultyStats(2, bestSeconds = 500, averageSeconds = 550), stats.byDifficulty[Difficulty.HARD])
        // Both games fall on October 23 in GMT-5, though they straddle midnight UTC.
        assertEquals(1, stats.longestStreak)
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun theDailyChallengeIsSavedAndCompletedApartFromTheRegularGame() = runBlocking {
        val repository = repository()
        val regular = SavedGame(repository.randomPuzzle(Difficulty.EASY), ENTRIES, elapsedSeconds = 1)
        val daily = SavedGame(repository.dailyPuzzle(DAY), ENTRIES, elapsedSeconds = 2, notes = NOTES, dailyDay = DAY)

        repository.save(regular)
        repository.save(daily)
        assertEquals(regular, repository.savedGame())
        assertEquals(daily, repository.savedDailyGame(DAY))
        assertNull(repository.savedDailyGame(DAY + 1))

        repository.completeGame(daily.puzzle, elapsedSeconds = 300, dailyDay = DAY)
        assertNull(repository.savedDailyGame(DAY))
        assertEquals(regular, repository.savedGame())
        assertTrue(repository.hasSavedGame().first())
    }

    @Test
    fun dailyPuzzlesRepeatOnlyAfterEveryPuzzleHasComeUp() = runBlocking {
        val repository = repository()

        val ids = (DAY until DAY + PUZZLE_TOTAL).map { repository.dailyPuzzle(it).id }

        assertEquals(PUZZLE_TOTAL, ids.toSet().size)
        assertEquals(ids.first(), repository.dailyPuzzle(DAY + PUZZLE_TOTAL).id)
    }

    @Test
    fun solvingTodaysChallengeShowsInTheStats() = runBlocking {
        val repository = repository(now = { OCT_23_UTC + HOUR }, timeZone = TimeZone.getTimeZone("UTC"))
        val today = repository.today()

        repository.completeGame(repository.dailyPuzzle(today), elapsedSeconds = 300, dailyDay = today)

        val stats = repository.stats().first()
        assertTrue(stats.dailySolvedToday)
        assertEquals(1, stats.dailyCurrentStreak)
    }

    @Test
    fun upgradeFromVersion2KeepsTheGameWithNoNotes() = runBlocking {
        SudroidDatabase.build(context, NAME).apply { openHelper.writableDatabase }.close()
        val pattern = SQLiteDatabase.openDatabase(context.getDatabasePath(NAME).path, null, 0).use { db ->
            db.execSQL("DROP TABLE saved_game")
            db.execSQL("DROP TABLE completed_games")
            db.execSQL(
                "CREATE TABLE `saved_game` (`slot` INTEGER NOT NULL, `puzzle_id` INTEGER NOT NULL, " +
                    "`entries` TEXT NOT NULL, `elapsed_seconds` INTEGER NOT NULL, PRIMARY KEY(`slot`))",
            )
            db.execSQL("INSERT INTO saved_game VALUES (0, 12, '$ENTRIES', 30)")
            db.version = 2
            db.rawQuery("SELECT digits FROM puzzles WHERE id = 12", null).use { it.moveToFirst(); it.getString(0) }
        }

        val repository = repository()

        assertEquals(
            SavedGame(Puzzle(12, Difficulty.EASY, pattern), ENTRIES, elapsedSeconds = 30, notes = ""),
            repository.savedGame(),
        )
        assertNull(repository.savedDailyGame(DAY))
    }

    @Test
    fun upgradeFromVersion4KeepsTheGameAndTheStats() = runBlocking {
        SudroidDatabase.build(context, NAME).apply { openHelper.writableDatabase }.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(NAME).path, null, 0).use { db ->
            db.execSQL("DROP TABLE saved_game")
            db.execSQL("DROP TABLE completed_games")
            db.execSQL(
                "CREATE TABLE `saved_game` (`slot` INTEGER NOT NULL, `puzzle_id` INTEGER NOT NULL, " +
                    "`entries` TEXT NOT NULL, `elapsed_seconds` INTEGER NOT NULL, `notes` TEXT NOT NULL DEFAULT '', " +
                    "PRIMARY KEY(`slot`))",
            )
            db.execSQL(
                "CREATE TABLE `completed_games` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`puzzle_id` INTEGER NOT NULL, `difficulty` TEXT NOT NULL, `elapsed_seconds` INTEGER NOT NULL, " +
                    "`completed_at` INTEGER NOT NULL)",
            )
            db.execSQL("INSERT INTO saved_game VALUES (0, 12, '$ENTRIES', 30, '$NOTES')")
            db.execSQL("INSERT INTO completed_games (puzzle_id, difficulty, elapsed_seconds, completed_at) VALUES (5, 'EASY', 90, 0)")
            db.version = 4
        }
        val repository = repository()

        assertEquals(NOTES, repository.savedGame()?.notes)
        assertNull(repository.savedDailyGame(DAY))
        val stats = repository.stats().first()
        assertEquals(1, stats.completed)
        assertEquals(0, stats.dailyLongestStreak)
    }

    @Test
    fun upgradeFromVersion5AddsTheGeneratedPuzzlesAndKeepsTheGame() = runBlocking {
        SudroidDatabase.build(context, NAME).apply { openHelper.writableDatabase }.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(NAME).path, null, 0).use { db ->
            db.execSQL("DELETE FROM puzzles WHERE id > 450")
            createVersion5Games(db)
            db.execSQL("INSERT INTO saved_game VALUES (0, 12, '$ENTRIES', 30, '$NOTES', NULL)")
            db.version = 5
        }
        val repository = repository()

        assertEquals(PUZZLE_COUNTS, puzzleCounts())
        assertEquals(12, repository.savedGame()?.puzzle?.id)
        val (id, difficulty, digits) = context.assets.open("puzzles.txt").bufferedReader().readLine().split(',')
        val first = checkNotNull(database).openHelper.readableDatabase
            .query("SELECT difficulty, digits FROM puzzles WHERE id = ?", arrayOf(id.toInt()))
            .use { it.moveToFirst(); it.getString(0) to it.getString(1) }
        assertEquals(difficulty to digits, first)
    }

    @Test
    fun upgradeFromVersion6KeepsGamesAndCountsNoHints() = runBlocking {
        SudroidDatabase.build(context, NAME).apply { openHelper.writableDatabase }.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(NAME).path, null, 0).use { db ->
            createVersion5Games(db)
            db.execSQL("INSERT INTO saved_game VALUES (0, 12, '$ENTRIES', 30, '$NOTES', NULL)")
            db.execSQL("INSERT INTO completed_games (puzzle_id, difficulty, elapsed_seconds, completed_at) VALUES (5, 'EASY', 90, 0)")
            db.version = 6
        }
        val repository = repository()

        assertEquals(0, repository.savedGame()?.hints)
        assertEquals(DifficultyStats(1, bestSeconds = 90, averageSeconds = 90), repository.stats().first().byDifficulty[Difficulty.EASY])
    }

    @Test
    fun hintsAreSavedAndKeepHintedTimesOutOfTheRecords() = runBlocking {
        val repository = repository()
        val easy = repository.randomPuzzle(Difficulty.EASY)
        repository.save(SavedGame(easy, ENTRIES, elapsedSeconds = 1, hints = 2))
        assertEquals(2, repository.savedGame()?.hints)

        repository.completeGame(easy, elapsedSeconds = 100, hints = 2)
        repository.completeGame(easy, elapsedSeconds = 300)

        val stats = repository.stats().first().byDifficulty.getValue(Difficulty.EASY)
        assertEquals(DifficultyStats(2, bestSeconds = 300, averageSeconds = 300, withoutHints = 1), stats)
    }

    /** Replaces the game tables with their version 5 and 6 shape, before the hints columns. */
    private fun createVersion5Games(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE saved_game")
        db.execSQL("DROP TABLE completed_games")
        db.execSQL(
            "CREATE TABLE `saved_game` (`slot` INTEGER NOT NULL, `puzzle_id` INTEGER NOT NULL, `entries` TEXT NOT NULL, " +
                "`elapsed_seconds` INTEGER NOT NULL, `notes` TEXT NOT NULL DEFAULT '', `daily_day` INTEGER, PRIMARY KEY(`slot`))",
        )
        db.execSQL(
            "CREATE TABLE `completed_games` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `puzzle_id` INTEGER NOT NULL, " +
                "`difficulty` TEXT NOT NULL, `elapsed_seconds` INTEGER NOT NULL, `completed_at` INTEGER NOT NULL, `daily_day` INTEGER)",
        )
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

    private fun repository(
        now: () -> Long = System::currentTimeMillis,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): RoomGameRepository {
        val db = SudroidDatabase.build(context, NAME).also { database = it }
        return RoomGameRepository(db.gameDao(), now) { timeZone }
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
        // 150 per difficulty from data.txt and 150 from puzzles.txt.
        val PUZZLE_COUNTS = mapOf("EASY" to 300, "MEDIUM" to 300, "HARD" to 300)
        const val PUZZLE_TOTAL = 900
        const val HOUR = 60 * 60 * 1000L
        const val OCT_23_UTC = 20_749 * 24 * HOUR
        const val DAY = 20_749L
    }
}

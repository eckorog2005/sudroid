# Modernization progress

The plan for turning the 2012 app into a modern Android game, step by step. Tick items as their PRs merge and link the PR.

## Where we left off (2026-10-24)

- Steps 1 and 2 are merged. Step 3 is in progress, one branch per feature:
  1. Done on `feat/core-play-features`: undo and redo, held in memory by `GameViewModel` (the history is lost when the game screen closes).
  2. Done and merged in [#7](https://github.com/eckorog2005/sudroid/pull/7): notes, stored in `Board` so undo covers them, and saved in a new `saved_game.notes` column through a Room 2→3 auto-migration, so `entries` keeps its format.
  3. Done and merged in [#8](https://github.com/eckorog2005/sudroid/pull/8): the selected cell's row, column and box get a tint, and cells with the same digit get a stronger one. It only changes drawing; `Cell.peers` is now public.
  4. Done and merged in [#9](https://github.com/eckorog2005/sudroid/pull/9): each pad button shows how many of its digit are left (`Board.remaining`) and is disabled at 0.
  5. Done and merged in [#10](https://github.com/eckorog2005/sudroid/pull/10): a global setting on the menu (always, on request, never), kept in `SharedPreferences` through `SettingsRepository`. Conflicts get a corner triangle as well as red, and on request a Check button shows them until the next move. Step 3 is complete.
  6. Step 4 in progress. Done in [#11](https://github.com/eckorog2005/sudroid/pull/11): dynamic color on Android 12+ with the static scheme as fallback, light and dark following the system, and a day/night window theme so the launch frame matches. Then done on `feat/large-screens`: the board and pad are capped in width and centered, so tablets, foldables and resized windows no longer stretch them; the layout follows the window size, not the device. Then done on `feat/accessibility`: each cell is a tap target with a spoken description (position, digit or notes, given, conflict) and a selected state, the pad buttons read as "digit, count left", pad labels shrink to fit instead of clipping at large font sizes, and the mistake setting is a radio list that scales with the font. Then done on `feat/adaptive-icon`: a drawn vector icon with a monochrome layer, replacing the 2010 raster icon. Then done on `feat/splash-screen`: the SplashScreen API through `core-splashscreen`, showing the icon's grid on the icon's indigo and handing over to `Theme.Sudroid`. Step 4 is complete.
  7. Step 5 in progress. Done and merged in [#18](https://github.com/eckorog2005/sudroid/pull/18): each solved game is recorded in a new `completed_games` table (Room 3→4 auto-migration), and a Statistics screen from the menu shows games completed, the current and longest streak of days with a finished game, and the count, best and average time per difficulty. The daily challenge can build its streaks on the same table.
  8. Done and merged in [#19](https://github.com/eckorog2005/sudroid/pull/19): a Daily Challenge button on the menu opens the same puzzle for everyone on a given day, stepping through all 450 puzzles by a prime stride before any repeats. It saves in its own `saved_game` slot, so it never overwrites the regular game, and keeps its day across process death, so a challenge started before midnight resumes as the same one. Solving it records the challenge's day in `completed_games.daily_day` (Room 4→5 auto-migration); the menu shows "Solved today" and the daily streak, and the Statistics screen shows the current and longest daily streak.
  9. Done and merged in [#20](https://github.com/eckorog2005/sudroid/pull/20): a Hint button reveals one cell, using `Solver.hint`, which solves from the givens alone, and selects it. The hint is an ordinary move, so undo takes it back; hints are not counted or recorded yet.
  10. Done and merged in [#21](https://github.com/eckorog2005/sudroid/pull/21): each hint says why its digit goes there, in a snackbar the next move dismisses. `Hint.Reason` is a corrected mistake, a naked single, a hidden single in the cell's row, column or box, or, when no single applies, the solution. Without a selection a hint prefers a wrong entry, then a naked single, then a hidden single. Harder techniques (pairs, pointing, X-wing) fall back to the solution for now; difficulty grading can build on the same detection.
  11. Done and merged in [#22](https://github.com/eckorog2005/sudroid/pull/22): 450 generated puzzles (IDs 451 to 900) join the original 450, graded by `Grader` by the technique they need: naked singles for easy, hidden singles for medium, locked candidates for hard. `./gradlew :engine:generatePuzzles` appends more to `puzzles.txt`, and Room version 6 inserts them on existing installs. The original puzzles keep their labels, and the daily challenge now steps through all 900, so each day maps to a different puzzle than before. Step 5 is complete.

## 1. Port the engine to Kotlin with tests: done

Merged 2026-10-11 in [#2](https://github.com/eckorog2005/sudroid/pull/2).

- [x] Pure-Kotlin `:engine` module: immutable `Board`, `Cell`, `Solver`
- [x] 23 JVM unit tests, including a check that all 450 bundled puzzles have exactly one solution
- [x] Old Java `Board`, `Cell`, `Position` and exception classes removed; `SudroidView` uses the engine
- [x] Fixed: a stale conflict flag could stop a correctly filled board from registering as finished
- [x] Played through on an Android 17 emulator

## 2. Game screen in Compose with a ViewModel and Room: done

Merged 2026-10-12 in [#4](https://github.com/eckorog2005/sudroid/pull/4).

Architecture:

- [x] One Activity with Compose Navigation (menu, game, results, and stats from step 5)
- [x] `ViewModel` holding a `StateFlow` of the game state
- [x] Room for saved games, with an autosave after every move
- [x] Board drawn with a Compose `Canvas`, sized from the screen instead of fixed pixels and the grid JPEG
- [x] Timer as a coroutine tied to the screen lifecycle, replacing `java.util.Timer` and the Handler messages

Existing bugs this step fixes:

- [x] Any configuration change (rotation, dark mode, window resize) starts a new puzzle
- [x] Pressing Back loses progress (`saveGame()` is empty)
- [x] A finished puzzle can come back as "Resume"
- [x] Finishing a puzzle crashes with a `NullPointerException` at `mTimer.cancel()` and can open the score screen twice (confirmed on the emulator, on `main` as well)
- [x] "Back to Main" stacks a new menu instead of closing the screen
- [x] `Thread.sleep(100)` blocks the main thread in the View constructor
- [x] Unclosed cursors, a database closed before its cursor is read, SQL built by string concatenation

## 3. Core play features

- [x] Pencil marks (notes), cleared automatically when a digit is placed in the same row, column or box
- [x] Undo and redo (also makes Reset safe; it currently has no confirmation)
- [x] Highlight the selected cell's row, column and box, and every cell with the same digit
- [x] Number pad shows how many of each digit are left and dims finished digits
- [x] Mistake modes: show immediately, only on request, or never; mark errors with a shape as well as color

## 4. Platform and visuals

- [x] Material 3 with dynamic color and dark mode
- [x] Layouts for tablets, foldables and landscape (the portrait lock is ignored on large screens since Android 16)
- [x] Accessibility: a TalkBack description for each cell, and text that follows the system font size
- [x] Adaptive launcher icon with a monochrome layer
- [x] SplashScreen API

## 5. Retention features

- [x] Daily challenge chosen from the date, with streaks ([#19](https://github.com/eckorog2005/sudroid/pull/19))
- [x] Stats: best and average time per difficulty, games completed, streaks ([#18](https://github.com/eckorog2005/sudroid/pull/18))
- [x] Hints built on `Solver`: reveal a cell ([#20](https://github.com/eckorog2005/sudroid/pull/20)), then explain the technique (naked and hidden singles, [#21](https://github.com/eckorog2005/sudroid/pull/21))
- [x] More puzzles: generate them with the uniqueness check and grade difficulty by technique, or bundle a larger set (generated and bundled; the original 450 keep their labels, [#22](https://github.com/eckorog2005/sudroid/pull/22))

## Unscheduled

- [ ] Pause screen that hides the board
- [ ] Haptics on input, an animation when a row, column or box is completed, and a celebration on finishing
- [x] `.gitattributes` to keep the CRLF line endings in `data.txt` (and `gradlew.bat`), marked `-text` so no checkout converts them

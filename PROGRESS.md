# Modernization progress

The plan for turning the 2012 app into a modern Android game, step by step. Tick items as their PRs merge and link the PR.

## Where we left off (2026-10-12)

- Steps 1 and 2 are merged. Step 3 is in progress, one branch per feature:
  1. Done on `feat/core-play-features`: undo and redo, held in memory by `GameViewModel` (the history is lost when the game screen closes).
  2. Done on `feat/pencil-marks` (branched from the undo branch): notes, stored in `Board` so undo covers them, and saved in a new `saved_game.notes` column through a Room 2→3 auto-migration, so `entries` keeps its format. Checked on the emulator in portrait and landscape.
  3. Next highlighting and number pad counts, which only change the UI, and mistake modes last, since they need a setting.

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

- [x] One Activity with Compose Navigation (menu, game, results; stats arrives with step 5)
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
- [ ] Highlight the selected cell's row, column and box, and every cell with the same digit
- [ ] Number pad shows how many of each digit are left and dims finished digits
- [ ] Mistake modes: show immediately, only on request, or never; mark errors with a shape as well as color

## 4. Platform and visuals

- [ ] Material 3 with dynamic color and dark mode
- [ ] Layouts for tablets, foldables and landscape (the portrait lock is ignored on large screens since Android 16)
- [ ] Accessibility: a TalkBack description for each cell, and text that follows the system font size
- [ ] Adaptive launcher icon with a monochrome layer
- [ ] SplashScreen API

## 5. Retention features

- [ ] Daily challenge chosen from the date, with streaks
- [ ] Stats: best and average time per difficulty, games completed, streaks
- [ ] Hints built on `Solver`: reveal a cell first, explain the technique later
- [ ] More puzzles: generate them with the uniqueness check and grade difficulty by technique, or bundle a larger set

## Unscheduled

- [ ] Pause screen that hides the board
- [ ] Haptics on input, an animation when a row, column or box is completed, and a celebration on finishing
- [ ] `.gitattributes` to keep the CRLF line endings in `data.txt`

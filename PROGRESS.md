# Modernization progress

The plan for turning the 2012 app into a modern Android game, step by step. Tick items as their PRs merge and link the PR.

## Where we left off (2026-10-12)

- Step 1 is merged. Step 2 is complete on `feat/compose-game-screen` and ready for a PR. Step 3 is next.
- Step 2 order:
  1. Done: add Compose, Lifecycle/ViewModel, Navigation and Room to the version catalog, plus the Kotlin Compose compiler plugin at the same Kotlin version (2.4.21).
  2. Done: build the game screen first: a `ViewModel` holding the engine `Board`, a `Canvas` board, the number pad, and the timer.
  3. Done: saving moved to Room (`RoomGameRepository`). A 1→2 migration on the same `SudroidDB` file converts the old tables, so in-progress games survive the upgrade (checked on the emulator by upgrading from `main`).
  4. Done: one `SudroidActivity` with Navigation 3 over the Compose menu, game and results screens. All Java and XML layouts are gone.
- Verified on the emulator: start, rotate mid-game, Back mid-game and Resume, process death (from Recents), finish, Back to Main, and Back from the menu exits.

## 1. Port the engine to Kotlin with tests: done

Merged 2026-10-11 in [#2](https://github.com/eckorog2005/sudroid/pull/2).

- [x] Pure-Kotlin `:engine` module: immutable `Board`, `Cell`, `Solver`
- [x] 23 JVM unit tests, including a check that all 450 bundled puzzles have exactly one solution
- [x] Old Java `Board`, `Cell`, `Position` and exception classes removed; `SudroidView` uses the engine
- [x] Fixed: a stale conflict flag could stop a correctly filled board from registering as finished
- [x] Played through on an Android 17 emulator

## 2. Game screen in Compose with a ViewModel and Room: done, PR pending

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

- [ ] Pencil marks (notes), cleared automatically when a digit is placed in the same row, column or box
- [ ] Undo and redo (also makes Reset safe; it currently has no confirmation)
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

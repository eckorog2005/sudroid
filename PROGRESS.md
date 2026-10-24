# Modernization progress

The plan for turning the 2012 app into a modern Android game, step by step. Tick items as their PRs merge and link the PR.

## Current state (2026-10-24)

Every step below is merged, along with the unscheduled items and two follow-ups. The app targets Android 17 (API 37) since [#1](https://github.com/eckorog2005/sudroid/pull/1), and Room is at version 7.

Notes that the code and AGENTS.md don't spell out:

- Undo and redo history lives in memory in `GameViewModel`, so it is lost when the game screen closes.
- [#22](https://github.com/eckorog2005/sudroid/pull/22) changed the number of puzzles, so since then each day's challenge maps to a different puzzle than before.
- Hints explain naked singles, hidden singles and chains of locked candidates. Anything harder falls back to "from the solution". No generated puzzle needs that, but 28 of the original 450 do (1 easy, 10 medium, 17 hard).
- The instrumented database tests pass on the `sudroid_pixel9` emulator (API 37). Haptics and the confetti haven't been checked on a physical phone.

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

## 3. Core play features: done

Merged 2026-10-13 to 2026-10-14.

- [x] Pencil marks (notes), cleared automatically when a digit is placed in the same row, column or box ([#7](https://github.com/eckorog2005/sudroid/pull/7); stored in `Board` so undo covers them)
- [x] Undo and redo (also makes Reset safe; it currently has no confirmation) ([#5](https://github.com/eckorog2005/sudroid/pull/5))
- [x] Highlight the selected cell's row, column and box, and every cell with the same digit ([#8](https://github.com/eckorog2005/sudroid/pull/8))
- [x] Number pad shows how many of each digit are left and dims finished digits ([#9](https://github.com/eckorog2005/sudroid/pull/9))
- [x] Mistake modes: show immediately, only on request, or never; mark errors with a shape as well as color ([#10](https://github.com/eckorog2005/sudroid/pull/10); a corner triangle, and a Check button on request)

## 4. Platform and visuals: done

Merged 2026-10-14; [#12](https://github.com/eckorog2005/sudroid/pull/12) to [#15](https://github.com/eckorog2005/sudroid/pull/15) landed together in [#16](https://github.com/eckorog2005/sudroid/pull/16).

- [x] Material 3 with dynamic color and dark mode ([#11](https://github.com/eckorog2005/sudroid/pull/11))
- [x] Layouts for tablets, foldables and landscape (the portrait lock is ignored on large screens since Android 16) ([#12](https://github.com/eckorog2005/sudroid/pull/12); the layout follows the window size, not the device)
- [x] Accessibility: a TalkBack description for each cell, and text that follows the system font size ([#13](https://github.com/eckorog2005/sudroid/pull/13))
- [x] Adaptive launcher icon with a monochrome layer ([#14](https://github.com/eckorog2005/sudroid/pull/14))
- [x] SplashScreen API ([#15](https://github.com/eckorog2005/sudroid/pull/15))

## 5. Retention features: done

Merged 2026-10-24.

- [x] Daily challenge chosen from the date, with streaks ([#19](https://github.com/eckorog2005/sudroid/pull/19); its own save slot, and streaks count the challenge's day)
- [x] Stats: best and average time per difficulty, games completed, streaks ([#18](https://github.com/eckorog2005/sudroid/pull/18))
- [x] Hints built on `Solver`: reveal a cell ([#20](https://github.com/eckorog2005/sudroid/pull/20)), then explain the technique (naked and hidden singles, [#21](https://github.com/eckorog2005/sudroid/pull/21))
- [x] More puzzles: generate them with the uniqueness check and grade difficulty by technique, or bundle a larger set (generated and bundled; the original 450 keep their labels, [#22](https://github.com/eckorog2005/sudroid/pull/22))

## Unscheduled: done

Merged 2026-10-24.

- [x] Pause screen that hides the board ([#25](https://github.com/eckorog2005/sudroid/pull/25); stops the clock and disables the controls, and survives process death)
- [x] Haptics on input, an animation when a row, column or box is completed, and a celebration on finishing ([#26](https://github.com/eckorog2005/sudroid/pull/26); a fading flash over each completed house, and confetti on the results screen)
- [x] `.gitattributes` to keep the CRLF line endings in `data.txt` ([#24](https://github.com/eckorog2005/sudroid/pull/24); `gradlew.bat` too, both marked `-text`)

## Follow-ups

- [x] Hints explain locked candidates, chaining steps until one reveals a single ([#27](https://github.com/eckorog2005/sudroid/pull/27))
- [x] Count hints: hinted games still count as completed and keep streaks, but only hint-free games set best and average times ([#28](https://github.com/eckorog2005/sudroid/pull/28))
- [x] Game screen polish: a top bar with Back, the difficulty or daily date, and the timer; icons on the action buttons; Reset asks first; givens are never marked as mistakes; the menu's mistake options are centered

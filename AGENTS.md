# AGENTS.md

Sudroid is a GPL-3 Sudoku game for Android that started as a 2010-era Java app. It is being modernized in stages: game rules live in the Kotlin `:engine` module, and the Java UI in `:app` is due to be rewritten in Kotlin and Jetpack Compose.

The roadmap and its status are in `PROGRESS.md`. Read it before starting modernization work, and tick items there in the same PR that completes them.

## Commands

- `./gradlew :engine:test` runs the engine unit tests on the plain JVM, so no device is needed.
- A single test: `./gradlew :engine:test --tests 'com.rlamb.android.sudroid.engine.BoardTest.reset*'`. Test names contain spaces, so match them with a wildcard.
- `./gradlew assembleDebug assembleRelease lint` builds both variants and lints. Only the release build runs R8 minification.
- `./gradlew installDebug` installs on a connected device or emulator.
- Building needs JDK 17 or newer. `:app` has no tests yet.

## Modules

- `:engine` is pure Kotlin on the JVM, with no Android dependencies.
  - `Board` is immutable: every move returns a new board. Conflicts are worked out from the grid on each call, never stored.
  - `Cell` uses 0-based rows and columns.
  - `Solver` does bitmask backtracking: `solve`, `countSolutions` and `hasUniqueSolution`.
  - While `:app` is still Java, make the public API callable from Java with `@JvmStatic`, `@JvmField` and `@JvmOverloads`.
- `:app` is Java: three Activities (menu, game, score) and `SudroidView`, a custom `View`. The view holds the current `Board`, draws it on a `Canvas`, runs the timer, and checks for a win inside `onDraw`.

## Puzzle data and saves

- Puzzles ship as SQL statements in `app/src/main/assets/data.txt`, 150 each for easy, medium and hard. `SudroidDatabaseHelper.onCreate` runs them on first launch.
- The database is at version 1 and `onUpgrade` is empty, so edits to `data.txt` only reach fresh installs. To reach existing installs, bump the version and implement `onUpgrade`.
- Puzzle strings are 81 digits in row-major order, with `0` for an empty cell. The saved game (`Current.userEntered`) uses the same format with givens written as `0`; see `Board.entriesString()` and `Board.withEntries()`. Keep this format compatible so existing saved games still resume.
- `SolverTest` reads `data.txt`, whose path comes in through the `sudroid.puzzleData` system property, and requires every bundled puzzle to have exactly one solution.

## Gotchas

- **Line endings:** `data.txt` must stay CRLF, because `SudroidDatabaseHelper` splits the statements on `\r\n`. Most Java files in `:app` are CRLF as well. Preserve each file's existing line endings when editing, since no `.gitattributes` enforces them.
- **Saving:** the game is saved only through `SudroidView.onSaveInstanceState`; `saveGame()` is empty. As a result, pressing Back loses progress, and any configuration change (rotation, toggling dark mode) starts a new puzzle.
- **Crash on finish:** finishing a puzzle crashes with a `NullPointerException` at `mTimer.cancel()`. The win check in `onDraw` keeps running after `pauseTimer()` has set `mTimer` to null, and it can also open the score screen twice. This bug predates the engine port, so expect it during on-device testing.
- **Orientation:** Android 16 and later ignore `screenOrientation="portrait"` on large screens, so the game rotates on tablets and foldables.

## Conventions

- Commit messages follow Conventional Commits, for example `refactor(engine): ...` or `build!: ...`.
- Source files carry the GPL-3 license header; copy it from an existing file.

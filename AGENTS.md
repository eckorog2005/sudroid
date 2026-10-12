# AGENTS.md

Sudroid is a GPL-3 Sudoku game for Android that started as a 2010-era Java app. It is being modernized in stages: game rules live in the Kotlin `:engine` module, the game screen is Kotlin and Jetpack Compose, and the remaining Java UI in `:app` is due to be rewritten.

The roadmap and its status are in `PROGRESS.md`. Read it before starting modernization work, and tick items there in the same PR that completes them.

## Commands

- `./gradlew :engine:test` runs the engine unit tests on the plain JVM, so no device is needed.
- A single test: `./gradlew :engine:test --tests 'com.rlamb.android.sudroid.engine.BoardTest.reset*'`. Test names contain spaces, so match them with a wildcard.
- `./gradlew assembleDebug assembleRelease lint` builds both variants and lints. Only the release build runs R8 minification.
- `./gradlew installDebug` installs on a connected device or emulator.
- `./gradlew :app:testDebugUnitTest` runs the `:app` JVM unit tests, such as `GameViewModelTest`.
- Building needs JDK 17 or newer.

## Modules

- `:engine` is pure Kotlin on the JVM, with no Android dependencies.
  - `Board` is immutable: every move returns a new board. Conflicts are worked out from the grid on each call, never stored.
  - `Cell` uses 0-based rows and columns.
  - `Solver` does bitmask backtracking: `solve`, `countSolutions` and `hasUniqueSolution`.
  - While `:app` is still Java, make the public API callable from Java with `@JvmStatic`, `@JvmField` and `@JvmOverloads`.
- `:app` mixes Kotlin (`src/main/kotlin`) and Java (`src/main/java`).
  - `game.GameActivity` hosts the Compose `GameScreen`. `GameViewModel` holds a `StateFlow<GameUiState>`, autosaves after every move, and counts time in `runClock()`, which the screen runs only while resumed.
  - Storage goes through the `GameRepository` interface. `data.LegacyGameRepository` implements it on the original SQLite schema until Room replaces it.
  - The menu (`SudroidActivity`) and score screen (`SudroidHighScoreActivity`) are still Java Activities. The menu starts the game with a `GameModeEnum` ordinal in `GameActivity.EXTRA_MODE`.

## Puzzle data and saves

- Puzzles ship as SQL statements in `app/src/main/assets/data.txt`, 150 each for easy, medium and hard. `SudroidDatabaseHelper.onCreate` runs them on first launch.
- The database is at version 1 and `onUpgrade` is empty, so edits to `data.txt` only reach fresh installs. To reach existing installs, bump the version and implement `onUpgrade`.
- Puzzle strings are 81 digits in row-major order, with `0` for an empty cell. The saved game (`Current.userEntered`) uses the same format with givens written as `0`; see `Board.entriesString()` and `Board.withEntries()`. Keep this format compatible so existing saved games still resume.
- `SolverTest` reads `data.txt`, whose path comes in through the `sudroid.puzzleData` system property, and requires every bundled puzzle to have exactly one solution.

## Gotchas

- **Line endings:** `data.txt` must stay CRLF, because `SudroidDatabaseHelper` splits the statements on `\r\n`. Most Java files in `:app` are CRLF as well. Preserve each file's existing line endings when editing, since no `.gitattributes` enforces them.
- **Old saves:** the old game wrote `resume` into `Current.difficulty` for resumed games and could store times such as `00:0-1`. Read the difficulty from the `Puzzle` table and parse times leniently (`parseElapsed`).
- **Orientation:** Android 16 and later ignore `screenOrientation="portrait"` on large screens, so the menu and score screens rotate on tablets and foldables. The game screen is unlocked and has a landscape layout.

## Conventions

- Commit messages follow Conventional Commits, for example `refactor(engine): ...` or `build!: ...`.
- Source files carry the GPL-3 license header; copy it from an existing file.

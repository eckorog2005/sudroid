# AGENTS.md

Sudroid is a GPL-3 Sudoku game for Android that started as a 2010-era Java app. It is being modernized in stages: game rules live in the Kotlin `:engine` module, and the app in `:app` is Kotlin and Jetpack Compose.

The roadmap and its status are in `PROGRESS.md`. Read it before starting modernization work, and tick items there in the same PR that completes them.

## Commands

- `./gradlew :engine:test` runs the engine unit tests on the plain JVM, so no device is needed.
- A single test: `./gradlew :engine:test --tests 'com.rlamb.android.sudroid.engine.BoardTest.reset*'`. Test names contain spaces, so match them with a wildcard.
- `./gradlew assembleDebug assembleRelease lint` builds both variants and lints. Only the release build runs R8 minification.
- `./gradlew installDebug` installs on a connected device or emulator.
- `./gradlew :engine:generatePuzzles -Pcount=150` appends that many new puzzles per difficulty to `app/src/main/assets/puzzles.txt`. It runs from the engine's `tools` source set, which the app doesn't include.
- `./gradlew :app:testDebugUnitTest` runs the `:app` JVM unit tests, such as `GameViewModelTest`.
- `./gradlew :app:connectedDebugAndroidTest` runs the instrumented tests, such as `SudroidDatabaseTest`, on a connected device or emulator.
- Building needs JDK 17 or newer.

## Modules

- `:engine` is pure Kotlin on the JVM, with no Android dependencies.
  - `Board` is immutable: every move returns a new board. Conflicts are worked out from the grid on each call, never stored.
  - `Cell` uses 0-based rows and columns. `Cell.HOUSES` lists the 27 rows, columns and boxes; `Board.isComplete(house)` is true when one is full with no repeat.
  - `Solver` does bitmask backtracking: `solve`, `countSolutions` and `hasUniqueSolution`.
  - `Grader` solves the way a person does (naked singles, hidden singles, locked candidates) and returns the hardest `Technique` needed, or null. `Generator` digs clues out of a random full grid while the puzzle stays unique and no harder than its target. `hint` solves from the givens alone, so it can correct wrong entries, and returns a `Hint` whose `Reason` names the technique (`Hint.kt`), up to a chain of locked-candidates steps trimmed to the ones its single depends on. `Candidates.kt` holds the candidate masks and locked-candidates eliminations that `Grader` and hints share.
- `:app` is Kotlin and Jetpack Compose, in `src/main/kotlin`.
  - `SudroidActivity` is the only Activity; it keeps its original name so existing launcher shortcuts still work. `SudroidApp` runs Navigation 3 over the `Menu`, `Game(request)` (`New(difficulty)`, `Resume` or `Daily`), `Results(elapsedSeconds)` and `Statistics` keys. Finishing a game replaces `Game` with `Results`, so Back never returns to a solved board.
  - `GameViewModel` holds a `StateFlow<GameUiState>`, autosaves after every move, and counts time in `runClock()`, which the screen runs only while resumed. Pausing hides the board behind `PausedPanel`, stops the clock and ignores moves; the flag lives in its `SavedStateHandle`.
  - Solving a game calls `GameRepository.completeGame`, which records it in `completed_games` and deletes the save in one transaction. `stats.statsOf` turns those rows into per-difficulty counts, best and average times, and day streaks; days are local calendar days in the current time zone.
  - Storage goes through the `GameRepository` and `StatsRepository` interfaces, implemented by `data.RoomGameRepository` on the Room `SudroidDatabase`. Saves run in a process-wide scope passed to `GameViewModel`, so the last save or delete lands after the screen closes.
  - The back stack survives process death, so a restored `Game(New(...))` key would start over. `GameViewModel` marks the game as started in its `SavedStateHandle` and resumes the autosave instead.
  - The daily challenge's puzzle comes from the day (`dailyPuzzleIndex`). It saves in its own `saved_game` slot (1; the regular game is slot 0) tagged with its day, and `GameViewModel` keeps that day in its `SavedStateHandle`, so a challenge started before midnight resumes as the same one. Daily streaks count `completed_games.daily_day`, the challenge's day, not the day it was finished.

## Puzzle data and saves

- Puzzles ship as SQL statements in `app/src/main/assets/data.txt`, 150 each for easy, medium and hard. They build the original version 1 schema (`Puzzle`, `Current`, ...). Their difficulty labels predate `Grader` and are kept as they are.
- Generated puzzles ship in `app/src/main/assets/puzzles.txt` as `id,DIFFICULTY,digits` lines, IDs 451 on: easy needs only naked singles (36 clues), medium needs hidden singles (32), hard needs locked candidates (28). Only append to it, never rewrite or reorder lines, since installs have saved and finished those puzzles by ID.
- Room keeps the old file name, `SudroidDB`, now at version 6. `MIGRATION_1_2` converts the version 1 tables into `puzzles` and `saved_game` and drops them; auto-migrations add `saved_game.notes` in version 3, the `completed_games` table in version 4, and `daily_day` on both `saved_game` and `completed_games` in version 5; version 6 inserts `puzzles.txt` (`AddGeneratedPuzzles`). Fresh installs run `data.txt`, the same conversion, and then insert `puzzles.txt`, so puzzle IDs match in both paths.
- Edits to `data.txt` only reach fresh installs. To reach existing installs, bump the database version and add a migration. After appending to `puzzles.txt`, bump the version and add another `AddGeneratedPuzzles` migration; it inserts with `INSERT OR IGNORE`.
- Changing the number of puzzles changes which puzzle each day's challenge gets (`dailyPuzzleIndex`); a challenge in progress still resumes, since its save holds the puzzle ID. Room exports each schema to `app/schemas`; commit it.
- Puzzle strings are 81 digits in row-major order, with `0` for an empty cell. The saved game (`saved_game.entries`, formerly `Current.userEntered`) uses the same format with givens written as `0`; see `Board.entriesString()` and `Board.withEntries()`. Pencil marks go in `saved_game.notes` as 81 comma-separated digit groups (`Board.notesString()`), empty for saves from before version 3. Keep this format compatible so existing saved games still resume.
- `SolverTest` and `GraderTest` read `data.txt` and `puzzles.txt`, whose paths come in through the `sudroid.puzzleData` and `sudroid.generatedPuzzles` system properties. Every puzzle must have exactly one solution, none may repeat, and each generated puzzle must grade as its difficulty.

## Gotchas

- **Line endings:** `data.txt` is CRLF. The loader reads `data.txt` line by line, so either ending works, but keep it CRLF to match what old installs ran. `.gitattributes` marks it and `gradlew.bat` `-text`, so Git never converts them; other files have no rule, so preserve each file's existing line endings when editing.
- **Old saves:** the old game wrote `resume` into `Current.difficulty` for resumed games and could store times such as `00:0-1`. `MIGRATION_1_2` takes the difficulty from the `Puzzle` table and parses times leniently (`parseElapsed`).
- **Orientation:** every screen rotates. The game screen has a separate landscape layout; the menu and results screens scroll if they don't fit.

## Conventions

- Commit messages follow Conventional Commits, for example `refactor(engine): ...` or `build!: ...`.
- Source files carry the GPL-3 license header; copy it from an existing file.

package com.rlamb.android.sudroid.engine

import com.rlamb.android.sudroid.engine.BoardTest.Companion.PUZZLE
import com.rlamb.android.sudroid.engine.BoardTest.Companion.SOLUTION
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolverTest {
    private val empty = Board.EMPTY

    @Test
    fun `solve fills the puzzle with its solution`() {
        val solved = assertNotNull(Solver.solve(Board.fromPuzzle(PUZZLE)))

        assertTrue(solved.isSolved)
        assertEquals(Board.fromPuzzle(PUZZLE).withEntries(SOLUTION).entriesString(), solved.entriesString())
    }

    @Test
    fun `solve keeps the player's correct entries`() {
        val played = Board.fromPuzzle(PUZZLE).place(Cell(0, 2), 4)

        assertEquals(4, assertNotNull(Solver.solve(played)).digitAt(Cell(0, 2)))
    }

    @Test
    fun `solve returns null when the board contradicts itself`() {
        assertNull(Solver.solve(Board.fromPuzzle(PUZZLE).place(Cell(0, 2), 5)))
    }

    @Test
    fun `solve returns null when a wrong entry makes the puzzle unsolvable`() {
        assertNull(Solver.solve(Board.fromPuzzle(PUZZLE).place(Cell(0, 2), 1)))
    }

    @Test
    fun `countSolutions stops at the limit`() {
        assertEquals(2, Solver.countSolutions(empty))
        assertEquals(5, Solver.countSolutions(empty, limit = 5))
        assertFailsWith<IllegalArgumentException> { Solver.countSolutions(empty, limit = 0) }
    }

    @Test
    fun `a well formed puzzle has a unique solution`() {
        assertTrue(Solver.hasUniqueSolution(Board.fromPuzzle(PUZZLE)))
        assertEquals(false, Solver.hasUniqueSolution(empty))
    }

    @Test
    fun `a hint on a wrong entry corrects it and names the wrong digit`() {
        val board = Board.fromPuzzle(PUZZLE).place(Cell(0, 2), 1)

        assertEquals(Hint(Cell(0, 2), 4, Hint.Reason.Mistake(1)), Solver.hint(board, preferred = Cell(0, 2)))
    }

    @Test
    fun `a hint explains the selected cell by the technique that finds it`() {
        val board = Board.fromPuzzle(PUZZLE)
        fun hintAt(cell: Cell) = Solver.hint(board, preferred = cell)

        assertEquals(Hint(Cell(4, 4), 5, Hint.Reason.NakedSingle), hintAt(Cell(4, 4)))
        assertEquals(Hint(Cell(2, 6), 5, Hint.Reason.HiddenSingle(Hint.House.ROW)), hintAt(Cell(2, 6)))
        assertEquals(Hint(Cell(2, 4), 4, Hint.Reason.HiddenSingle(Hint.House.COLUMN)), hintAt(Cell(2, 4)))
        assertEquals(Hint(Cell(8, 6), 1, Hint.Reason.HiddenSingle(Hint.House.BOX)), hintAt(Cell(8, 6)))
        assertEquals(Hint(Cell(0, 3), 6, Hint.Reason.FromSolution), hintAt(Cell(0, 3)))
    }

    @Test
    fun `a hint corrects a wrong entry before filling an empty cell`() {
        val board = Board.fromPuzzle(PUZZLE).place(Cell(0, 2), 4).place(Cell(8, 0), 1)

        assertEquals(Hint(Cell(8, 0), 3, Hint.Reason.Mistake(1)), Solver.hint(board, preferred = Cell(0, 2)))
    }

    @Test
    fun `without a selection a hint prefers a naked single`() {
        assertEquals(Hint(Cell(4, 4), 5, Hint.Reason.NakedSingle), Solver.hint(Board.fromPuzzle(PUZZLE)))
    }

    @Test
    fun `without a selection a hint takes a hidden single when no naked single is left`() {
        val board = Board.fromPuzzle("314080050750310086008070004000102000031060590000508000100040600260051049080020175")

        assertEquals(Hint(Cell(1, 5), 4, Hint.Reason.HiddenSingle(Hint.House.ROW)), Solver.hint(board))
    }

    @Test
    fun `when no single is left a hint explains the locked candidates that reveal one`() {
        val (board, hint) = firstHintBeyondSingles(LOCKED_CANDIDATES_PUZZLE)
        val reason = assertIs<Hint.Reason.LockedCandidates>(hint.reason)

        assertEquals(assertNotNull(Solver.solve(board)).digitAt(hint.cell), hint.digit)
        assertTrue(reason.then == Hint.Reason.NakedSingle || reason.then is Hint.Reason.HiddenSingle)
        val lock = reason.locks.single()
        val within = cellsOf(lock.within)
        val confinedTo = cellsOf(lock.confinedTo)
        val spots = within.filter { board.digitAt(it) == 0 && lock.digit in candidates(board, it) }
        assertTrue(spots.size >= 2 && confinedTo.containsAll(spots))
        assertTrue(hint.cell in confinedTo - within.toSet() || lock.digit == hint.digit)
    }

    @Test
    fun `hints explain every step of a generated hard puzzle without falling back to the solution`() {
        for (puzzle in generatedPuzzles().filter { it.difficulty == "HARD" }) {
            var board = Board.fromPuzzle(puzzle.digits)
            while (!board.isSolved) {
                val hint = assertNotNull(Solver.hint(board))
                assertTrue(hint.reason != Hint.Reason.FromSolution, "Puzzle ${puzzle.id} fell back at ${hint.cell}")
                board = board.place(hint.cell, hint.digit)
            }
        }
    }

    @Test
    fun `a hint on a selected cell uses locked candidates when no single finds it`() {
        val (board, hint) = firstHintBeyondSingles(LOCKED_CANDIDATES_PUZZLE)

        assertEquals(hint, Solver.hint(board, preferred = hint.cell))
    }

    /** Plays naked and hidden singles from the hints until a hint needs something more. */
    private fun firstHintBeyondSingles(puzzle: String): Pair<Board, Hint> {
        var board = Board.fromPuzzle(puzzle)
        while (true) {
            val hint = assertNotNull(Solver.hint(board))
            if (hint.reason != Hint.Reason.NakedSingle && hint.reason !is Hint.Reason.HiddenSingle) return board to hint
            board = board.place(hint.cell, hint.digit)
        }
    }

    private fun cellsOf(area: Hint.Area): List<Cell> = Cell.ALL.filter {
        when (area.house) {
            Hint.House.ROW -> it.row == area.index
            Hint.House.COLUMN -> it.col == area.index
            Hint.House.BOX -> it.row / 3 * 3 + it.col / 3 == area.index
        }
    }

    private fun candidates(board: Board, cell: Cell): Set<Int> = (1..Board.SIZE).toSet() - cell.peers.map(board::digitAt).toSet()

    @Test
    fun `a hint falls back to the solution when no single technique applies`() {
        val hint = assertNotNull(Solver.hint(empty))

        assertEquals(Cell(0, 0), hint.cell)
        assertEquals(Hint.Reason.FromSolution, hint.reason)
    }

    @Test
    fun `there is no hint for a solved board`() {
        assertNull(Solver.hint(assertNotNull(Solver.solve(Board.fromPuzzle(PUZZLE))), preferred = Cell(0, 0)))
    }

    @Test
    fun `every bundled puzzle has exactly one solution`() {
        val data = File(System.getProperty("sudroid.puzzleData")).readText()
        val puzzles = PUZZLE_ROW.findAll(data).map { it.groupValues[1] to it.groupValues[2] }.toList()

        assertEquals(mapOf("easy" to 150, "medium" to 150, "hard" to 150), puzzles.groupingBy { it.first }.eachCount())
        val ambiguous = puzzles.filterNot { (_, pattern) -> Solver.hasUniqueSolution(Board.fromPuzzle(pattern)) }
        assertEquals(emptyList(), ambiguous)
    }

    @Test
    fun `every generated puzzle has exactly one solution and none repeats a bundled one`() {
        val legacy = PUZZLE_ROW.findAll(File(System.getProperty("sudroid.puzzleData")).readText()).map { it.groupValues[2] }
        val generated = generatedPuzzles()

        assertEquals(emptyList(), generated.filterNot { Solver.hasUniqueSolution(Board.fromPuzzle(it.digits)) })
        val all = legacy.toList() + generated.map { it.digits }
        assertEquals(all.size, all.toSet().size)
        assertEquals((451..450 + generated.size).toList(), generated.map { it.id })
    }

    private companion object {
        // Bundled puzzle 311, which Grader grades as needing locked candidates.
        const val LOCKED_CANDIDATES_PUZZLE = "003001000608050090090200060800302650300000009054609003040003010030020406000400700"
        val PUZZLE_ROW = Regex("""INSERT INTO Puzzle values\('\d+', '(\w+)', '\d+', '(\d{81})'""")
    }
}

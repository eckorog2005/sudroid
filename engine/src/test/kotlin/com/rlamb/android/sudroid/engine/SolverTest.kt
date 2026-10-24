package com.rlamb.android.sudroid.engine

import com.rlamb.android.sudroid.engine.BoardTest.Companion.PUZZLE
import com.rlamb.android.sudroid.engine.BoardTest.Companion.SOLUTION
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        val PUZZLE_ROW = Regex("""INSERT INTO Puzzle values\('\d+', '(\w+)', '\d+', '(\d{81})'""")
    }
}

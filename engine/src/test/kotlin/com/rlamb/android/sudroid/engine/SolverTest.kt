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
    fun `every bundled puzzle has exactly one solution`() {
        val data = File(System.getProperty("sudroid.puzzleData")).readText()
        val puzzles = PUZZLE_ROW.findAll(data).map { it.groupValues[1] to it.groupValues[2] }.toList()

        assertEquals(mapOf("easy" to 150, "medium" to 150, "hard" to 150), puzzles.groupingBy { it.first }.eachCount())
        val ambiguous = puzzles.filterNot { (_, pattern) -> Solver.hasUniqueSolution(Board.fromPuzzle(pattern)) }
        assertEquals(emptyList(), ambiguous)
    }

    private companion object {
        val PUZZLE_ROW = Regex("""INSERT INTO Puzzle values\('\d+', '(\w+)', '\d+', '(\d{81})'""")
    }
}

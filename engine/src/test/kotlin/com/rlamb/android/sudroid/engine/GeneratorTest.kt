package com.rlamb.android.sudroid.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GeneratorTest {
    @Test
    fun `a generated puzzle is unique, has its clues and grades as asked`() {
        for ((technique, clues) in listOf(Technique.NAKED_SINGLE to 36, Technique.HIDDEN_SINGLE to 32, Technique.LOCKED_CANDIDATES to 28)) {
            repeat(5) { seed ->
                val puzzle = Generator.generate(technique, clues, Random(seed))

                assertTrue(Solver.hasUniqueSolution(puzzle))
                assertEquals(clues, Cell.ALL.count(puzzle::isGiven))
                assertEquals(technique, Grader.grade(puzzle))
            }
        }
    }

    @Test
    fun `the same seed generates the same puzzle`() {
        val first = Generator.generate(Technique.HIDDEN_SINGLE, 32, Random(7))
        val second = Generator.generate(Technique.HIDDEN_SINGLE, 32, Random(7))

        assertEquals(givens(first), givens(second))
    }

    @Test
    fun `too few clues for a unique solution are rejected`() {
        assertFailsWith<IllegalArgumentException> { Generator.generate(Technique.NAKED_SINGLE, 16, Random(0)) }
    }

    private fun givens(board: Board) = Cell.ALL.joinToString("") { if (board.isGiven(it)) board.digitAt(it).toString() else "0" }
}

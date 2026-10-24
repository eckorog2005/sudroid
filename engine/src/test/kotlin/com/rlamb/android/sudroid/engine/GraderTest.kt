package com.rlamb.android.sudroid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GraderTest {
    @Test
    fun `a puzzle grades by the hardest technique it needs`() {
        assertEquals(Technique.NAKED_SINGLE, grade(NAKED_SINGLES))
        assertEquals(Technique.HIDDEN_SINGLE, grade(HIDDEN_SINGLES))
        assertEquals(Technique.LOCKED_CANDIDATES, grade(LOCKED_CANDIDATES))
    }

    @Test
    fun `a puzzle the techniques cannot finish has no grade`() {
        assertNull(grade(BEYOND))
        assertNull(Grader.grade(Board.EMPTY))
    }

    @Test
    fun `a solved board grades as naked singles`() {
        assertEquals(Technique.NAKED_SINGLE, Grader.grade(Board.fromPuzzle(BoardTest.SOLUTION)))
    }

    @Test
    fun `grading counts the player's entries as placed`() {
        val board = Board.fromPuzzle(BoardTest.PUZZLE).withEntries(BoardTest.SOLUTION)

        assertEquals(Technique.NAKED_SINGLE, Grader.grade(board))
    }

    @Test
    fun `every generated puzzle grades as its difficulty`() {
        val expected = mapOf("EASY" to Technique.NAKED_SINGLE, "MEDIUM" to Technique.HIDDEN_SINGLE, "HARD" to Technique.LOCKED_CANDIDATES)

        val misgraded = generatedPuzzles().filter { grade(it.digits) != expected.getValue(it.difficulty) }

        assertEquals(emptyList(), misgraded)
    }

    private fun grade(puzzle: String) = Grader.grade(Board.fromPuzzle(puzzle))

    private companion object {
        // Bundled puzzles 2, 1, 311 and 301.
        const val NAKED_SINGLES = "800250000075003802932800005050001080029080450040700010200008671301400520000012009"
        const val HIDDEN_SINGLES = "012604830700800052806003000280065000030000020000380046000900105950008007068507290"
        const val LOCKED_CANDIDATES = "003001000608050090090200060800302650300000009054609003040003010030020406000400700"
        const val BEYOND = "801040060200100000000397000070002009009714300300900070000563000000001002040020603"
    }
}

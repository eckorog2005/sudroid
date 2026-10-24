/*
 *  Copyright 2026 Roger Lamb
 *
 *  This file is part of Sudroid.
 *
 *  Sudroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Sudroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Sudroid.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rlamb.android.sudroid.engine.tools

import com.rlamb.android.sudroid.engine.Cell
import com.rlamb.android.sudroid.engine.Generator
import com.rlamb.android.sudroid.engine.Technique
import java.io.File
import kotlin.random.Random

/**
 * Appends `count` new puzzles per difficulty to the generated puzzle file, as `id,DIFFICULTY,digits`
 * lines numbered after every puzzle already bundled. Lines already in the file are never changed,
 * since installs may have saved or finished those puzzles by ID.
 *
 * Usage: `GeneratePuzzles <data.txt> <puzzles.txt> <count>`
 */
fun main(args: Array<String>) {
    require(args.size == 3) { "Usage: GeneratePuzzles <data.txt> <puzzles.txt> <count>" }
    val legacy = LEGACY_ROW.findAll(File(args[0]).readText()).map { it.groupValues[1].toInt() to it.groupValues[2] }.toList()
    val output = File(args[1])
    val generated = if (output.exists()) output.readLines().filter(String::isNotBlank).map(::parseLine) else emptyList()
    val count = args[2].toInt()

    var nextId = (legacy + generated).maxOf { it.first } + 1
    val known = (legacy + generated).mapTo(mutableSetOf()) { it.second }
    val random = Random(nextId)
    val lines = buildList {
        for ((difficulty, grade) in GRADES) {
            var made = 0
            while (made < count) {
                val puzzle = Generator.generate(grade.technique, grade.clues, random)
                val digits = Cell.ALL.joinToString("") { if (puzzle.isGiven(it)) puzzle.digitAt(it).toString() else "0" }
                if (!known.add(digits)) continue
                add("${nextId++},$difficulty,$digits")
                made++
            }
        }
    }
    output.appendText(lines.joinToString("") { it + "\n" })
    println("Added ${lines.size} puzzles to $output")
}

private class Grade(val technique: Technique, val clues: Int)

// Clue counts follow the bundled set: about 36 for easy, 32 for medium and 28 to 30 for hard.
private val GRADES = linkedMapOf(
    "EASY" to Grade(Technique.NAKED_SINGLE, clues = 36),
    "MEDIUM" to Grade(Technique.HIDDEN_SINGLE, clues = 32),
    "HARD" to Grade(Technique.LOCKED_CANDIDATES, clues = 28),
)

private val LEGACY_ROW = Regex("""INSERT INTO Puzzle values\('(\d+)', '\w+', '\d+', '(\d{81})'""")

private fun parseLine(line: String): Pair<Int, String> {
    val (id, _, digits) = line.split(',')
    return id.toInt() to digits
}

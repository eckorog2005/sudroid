package com.rlamb.android.sudroid.engine

import java.io.File

data class GeneratedPuzzle(val id: Int, val difficulty: String, val digits: String)

/** The `id,DIFFICULTY,digits` lines of `puzzles.txt`, whose path comes in through `sudroid.generatedPuzzles`. */
fun generatedPuzzles(): List<GeneratedPuzzle> =
    File(System.getProperty("sudroid.generatedPuzzles")).readLines().filter(String::isNotBlank).map { line ->
        val (id, difficulty, digits) = line.split(',')
        GeneratedPuzzle(id.toInt(), difficulty, digits)
    }

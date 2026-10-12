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

package com.rlamb.android.sudroid.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rlamb.android.sudroid.GameModeEnum
import com.rlamb.android.sudroid.SudroidHighScoreActivity
import com.rlamb.android.sudroid.data.LegacyGameRepository
import com.rlamb.android.sudroid.ui.SudroidTheme

class GameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val request = when (val mode = GameModeEnum.entries[intent.getIntExtra(EXTRA_MODE, 0)]) {
            GameModeEnum.resume -> GameRequest.Resume
            GameModeEnum.easy, GameModeEnum.medium, GameModeEnum.hard ->
                GameRequest.New(Difficulty.valueOf(mode.name.uppercase()))
            GameModeEnum.weekly -> error("The weekly puzzle is not implemented")
        }
        val repository = LegacyGameRepository.get(this)

        setContent {
            SudroidTheme {
                GameScreen(
                    viewModel = viewModel { GameViewModel(repository, request) },
                    onSolved = { elapsedSeconds -> showScore(elapsedSeconds) },
                )
            }
        }
    }

    private fun showScore(elapsedSeconds: Int) {
        startActivity(
            Intent(this, SudroidHighScoreActivity::class.java)
                .putExtra(SudroidHighScoreActivity.finishTime, formatElapsed(elapsedSeconds))
                .putExtra("week", false),
        )
        finish()
    }

    companion object {
        /** A [GameModeEnum] ordinal. */
        const val EXTRA_MODE = "com.rlamb.android.sudroid.game.mode"
    }
}

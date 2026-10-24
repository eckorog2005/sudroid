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

package com.rlamb.android.sudroid.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rlamb.android.sudroid.R
import com.rlamb.android.sudroid.game.formatElapsed
import com.rlamb.android.sudroid.ui.SudroidTheme
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ResultsScreen(elapsedSeconds: Int, hints: Int, onBackToMenu: () -> Unit) {
    Scaffold { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.final_message),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Text(formatElapsed(elapsedSeconds), style = MaterialTheme.typography.displayLarge)
                if (hints > 0) {
                    Text(
                        pluralStringResource(R.plurals.results_hints, hints, hints),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onBackToMenu) { Text(stringResource(R.string.backMenu)) }
            }
            Confetti(Modifier.matchParentSize())
        }
    }
}

/** Falls once over the screen; saved state keeps a rotation from replaying it. */
@Composable
private fun Confetti(modifier: Modifier) {
    var played by rememberSaveable { mutableStateOf(false) }
    if (played) return
    val colors = MaterialTheme.colorScheme
    val palette = listOf(colors.primary, colors.secondary, colors.tertiary, colors.primaryContainer, colors.tertiaryContainer)
    val pieces = remember {
        val random = Random(CONFETTI_SEED)
        List(CONFETTI_PIECES) {
            Piece(
                x = random.nextFloat(),
                start = random.nextFloat() * 0.35f,
                speed = 0.8f + random.nextFloat() * 0.5f,
                phase = random.nextFloat() * 2 * PI.toFloat(),
                spin = (random.nextFloat() - 0.5f) * 1080f,
                color = it % palette.size,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 3200, easing = LinearEasing))
        played = true
    }
    Canvas(modifier.clearAndSetSemantics {}) {
        val width = 8.dp.toPx()
        val height = 14.dp.toPx()
        for (piece in pieces) {
            val t = ((progress.value - piece.start) / (1f - piece.start)).coerceIn(0f, 1f)
            if (t == 0f || t == 1f) continue
            val y = -height + (size.height + 2 * height) * t * piece.speed
            val x = piece.x * size.width + sin(piece.phase + t * 8f) * 24.dp.toPx()
            rotate(piece.spin * t, pivot = Offset(x, y)) {
                drawRect(
                    palette[piece.color],
                    topLeft = Offset(x - width / 2, y - height / 2),
                    size = Size(width, height),
                    alpha = 1f - t * t,
                )
            }
        }
    }
}

private class Piece(val x: Float, val start: Float, val speed: Float, val phase: Float, val spin: Float, val color: Int)

private const val CONFETTI_PIECES = 90
private const val CONFETTI_SEED = 2010

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ResultsScreenPreview() {
    SudroidTheme { ResultsScreen(elapsedSeconds = 754, hints = 2, onBackToMenu = {}) }
}

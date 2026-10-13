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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.rlamb.android.sudroid.R
import com.rlamb.android.sudroid.engine.Board
import com.rlamb.android.sudroid.engine.Cell
import com.rlamb.android.sudroid.ui.SudroidTheme

@Composable
fun GameScreen(viewModel: GameViewModel, onSolved: (elapsedSeconds: Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.runClock() }
    }
    if (state.isSolved) {
        LaunchedEffect(Unit) { onSolved(state.elapsedSeconds) }
    }
    GameContent(
        state = state,
        onSelect = viewModel::select,
        onDigit = viewModel::enter,
        onErase = viewModel::erase,
        onReset = viewModel::reset,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
    )
}

@Composable
private fun GameContent(
    state: GameUiState,
    onSelect: (Cell) -> Unit,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    Scaffold { insets ->
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(insets).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val board = state.board
            if (board == null) {
                CircularProgressIndicator()
                return@BoxWithConstraints
            }
            val sudokuBoard = @Composable { modifier: Modifier ->
                SudokuBoard(board, state.selected, onSelect, modifier)
            }
            val controls = @Composable { modifier: Modifier ->
                Controls(state, onDigit, onErase, onReset, onUndo, onRedo, modifier)
            }
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    sudokuBoard(Modifier.weight(1f, fill = false).aspectRatio(1f, matchHeightConstraintsFirst = true))
                    controls(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    sudokuBoard(Modifier.weight(1f, fill = false).aspectRatio(1f))
                    controls(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SudokuBoard(board: Board, selected: Cell?, onSelect: (Cell) -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val digitStyle = MaterialTheme.typography.headlineMedium
    val textMeasurer = rememberTextMeasurer()
    val currentOnSelect by rememberUpdatedState(onSelect)

    Canvas(
        modifier.pointerInput(Unit) {
            detectTapGestures { tap ->
                val cellSize = size.width / Board.SIZE.toFloat()
                val row = (tap.y / cellSize).toInt()
                val col = (tap.x / cellSize).toInt()
                if (row in 0 until Board.SIZE && col in 0 until Board.SIZE) currentOnSelect(Cell(row, col))
            }
        },
    ) {
        val cellSize = size.width / Board.SIZE
        drawRect(colors.surfaceContainerLow)
        if (selected != null) {
            drawRect(
                colors.primaryContainer,
                topLeft = Offset(selected.col * cellSize, selected.row * cellSize),
                size = Size(cellSize, cellSize),
            )
        }

        val fontSize = (cellSize * 0.6f).toSp()
        for (cell in Cell.ALL) {
            val digit = board.digitAt(cell)
            if (digit == 0) continue
            val style = when {
                board.hasConflict(cell) -> digitStyle.copy(color = colors.error)
                board.isGiven(cell) -> digitStyle.copy(color = colors.onSurface, fontWeight = FontWeight.Bold)
                else -> digitStyle.copy(color = colors.primary)
            }
            val text = textMeasurer.measure(digit.toString(), style.copy(fontSize = fontSize))
            drawText(
                text,
                topLeft = Offset(
                    cell.col * cellSize + (cellSize - text.size.width) / 2,
                    cell.row * cellSize + (cellSize - text.size.height) / 2,
                ),
            )
        }

        for (i in 0..Board.SIZE) {
            val isBoxEdge = i % 3 == 0
            val color = if (isBoxEdge) colors.onSurface else colors.outlineVariant
            val stroke = (if (isBoxEdge) 2.dp else 1.dp).toPx()
            val at = (i * cellSize).coerceIn(stroke / 2, size.width - stroke / 2)
            drawLine(color, Offset(at, 0f), Offset(at, size.height), stroke)
            drawLine(color, Offset(0f, at), Offset(size.width, at), stroke)
        }
    }
}

@Composable
private fun Controls(
    state: GameUiState,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(formatElapsed(state.elapsedSeconds), style = MaterialTheme.typography.titleLarge)
        for (digits in listOf(1..5, 6..9)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (digit in digits) {
                    FilledTonalButton(onClick = { onDigit(digit) }, Modifier.weight(1f), contentPadding = PadButtonPadding) {
                        Text(digit.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                }
                if (digits.last == 9) {
                    OutlinedButton(onClick = onErase, Modifier.weight(1f), contentPadding = PadButtonPadding) {
                        Text(stringResource(R.string.erase), maxLines = 1)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onUndo, enabled = state.canUndo) { Text(stringResource(R.string.undo)) }
            OutlinedButton(onClick = onRedo, enabled = state.canRedo) { Text(stringResource(R.string.redo)) }
            OutlinedButton(onClick = onReset) { Text(stringResource(R.string.reset)) }
        }
    }
}

private val PadButtonPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp)

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun GameContentPreview() {
    val board = Board.fromPuzzle("012604830700800052806003000280065000030000020000380046000900105950008007068507290")
        .place(Cell(0, 0), 9)
        .place(Cell(1, 1), 7)
    SudroidTheme {
        GameContent(GameUiState(board, selected = Cell(1, 1), elapsedSeconds = 83, canUndo = true), {}, {}, {}, {}, {}, {})
    }
}

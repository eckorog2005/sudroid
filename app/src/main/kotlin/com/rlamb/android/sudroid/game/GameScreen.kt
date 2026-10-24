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

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.rlamb.android.sudroid.R
import com.rlamb.android.sudroid.engine.Board
import com.rlamb.android.sudroid.engine.Cell
import com.rlamb.android.sudroid.engine.Hint
import com.rlamb.android.sudroid.ui.SudroidTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun GameScreen(viewModel: GameViewModel, onBack: () -> Unit, onSolved: (elapsedSeconds: Int, hints: Int) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.runClock() }
    }
    if (state.isSolved) {
        LaunchedEffect(Unit) { onSolved(state.elapsedSeconds, state.hints) }
    }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.completed) {
        if (state.completed.isNotEmpty()) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }
    GameContent(
        state = state,
        onBack = onBack,
        onSelect = viewModel::select,
        onDigit = {
            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            viewModel.enter(it)
        },
        onErase = {
            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            viewModel.erase()
        },
        onReset = viewModel::reset,
        onUndo = viewModel::undo,
        onRedo = viewModel::redo,
        onToggleNotes = viewModel::toggleNotesMode,
        onCheck = viewModel::checkMistakes,
        onHint = viewModel::hint,
        onPause = viewModel::pause,
        onUnpause = viewModel::unpause,
    )
}

@Composable
private fun GameContent(
    state: GameUiState,
    onBack: () -> Unit,
    onSelect: (Cell) -> Unit,
    onDigit: (Int) -> Unit,
    onErase: () -> Unit,
    onReset: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleNotes: () -> Unit,
    onCheck: () -> Unit,
    onHint: () -> Unit,
    onPause: () -> Unit,
    onUnpause: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    // Restarting on the next move cancels showSnackbar, which dismisses the explanation.
    LaunchedEffect(state.hint) {
        state.hint?.let {
            snackbarHostState.showSnackbar(explain(resources, it), withDismissAction = true, duration = SnackbarDuration.Long)
        }
    }
    var confirmingReset by rememberSaveable { mutableStateOf(false) }
    if (confirmingReset) {
        ResetDialog(
            onConfirm = {
                confirmingReset = false
                onReset()
            },
            onDismiss = { confirmingReset = false },
        )
    }
    Scaffold(
        topBar = { GameTopBar(state, onBack, onPause, onUnpause) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
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
                if (state.paused) {
                    PausedPanel(onUnpause, modifier)
                } else {
                    SudokuBoard(board, state.selected, state.showMistakes, state.completed, onSelect, modifier)
                }
            }
            val controls = @Composable { modifier: Modifier ->
                Controls(state, onDigit, onErase, { confirmingReset = true }, onUndo, onRedo, onToggleNotes, onCheck, onHint, modifier)
            }
            if (maxWidth > maxHeight) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    sudokuBoard(
                        Modifier.weight(1f, fill = false).widthIn(max = MaxBoardWidth)
                            .aspectRatio(1f, matchHeightConstraintsFirst = true),
                    )
                    controls(Modifier.weight(1f, fill = false).widthIn(max = MaxControlsWidth))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    sudokuBoard(Modifier.weight(1f, fill = false).widthIn(max = MaxBoardWidth).aspectRatio(1f))
                    controls(Modifier.widthIn(max = MaxControlsWidth).fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTopBar(state: GameUiState, onBack: () -> Unit, onPause: () -> Unit, onUnpause: () -> Unit) {
    TopAppBar(
        title = { Text(title(state), maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
            }
        },
        actions = {
            if (state.board != null) {
                Text(formatElapsed(state.elapsedSeconds), style = MaterialTheme.typography.titleLarge)
                if (state.paused) {
                    IconButton(onClick = onUnpause) { Icon(painterResource(R.drawable.ic_play), stringResource(R.string.unpause)) }
                } else {
                    IconButton(onClick = onPause) { Icon(painterResource(R.drawable.ic_pause), stringResource(R.string.pause)) }
                }
            }
        },
    )
}

@Composable
private fun title(state: GameUiState): String {
    val dailyDay = state.dailyDay
    return when {
        dailyDay != null -> stringResource(R.string.game_title_daily, formatDay(dailyDay))
        state.difficulty == Difficulty.EASY -> stringResource(R.string.easy)
        state.difficulty == Difficulty.MEDIUM -> stringResource(R.string.medium)
        state.difficulty == Difficulty.HARD -> stringResource(R.string.hard)
        else -> ""
    }
}

// Epoch days count local calendar days, so the day's midnight in UTC formats as that date.
private fun formatDay(epochDay: Long): String {
    val locale = Locale.getDefault()
    val format = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(epochDay * 86_400_000L))
}

@Composable
private fun ResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reset_title)) },
        text = { Text(stringResource(R.string.reset_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.reset)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Takes the board's place while paused, so the puzzle can't be studied with the clock stopped. */
@Composable
private fun PausedPanel(onUnpause: () -> Unit, modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerLow), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.paused), style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onUnpause) { Text(stringResource(R.string.unpause)) }
        }
    }
}

@Composable
private fun SudokuBoard(
    board: Board,
    selected: Cell?,
    showMistakes: Boolean,
    completed: Set<Cell>,
    onSelect: (Cell) -> Unit,
    modifier: Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val resources = LocalResources.current
    val descriptions = remember(board, showMistakes, resources) {
        Cell.ALL.map { describe(resources, board, it, showMistakes) }
    }

    val flash = remember { Animatable(0f) }
    LaunchedEffect(completed) {
        if (completed.isEmpty()) {
            flash.snapTo(0f)
        } else {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(durationMillis = 800))
        }
    }

    Box(modifier) {
        BoardCanvas(board, selected, showMistakes, completed, { flash.value }, textMeasurer, Modifier.matchParentSize())
        CellTargets(descriptions, selected, onSelect, Modifier.matchParentSize())
    }
}

private fun describe(resources: Resources, board: Board, cell: Cell, showMistakes: Boolean): String {
    val row = cell.row + 1
    val col = cell.col + 1
    val digit = board.digitAt(cell)
    val description = when {
        digit == 0 -> {
            val notes = board.notesAt(cell).sorted()
            if (notes.isEmpty()) {
                resources.getString(R.string.cell_empty, row, col)
            } else {
                resources.getString(R.string.cell_notes, row, col, notes.joinToString(", "))
            }
        }
        board.isGiven(cell) -> resources.getString(R.string.cell_given, row, col, digit)
        else -> resources.getString(R.string.cell_entry, row, col, digit)
    }
    return if (board.showsMistake(cell, showMistakes)) resources.getString(R.string.cell_conflict, description) else description
}

// A given is never the wrong digit, so only the entries it clashes with are marked.
private fun Board.showsMistake(cell: Cell, showMistakes: Boolean) = showMistakes && !isGiven(cell) && hasConflict(cell)

private fun explain(resources: Resources, hint: Hint): String {
    val row = hint.cell.row + 1
    val col = hint.cell.col + 1
    return when (val reason = hint.reason) {
        is Hint.Reason.Mistake -> resources.getString(R.string.hint_mistake, reason.wrongDigit, row, col, hint.digit)
        Hint.Reason.NakedSingle -> resources.getString(R.string.hint_naked_single, hint.digit, row, col)
        is Hint.Reason.HiddenSingle -> resources.getString(
            when (reason.house) {
                Hint.House.ROW -> R.string.hint_hidden_single_row
                Hint.House.COLUMN -> R.string.hint_hidden_single_column
                Hint.House.BOX -> R.string.hint_hidden_single_box
            },
            hint.digit,
            row,
            col,
        )
        is Hint.Reason.LockedCandidates -> {
            val then = resources.getString(
                when (val single = reason.then) {
                    is Hint.Reason.HiddenSingle -> when (single.house) {
                        Hint.House.ROW -> R.string.hint_leaves_hidden_single_row
                        Hint.House.COLUMN -> R.string.hint_leaves_hidden_single_column
                        Hint.House.BOX -> R.string.hint_leaves_hidden_single_box
                    }
                    else -> R.string.hint_leaves_naked_single
                },
                hint.digit,
                row,
                col,
            )
            (reason.locks.map { explainLock(resources, it) } + then).joinToString(" ")
        }
        Hint.Reason.FromSolution -> resources.getString(R.string.hint_from_solution, hint.digit, row, col)
    }
}

private fun explainLock(resources: Resources, lock: Hint.Lock): String {
    val boxNames = resources.getStringArray(R.array.box_names)
    val within = lock.within
    val confinedTo = lock.confinedTo
    return when {
        within.house == Hint.House.BOX -> resources.getString(
            if (confinedTo.house == Hint.House.ROW) R.string.hint_locked_in_row else R.string.hint_locked_in_column,
            boxNames[within.index],
            lock.digit,
            confinedTo.index + 1,
        )
        else -> resources.getString(
            if (within.house == Hint.House.ROW) R.string.hint_locked_from_row else R.string.hint_locked_from_column,
            within.index + 1,
            lock.digit,
            boxNames[confinedTo.index],
        )
    }
}

@Composable
private fun CellTargets(descriptions: List<String>, selected: Cell?, onSelect: (Cell) -> Unit, modifier: Modifier) {
    Column(modifier) {
        for (row in 0 until Board.SIZE) {
            Row(Modifier.weight(1f)) {
                for (col in 0 until Board.SIZE) {
                    val cell = Cell(row, col)
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .clickable(interactionSource = null, indication = null, role = Role.Button) { onSelect(cell) }
                            .semantics {
                                contentDescription = descriptions[row * Board.SIZE + col]
                                this.selected = cell == selected
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardCanvas(
    board: Board,
    selected: Cell?,
    showMistakes: Boolean,
    completed: Set<Cell>,
    flash: () -> Float,
    textMeasurer: TextMeasurer,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val digitStyle = MaterialTheme.typography.headlineMedium

    Canvas(modifier) {
        val cellSize = size.width / Board.SIZE
        drawRect(colors.surfaceContainerLow)
        fun highlight(cell: Cell, color: Color) = drawRect(
            color,
            topLeft = Offset(cell.col * cellSize, cell.row * cellSize),
            size = Size(cellSize, cellSize),
        )
        if (selected != null) {
            val selectedDigit = board.digitAt(selected)
            selected.peers.forEach { highlight(it, colors.surfaceContainerHighest) }
            if (selectedDigit != 0) {
                Cell.ALL.filter { board.digitAt(it) == selectedDigit }.forEach { highlight(it, colors.tertiary.copy(alpha = 0.35f)) }
            }
            highlight(selected, colors.primaryContainer)
        }
        val flashAlpha = flash()
        if (flashAlpha > 0f) completed.forEach { highlight(it, colors.primary.copy(alpha = 0.45f * flashAlpha)) }

        val fontSize = (cellSize * 0.6f).toSp()
        val noteStyle = digitStyle.copy(color = colors.onSurfaceVariant, fontSize = (cellSize * 0.27f).toSp())
        val noteSize = cellSize / 3
        for (cell in Cell.ALL) {
            val digit = board.digitAt(cell)
            if (digit == 0) {
                for (note in board.notesAt(cell)) {
                    val text = textMeasurer.measure(note.toString(), noteStyle)
                    drawText(
                        text,
                        topLeft = Offset(
                            cell.col * cellSize + (note - 1) % 3 * noteSize + (noteSize - text.size.width) / 2,
                            cell.row * cellSize + (note - 1) / 3 * noteSize + (noteSize - text.size.height) / 2,
                        ),
                    )
                }
                continue
            }
            val isMistake = board.showsMistake(cell, showMistakes)
            if (isMistake) {
                val corner = cellSize * 0.28f
                val right = (cell.col + 1) * cellSize
                val top = cell.row * cellSize
                drawPath(
                    Path().apply {
                        moveTo(right - corner, top)
                        lineTo(right, top)
                        lineTo(right, top + corner)
                        close()
                    },
                    colors.error,
                )
            }
            val style = when {
                isMistake -> digitStyle.copy(color = colors.error)
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
    onToggleNotes: () -> Unit,
    onCheck: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier,
) {
    val playing = !state.paused
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        for (digits in listOf(1..5, 6..9)) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (digit in digits) {
                    val remaining = state.board?.remaining(digit) ?: 0
                    val description = stringResource(
                        if (state.notesMode) R.string.note_remaining else R.string.digit_remaining,
                        digit,
                        remaining,
                    )
                    FilledTonalButton(
                        onClick = { onDigit(digit) },
                        Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = description },
                        enabled = playing && remaining > 0,
                        colors = if (state.notesMode) {
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        },
                        contentPadding = PadButtonPadding,
                    ) {
                        Column(Modifier.clearAndSetSemantics {}, horizontalAlignment = Alignment.CenterHorizontally) {
                            DigitLabel(digit, small = state.notesMode)
                            Text(remaining.toString(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                if (digits.last == 9) {
                    PadAction(R.drawable.ic_backspace, stringResource(R.string.erase), onErase, Modifier.weight(1f).fillMaxHeight(), playing)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val weighted = Modifier.weight(1f)
            PadAction(R.drawable.ic_undo, stringResource(R.string.undo), onUndo, weighted, playing && state.canUndo)
            PadAction(R.drawable.ic_redo, stringResource(R.string.redo), onRedo, weighted, playing && state.canRedo)
            PadAction(
                R.drawable.ic_edit,
                stringResource(R.string.notes),
                onToggleNotes,
                weighted.semantics { selected = state.notesMode },
                playing,
                highlighted = state.notesMode,
            )
            if (state.mistakeMode == MistakeMode.ON_REQUEST) {
                PadAction(R.drawable.ic_check, stringResource(R.string.check), onCheck, weighted, playing)
            }
            PadAction(R.drawable.ic_lightbulb, stringResource(R.string.hint), onHint, weighted, playing)
            PadAction(R.drawable.ic_restart, stringResource(R.string.reset), onReset, weighted, playing)
        }
    }
}

@Composable
private fun PadAction(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    highlighted: Boolean = false,
) {
    val content = @Composable {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(icon), contentDescription = null, Modifier.size(20.dp))
            PadLabel(label)
        }
    }
    if (highlighted) {
        Button(onClick, modifier, enabled, contentPadding = PadButtonPadding) { content() }
    } else {
        OutlinedButton(onClick, modifier, enabled, contentPadding = PadButtonPadding) { content() }
    }
}

/** In notes mode the digit shrinks like a pencil mark, keeping its line height so the pad doesn't move. */
@Composable
private fun DigitLabel(digit: Int, small: Boolean) {
    val style = MaterialTheme.typography.titleLarge
    Text(
        digit.toString(),
        style = if (small) style.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize) else style,
    )
}

/** Shrinks to fit its button, so large system font sizes don't clip the label. */
@Composable
private fun PadLabel(text: String) {
    Text(
        text,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.labelLarge.fontSize),
    )
}

private val MaxBoardWidth = 640.dp
private val MaxControlsWidth = 440.dp
private val PadButtonPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp)

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun GameContentPreview() {
    val board = Board.fromPuzzle("012604830700800052806003000280065000030000020000380046000900105950008007068507290")
        .place(Cell(0, 0), 9)
        .place(Cell(1, 1), 7)
        .toggleNote(Cell(0, 4), 5)
        .toggleNote(Cell(0, 4), 7)
        .toggleNote(Cell(0, 8), 1)
    SudroidTheme {
        GameContent(
            GameUiState(board, Difficulty.MEDIUM, selected = Cell(1, 1), elapsedSeconds = 83, canUndo = true),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

package oleh.zaiets.game_of_life

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import oleh.zaiets.game_of_life.ui.theme.GameOfLifeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val game = GameOfLife()
        setContent {
            GameOfLifeTheme {
                Column {
                    val localDensity = LocalDensity.current
                    val cellSize = remember { with(localDensity) { 20.dp.toPx() } }
                    var size by remember { mutableStateOf(IntSize(width = 0, height = 0)) }
                    val rowsCount by remember { derivedStateOf { (size.height / cellSize).toInt() } }
                    val columnsCount by remember { derivedStateOf { (size.width / cellSize).toInt() } }
                    val cellsState by game.boardStateFlow().collectAsState()

                    LaunchedEffect(rowsCount, columnsCount) {
                        game.setBoardSize(rowsCount, columnsCount)
                    }

                    GameOfLifeBoard(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxWidth()
                            .padding(8.dp)
                            .onSizeChanged { size = it },
                        cellsState = cellsState,
                        onItemClick = game::clickOnCell
                    )

                    GameControllers(
                        game = game,
                        modifier = Modifier
                            .wrapContentHeight()
                            .fillMaxWidth()
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GameControllers(
    game: GameOfLife,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val gameState by game.gameStateFlow().collectAsState()
    val currentTick by game.tickFlow().collectAsState(initial = 0)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                text = if (gameState is GameState.Stopped) {
                    "Game stopped"
                } else {
                    "Game in progress"
                },
                textAlign = TextAlign.Start,
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(),
                text = "Tick: $currentTick",
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                modifier = Modifier.weight(1f),
                onClick = { scope.launch { game.start() } }
            ) {
                Text(text = "Start")
            }
            ElevatedButton(
                modifier = Modifier.weight(1f),
                onClick = { game.stop() }
            ) {
                Text(text = "Stop")
            }
            ElevatedButton(
                modifier = Modifier.weight(1f),
                onClick = { game.clearBoard() }
            ) {
                Text(text = "Clear")
            }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            text = "Tick speed",
            textAlign = TextAlign.Start,
        )
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                modifier = Modifier.weight(1f),
                onClick = { game.tickSpeed(tickTimeInMillis = 200) }
            ) {
                Text(text = "Slow")
            }
            ElevatedButton(
                modifier = Modifier.weight(1f),
                onClick = { game.tickSpeed(tickTimeInMillis = 100) }
            ) {
                Text(text = "Default")
            }
            ElevatedButton(
                modifier = Modifier.weight(1f),
                onClick = { game.tickSpeed(tickTimeInMillis = 50) }
            ) {
                Text(text = "Fast")
            }
        }
    }
}

@Composable
private fun GameOfLifeBoard(
    cellsState: Array<BooleanArray>,
    onItemClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        repeat(cellsState.size) { rowIndex ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                repeat(cellsState[rowIndex].size) { columnIndex ->
                    Cell(
                        modifier = Modifier.clickable { onItemClick(rowIndex, columnIndex) },
                        isAlive = cellsState.getOrNull(rowIndex)
                            ?.getOrNull(columnIndex)
                            ?: false,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.Cell(
    isAlive: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .background(Color.Black)
            .padding(1.dp)
            .background(
                if (isAlive) {
                    Color.DarkGray
                } else {
                    Color.White
                }
            )
    )
}

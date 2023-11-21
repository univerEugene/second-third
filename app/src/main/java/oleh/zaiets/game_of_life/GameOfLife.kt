package oleh.zaiets.game_of_life

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameOfLife {

    private data class CellIndexes(
        val rowIndex: Int,
        val columnIndex: Int,
    )

    private var rowsCount = 1

    private var columnsCount = 1

    private var gameJob: Job? = null

    private val board = MutableStateFlow(Array(size = 1) { booleanArrayOf(false) })

    private val gameState = MutableStateFlow<GameState>(GameState.Stopped)

    private var tickTimeInMillis: Long = 100L

    fun boardStateFlow(): StateFlow<Array<BooleanArray>> = board.asStateFlow()

    fun gameStateFlow(): StateFlow<GameState> = gameState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun tickFlow(): Flow<Long> = gameState
        .filterIsInstance<GameState.InProgress>()
        .mapLatest { it.tick }

    fun setBoardSize(rowsCount: Int, columnsCount: Int) {
        require(rowsCount > 1) { "cells in row must be bigger than 1" }
        require(columnsCount > 1) { "cells in column must be bigger than 1" }
        this.columnsCount = columnsCount
        this.rowsCount = rowsCount
        clearBoard()
    }

    fun clickOnCell(rowIndex: Int, columnIndex: Int) {
        if (gameJob == null) {
            invertCell(rowIndex, columnIndex)
        }
    }

    fun clearBoard() {
        stop()
        board.tryEmit(Array(size = rowsCount) { BooleanArray(size = columnsCount) { false } })
    }

    fun stop() {
        gameJob?.cancel()
        gameJob = null
        gameState.tryEmit(GameState.Stopped)
    }

    fun tickSpeed(tickTimeInMillis: Long) {
        if (tickTimeInMillis > 0) {
            this.tickTimeInMillis = tickTimeInMillis
        }
    }

    suspend fun start() = withContext(Dispatchers.Default) {
        if (gameJob != null) {
            return@withContext
        }

        gameJob = this.launch {
            gameState.emit(GameState.InProgress(0L))
            var tick = 0L
            while (isActive) {
                val currentGameState = board.value
                val newGameState = Array(size = rowsCount) { rowIndex ->
                    BooleanArray(size = columnsCount) { columnIndex ->
                        currentGameState[rowIndex][columnIndex]
                    }
                }

                currentGameState.forEachIndexed { rowIndex, booleans ->
                    booleans.forEachIndexed { columnIndex, _ ->
                        newGameState[rowIndex][columnIndex] = isAlive(currentGameState, rowIndex, columnIndex)
                    }
                }

                tick++

                board.emit(newGameState)
                gameState.emit(GameState.InProgress(tick))

                if (tick == Long.MAX_VALUE) {
                    tick = 0
                }

                if (currentGameState.contentEquals(newGameState)) {
                    stop()
                }

                delay(tickTimeInMillis)
            }
        }
    }

    private fun isAlive(
        currentState:  Array<BooleanArray>,
        rowIndex: Int,
        columnIndex: Int
    ): Boolean {
        val aliveCount = getNeighbourIndexes(rowIndex, columnIndex).count { indexes ->
            currentState[indexes.rowIndex][indexes.columnIndex]
        }

        val currentLifeState = currentState[rowIndex][columnIndex]

        return when {
            aliveCount == 3 -> true
            aliveCount == 2 && currentLifeState -> true
            else -> false
        }
    }

    private fun getNeighbourIndexes(rowIndex: Int, columnIndex: Int): List<CellIndexes> {
        return listOf(
            calculateIndex(rowIndex = rowIndex, columnIndex = columnIndex - 1),
            calculateIndex(rowIndex = rowIndex, columnIndex = columnIndex + 1),
            calculateIndex(rowIndex = rowIndex - 1, columnIndex = columnIndex - 1),
            calculateIndex(rowIndex = rowIndex - 1, columnIndex = columnIndex),
            calculateIndex(rowIndex = rowIndex - 1, columnIndex = columnIndex + 1),
            calculateIndex(rowIndex = rowIndex + 1, columnIndex = columnIndex - 1),
            calculateIndex(rowIndex = rowIndex + 1, columnIndex = columnIndex),
            calculateIndex(rowIndex = rowIndex + 1, columnIndex = columnIndex + 1),
        )
    }

    private fun calculateIndex(rowIndex: Int, columnIndex: Int): CellIndexes {
        val newRowIndex = when {
            rowIndex < 0 -> rowsCount - 1
            rowIndex >= rowsCount -> 0
            else -> rowIndex
        }

        val newColumnIndex = when {
            columnIndex < 0 -> columnsCount - 1
            columnIndex >= columnsCount -> 0
            else -> columnIndex
        }

        return CellIndexes(newRowIndex, newColumnIndex)
    }

    private fun invertCell(rowIndex: Int, columnIndex: Int) {
        board.update { oldCellsState ->
            Array(size = oldCellsState.size) { newRowIndex ->
                val oldRowValue = oldCellsState[newRowIndex]
                if (newRowIndex == rowIndex) {
                    BooleanArray(size = oldRowValue.size) { newColumnIndex ->
                        val oldColumnValue = oldRowValue[newColumnIndex]
                        if (newColumnIndex == columnIndex) {
                            !oldColumnValue
                        } else {
                            oldColumnValue
                        }
                    }
                } else {
                    oldRowValue
                }
            }
        }
    }
}

sealed class GameState {
    object Stopped: GameState()
    data class InProgress(val tick: Long): GameState()
}

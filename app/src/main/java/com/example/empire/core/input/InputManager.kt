package com.example.empire.core.input

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Direction { None, Up, Down, Left, Right }

data class DPadState(
    val direction: Direction = Direction.None,
    val dx: Int = 0,  // -1..1
    val dy: Int = 0   // -1..1
)

class InputManager {
    private val _dpad = MutableStateFlow(DPadState())
    val dpad: StateFlow<DPadState> = _dpad

    fun setDirection(dir: Direction) {
        val (dx, dy) = when (dir) {
            Direction.Up    -> 0 to -1
            Direction.Down  -> 0 to 1
            Direction.Left  -> -1 to 0
            Direction.Right -> 1 to 0
            Direction.None  -> 0 to 0
        }
        _dpad.value = DPadState(dir, dx, dy)
    }

    fun stop() = setDirection(Direction.None)
}

// Singleton instance
val inputManager = InputManager()

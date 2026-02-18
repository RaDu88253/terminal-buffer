package org.terminalbuffer

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CursorTest {

    @Test
    fun `cursor remains inside screen bounds when set`(){
        val buffer = TerminalBuffer(80, 24)
        buffer.setCursorPosition(80, 0)
        assertEquals( 79,buffer.getCursorPosition().first)
    }

    @Test
    fun `cursor correctly moves on both axes`(){
        val buffer = TerminalBuffer(80, 24)
        buffer.moveCursor(Directions.RIGHT, 10)
        buffer.moveCursor(Directions.DOWN, 15)
        assertEquals(10, buffer.getCursorPosition().first)
        assertEquals(15, buffer.getCursorPosition().second)
    }

    @Test
    fun `cursor remains inside screen bounds when moved`(){
        val buffer = TerminalBuffer(80, 24)
        buffer.moveCursor(Directions.UP, 26)
        assertEquals(0, buffer.getCursorPosition().second)
    }

    @AfterEach
    fun resetBuffer(){
        val buffer = TerminalBuffer(80, 24)
        buffer.setCursorPosition(0, 0)
    }

}
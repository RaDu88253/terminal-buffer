package org.terminalbuffer

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AttributesTest {

    @Test
    fun `style is applied correctly`(){
        val terminalBuffer = TerminalBuffer()
        terminalBuffer.setCurrentAttributes(Colors.RED, Colors.GREEN,
            "BOLD", "ITALIC", "UNDERLINE")
        assertEquals(Colors.RED, terminalBuffer.getForegroundColor())
        assertEquals(Colors.GREEN, terminalBuffer.getBackgroundColor())
        assertEquals(7, terminalBuffer.getStyle())
    }

}
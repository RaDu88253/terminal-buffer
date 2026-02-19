package org.terminalbuffer

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WriteTest {

    @Test
    fun `write function works in the general case`(){
        val terminalBuffer = TerminalBuffer()
        terminalBuffer.write("Test input\n")
        val (x, y) = terminalBuffer.getCursorPosition()
        val testChar = terminalBuffer.getCharAtScreenPosition(6, 0)
        assertEquals(10, x)
        assertEquals(1, y)
        assertEquals('n', testChar)
    }

    @Test
    fun `write function also updates the scrollback`(){
        val terminalBuffer = TerminalBuffer()
        for (i in 0 until 26){
            terminalBuffer.write(i.toString() + "\n\r")
        }
        val testChar = terminalBuffer.getCharAtScrollbackPosition(0, 1)
        assertEquals('1', testChar)
    }

    @Test
    fun `backspace properly deletes characters`(){
        val terminalBuffer = TerminalBuffer()
        terminalBuffer.write("Te\b2st")
        val testChar = terminalBuffer.getCharAtScreenPosition(1, 0)
        assertEquals('2', testChar)
    }

    @Test
    fun `cursor properly kept in bounds when trying to backspace out`(){
        val terminalBuffer = TerminalBuffer()
        terminalBuffer.write("Test\b\b\b\b\b\b")
        val (x, y) = terminalBuffer.getCursorPosition()
        assertEquals(0, x)
        assertEquals(0, y)
    }

    @Test
    fun `backspace properly goes to previous line`(){
        val terminalBuffer = TerminalBuffer()
        terminalBuffer.write("\n\n\b")
        val (x, y) = terminalBuffer.getCursorPosition()
        assertEquals(79, x)
        assertEquals(1, y)
    }

    @Test
    fun `carriage return works properly`(){
        val terminalBuffer = TerminalBuffer()
        terminalBuffer.write("Test\rAnotherTest")
        val testChar = terminalBuffer.getCharAtScreenPosition(0, 0)
        assertEquals('A', testChar)
    }

    @Test
    fun `tabs properly move the cursor in the general case`(){
        val terminalBuffer = TerminalBuffer()
        terminalBuffer.write("\tTest")
        val testChar = terminalBuffer.getCharAtScreenPosition(4, 0)
        assertEquals('T', testChar)
    }

    @Test
    fun `tabs properly move the cursor to the next line`(){
        val terminalBuffer = TerminalBuffer()
        terminalBuffer.write(CharArray(20){'\t'}.concatToString())
//        val testChar = terminalBuffer.getCharAtScreenPosition(1, 1)
//        assertEquals('T', testChar)
        val (x, y) = terminalBuffer.getCursorPosition()
        assertEquals(0, x)
        assertEquals(1, y)

    }

    @Test
    fun `text properly inserted in the general case`(){
        val terminalBuffer = TerminalBuffer()
        val input = "This is a test."
        terminalBuffer.write(input)
        terminalBuffer.setCursorPosition(9, 0)
        terminalBuffer.insertText(" successful ")
        val expectedOutput = "This is a successful test."
        val testOutput = terminalBuffer.getScreenContent()
        assertEquals(expectedOutput, testOutput)
    }

    @Test
    fun `text properly inserted on lines ending in a break`(){
        val terminalBuffer = TerminalBuffer()
        val input = "This is a test for lines ending in a line break.\n"
        terminalBuffer.write(input)
        terminalBuffer.setCursorPosition(9, 0)
        val textToBeInserted = " successful "
        terminalBuffer.insertText(textToBeInserted)
        val (x, y) = terminalBuffer.getCursorPosition()
        assertEquals(9 + textToBeInserted.length, x)
        assertEquals(0, y)
        val expectedOutput = "This is a successful test for lines ending in a \nline break."
        val testOutput = terminalBuffer.getScreenContent()
        assertEquals(expectedOutput, testOutput)
    }

    @Test
    fun `text properly inserted and wrapping the lines`(){
        val terminalBuffer = TerminalBuffer(width = 20)
        val input = "This is a test."
        terminalBuffer.write(input)
        terminalBuffer.setCursorPosition(9, 0)
        terminalBuffer.insertText(" successful ")
        val expectedScreen1stRow = "This is a successful"
        val actualScreen1stRow = terminalBuffer.getScreenRowAsString(0)
        assertEquals(expectedScreen1stRow, actualScreen1stRow)
        val expectedScreen2ndRow = " test."
        val actualScreen2ndRow = terminalBuffer.getScreenRowAsString(1)
        assertEquals(expectedScreen2ndRow, actualScreen2ndRow)
    }

    @Test
    fun `text properly inserted and updating the scrollback`(){
        val terminalBuffer = TerminalBuffer(height = 2, width = 20)
        val input = "This is a test."
        terminalBuffer.write(input)
        terminalBuffer.setCursorPosition(0, 1)
        terminalBuffer.write(input)
        terminalBuffer.setCursorPosition(9, 1)
        terminalBuffer.insertText(" successful ")
        val expectedScrollback1stRow = "This is a test."
        val actualScrollback1stRow = terminalBuffer.getScrollbackRowAsString(0)
        assertEquals(expectedScrollback1stRow, actualScrollback1stRow)
    }

    @Test
    fun `fill line works properly`(){
        val terminalBuffer = TerminalBuffer(width = 20)
        val input = "This is a test."
        terminalBuffer.write(input)
        terminalBuffer.fillCurrentLine('*')
        terminalBuffer.fillCurrentLine('*')
        val expectedScreen1stRow = "This is a test.*****"
        val actualScreen1stRow = terminalBuffer.getScreenRowAsString(0)
        assertEquals(expectedScreen1stRow, actualScreen1stRow)
        val expectedScreen2ndRow = "********************"
        val actualScreen2ndRow = terminalBuffer.getScreenRowAsString(1)
        assertEquals(expectedScreen2ndRow, actualScreen2ndRow)
    }

    @Test
    fun `setting attributes works properly in the general case`(){
        val terminalBuffer = TerminalBuffer(width = 20)
        val input = "This is a test."
        terminalBuffer.write(input)
        terminalBuffer.setCurrentAttributes(
            Colors.RED, Colors.GREEN, "BOLD", "ITALIC", "UNDERLINE"
        )
        terminalBuffer.fillCurrentLine('*')
        val expectedAttributes = Attributes(Colors.RED, Colors.GREEN, 7)
        val actualAttributes = terminalBuffer.getAttributesAtScreenPosition(16, 0)
        assertEquals(expectedAttributes, actualAttributes)
    }

    @Test
    fun `setting attributes works properly when inserting text`(){
        val terminalBuffer = TerminalBuffer()
        val input = "This is a test."
        terminalBuffer.write(input)
        terminalBuffer.setCurrentAttributes(
            Colors.RED, Colors.GREEN, "BOLD", "ITALIC", "UNDERLINE"
        )
        terminalBuffer.setCursorPosition(9, 0)
        terminalBuffer.insertText(" successful ")
        val expectedAttributes = Attributes(Colors.RED, Colors.GREEN, 7)
        val actualAttributes = terminalBuffer.getAttributesAtScreenPosition(9, 0)
        assertEquals(expectedAttributes, actualAttributes)
        val expected2ndRowAttributes = Attributes()
        val actual2ndRowAttributes = terminalBuffer.getAttributesAtScreenPosition(0, 1)
        assertEquals(expected2ndRowAttributes, actual2ndRowAttributes)
    }

    @Test
    fun `inserting line at the end of the screen properly updates scrollback`(){
        val terminalBuffer = TerminalBuffer()
        val input = "Test"
        terminalBuffer.write(input)
        terminalBuffer.insertEmptyLine()
        val expectedOutput = "Test"
        val actualOutput = terminalBuffer.getScrollbackRowAsString(0)
        assertEquals(expectedOutput, actualOutput)
    }

    @Test
    fun `clearing the screen properly works`(){
        val terminalBuffer = TerminalBuffer()
        val input = "Test"
        terminalBuffer.write(input)
        terminalBuffer.clearScreen()
        val expectedOutput = ""
        val actualOutput = terminalBuffer.getScreenContent()
        assertEquals(expectedOutput, actualOutput)
    }

    @Test
    fun `clearing the screen and the scrollback properly works`(){
        val terminalBuffer = TerminalBuffer()
        val input = "Test"
        terminalBuffer.write(input)
        terminalBuffer.insertEmptyLine()
        terminalBuffer.setCursorPosition(0, 0)
        terminalBuffer.write("Test")
        terminalBuffer.clearAll()
        val expectedOutput = ""
        val actualOutput = terminalBuffer.getAllContent()
        assertEquals(expectedOutput, actualOutput)
    }

    @Test
    fun `wide character test`(){
        val terminalBuffer = TerminalBuffer()
        val input = "Test \u4E2D"
        terminalBuffer.write(input)
        val actualOutput = terminalBuffer.getScreenContent()
        assertEquals(input, actualOutput)
    }

    @AfterEach
    fun resetTerminalBuffer(){
        val terminalBuffer = TerminalBuffer()
        terminalBuffer.setCursorPosition(0, 0)
    }

}
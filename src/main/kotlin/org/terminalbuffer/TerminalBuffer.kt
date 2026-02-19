package org.terminalbuffer

class TerminalBuffer(
    val width: Int = 80,
    val height: Int = 24,
    val maximumScrollback: Int = 100,
    val tabSize: Int = 4
) {

    private val screen = ArrayDeque<Array<Cell>>()
    private val scrollback = ArrayDeque<Array<Cell>>()

    init {
        repeat(height) {
            screen.addLast(Array(width) { Cell() })
        }
    }

    private object Cursor {
        var x = 0
        var y = 0
    }

    private var currentAttributes = Attributes()

    val styleMap = mapOf("BOLD" to (1 shl 0), "ITALIC" to (1 shl 1), "UNDERLINE" to (1 shl 2), "DEFAULT" to 0)

    /**
     * Sets foreground color, background color and style for following written text. Persistent until changed.
     */
    fun setCurrentAttributes(
        foregroundColor: Colors = currentAttributes.foregroundColor,
        backgroundColor: Colors = currentAttributes.backgroundColor,
        vararg styles: String
    ) {
        currentAttributes.foregroundColor = foregroundColor
        currentAttributes.backgroundColor = backgroundColor
        if (!styles.isEmpty())
            currentAttributes.style = 0
        for (style in styles) {
            currentAttributes.style = currentAttributes.style or styleMap.getOrDefault(style, 0)
        }
    }

    /**
     * Getter for the current foreground color
     */
    fun getForegroundColor(): Colors {
        return currentAttributes.foregroundColor
    }
    /**
      *Getter for the current background color
      */
    fun getBackgroundColor(): Colors {
        return currentAttributes.backgroundColor
    }

    /**
     * Getter for current style
     */
    fun getStyle(): Int {
        return currentAttributes.style
    }

    /**
     * Getter for the current cursor position
     */
    fun getCursorPosition(): Pair<Int, Int> {
        return Pair(Cursor.x, Cursor.y)
    }

    /**
     * Setter for the current cursor position
     */
    fun setCursorPosition(x: Int, y: Int) {
        Cursor.x = x.coerceIn(0, width - 1)
        Cursor.y = y.coerceIn(0, height - 1)
    }

    /**
     * Moves cursor in any direction by n squares, limited inside the screen.
     */
    fun moveCursor(direction: Directions, n: Int = 1) {
        when (direction) {
            Directions.RIGHT -> {
                Cursor.x += n
            }

            Directions.LEFT -> {
                Cursor.x -= n
            }

            Directions.DOWN -> {
                Cursor.y += n
            }

            Directions.UP -> {
                Cursor.y -= n
            }
        }

        Cursor.x = Cursor.x.coerceIn(0, width - 1)
        Cursor.y = Cursor.y.coerceIn(0, height - 1)

    }

    /**
     * Writes a text inside the terminal at the current position, moving the cursor.
     */
    fun write(input: String) {
        for (char in input) {
            when (char) {
                '\n' -> {
                    screen[Cursor.y][Cursor.x].character = '\n'
                    Cursor.y++
                    wrapCursorUpdateScrollback()
                }

                '\r' -> Cursor.x = 0
                '\b' -> {
                    Cursor.x--
                    wrapCursorUpdateScrollback()
                    screen[Cursor.y][Cursor.x].character = ' '
                }

                '\t' -> {
                    repeat(tabSize) {
                        Cursor.x++
                        wrapCursorUpdateScrollback()
                    }
                }

                else -> {
                    if (screen[Cursor.y][Cursor.x].character == '\n') {
                        Cursor.x = 0
                        Cursor.y++
                        wrapCursorUpdateScrollback()
                    }
                    if (isWide(char)){
                        if (Cursor.x == width - 1){
                            Cursor.x++
                            wrapCursorUpdateScrollback()
                        }
                        screen[Cursor.y][Cursor.x].character = char
                        screen[Cursor.y][Cursor.x].attributes = currentAttributes.copy()
                        screen[Cursor.y][Cursor.x].charType = CharacterWidthType.WIDE_START
                        Cursor.x++
                        screen[Cursor.y][Cursor.x].character = char
                        screen[Cursor.y][Cursor.x].attributes = currentAttributes.copy()
                        screen[Cursor.y][Cursor.x].charType = CharacterWidthType.WIDE_END
                        Cursor.x++
                        wrapCursorUpdateScrollback()
                    }
                    else {
                        screen[Cursor.y][Cursor.x].character = char
                        screen[Cursor.y][Cursor.x].attributes = currentAttributes.copy()
                        Cursor.x++
                        wrapCursorUpdateScrollback()
                    }
                }
            }
        }
    }

    /**
     * Inserts a text containing no line breaks at the current cursor position, wrapping lines if necessary.
     */
    fun insertText(input: String) {

        val cellsToRewrite = ArrayDeque<Cell>()
        for (cell in screen[Cursor.y].slice(Cursor.x + 1 until width)) cellsToRewrite.addLast(cell.copy())
        for (row in screen.slice(Cursor.y + 1 until height)){
            for (cell in row) cellsToRewrite.addLast(cell.copy())
        }

        write(input)
        val x = Cursor.x
        val y = Cursor.y
        write(cellsToRewrite)
        Cursor.x = x
        Cursor.y = y
        wrapCursorUpdateScrollback()
    }

    /**
     * Writes a text at the current position, updating the cursor. Keeps note of the style of each cell in the array passed as a parameter.
     */
    private fun write(cells: ArrayDeque<Cell>) {
        val initialAttributes = currentAttributes.copy()
        while (cells.last().character == ' ')
            cells.removeLast()
        for (cell in cells){
            if (cell.character != '\n') {
                currentAttributes = cell.attributes.copy()
                write(cell.character.toString())
            }
        }
        currentAttributes = initialAttributes.copy()
    }

    /**
     * Fills the current line with a specified character, then moves the cursor to the start of the next line.
     */
    fun fillCurrentLine(character: Char){
        write(character.toString().repeat(width - Cursor.x))
        wrapCursorUpdateScrollback()
    }

    /**
     * Fills the current line with empty spaces, then moves the cursor to the start of the next line.
     */
    fun fillCurrentLineEmpty(character: Char){
        write(character.toString().repeat(width - Cursor.x))
        wrapCursorUpdateScrollback()
    }

    /**
     * Inserts an empty line at the end of the screen.
     */
    fun insertEmptyLine(){
        scrollback.addLast(screen.first())
        screen.removeFirst()
        screen.addLast(Array(10) { Cell() })
    }

    /**
     * Removes all characters from the screen.
     */
    fun clearScreen(){
        screen.clear()
        repeat(height){
            screen.addLast(Array(width) { Cell() })
        }
    }

    /**
     * Removes all characters from the screen and the scrollback.
     */
    fun clearAll(){
        clearScreen()
        scrollback.clear()
    }

    /**
     * Keeps the position of the cursor valid, provides cursor terminal behavior, updates the scrollback if necessary.
     */
    private fun wrapCursorUpdateScrollback() {
        if (Cursor.x >= width) {
            Cursor.x = 0
            Cursor.y++
        }
        if (Cursor.y >= height) {
            Cursor.y = height - 1
            if (scrollback.size == maximumScrollback) {
                scrollback.removeFirst()
            }
            scrollback.addLast(screen.first())
            screen.removeFirst()
            screen.addLast(Array(width) { Cell() })
        }
        if (Cursor.x < 0) {
            Cursor.x = width - 1
            Cursor.y--
        }
        if (Cursor.y < 0) {
            Cursor.x = 0
            Cursor.y = 0
        }
    }

    /**
     * Retrieves a character on the screen by its x and y coordinates.
     */
    fun getCharAtScreenPosition(x: Int, y: Int): Char {
        return screen[y][x].character
    }

    /**
     * Retrieves the attributes of a character on the screen by its x and y coordinates.
     */
    fun getAttributesAtScreenPosition(x: Int, y: Int): Attributes{
        return screen[y][x].attributes
    }

    /**
     * Retrieves a character on the scrollback by its x and y coordinates.
     */
    fun getCharAtScrollbackPosition(x: Int, y: Int): Char {
        return scrollback[y][x].character
    }

    /**
     * Retrieves the entire content on the screen as a string.
     */
    fun getScreenContent(): String{
        return getContent(screen)
    }

    /**
     * Retrieves the entire content on the scrollback and the screen as a string.
     */
    fun getAllContent(): String{
        return getContent(scrollback) + getContent(screen)
    }

    /**
     * Returns the content inside a 2D array of cells as string.
     */
    private fun getContent(array: ArrayDeque<Array<Cell>>): String{
        val output =  array.joinToString("") { row ->
            buildString {
                for (cell in row) {
                    if (cell.charType == CharacterWidthType.WIDE_END) continue
                    append(cell.character)
                }
            }.trimEnd { it == ' ' }
        }
        return output
    }

    /**
     * Retrieves the content on a screen row as a string.
     */
    fun getScreenRowAsString(y: Int): String {
        if(y >= height)
            throw IndexOutOfBoundsException()
        val output = buildString {
            for (cell in screen[y]) append(cell.character)
        }.trimEnd()
        return output
    }

    /**
     * Retrieves the content on a screen row as a string.
     */
    fun getScrollbackRowAsString(y: Int): String {
        if(y >= maximumScrollback)
            throw IndexOutOfBoundsException()
        val output = buildString {
            for (cell in scrollback[y]) append(cell.character)
        }.trimEnd()
        return output
    }

    /**
     * Verifies whether a BMP character takes one or two spaces when rendered on a terminal.
     */
    fun isWide(char: Char): Boolean {

        val codePoint = char.code

        return codePoint in 0x1100..0x115F ||
        codePoint in 0x2E80..0xA4CF ||
        codePoint in 0xAC00..0xD7A3 ||
        codePoint in 0xF900..0xFAFF ||
        codePoint in 0xFE10..0xFE6F ||
        codePoint in 0xFF00..0xFF60 ||
        codePoint in 0xFFE0..0xFFE6
    }

}

private data class Cell(
    var character: Char = ' ',
    var charType: CharacterWidthType = CharacterWidthType.SIMPLE,
    var attributes: Attributes = Attributes(),
    var lineStart: Boolean = false
)

enum class CharacterWidthType {
    SIMPLE,
    WIDE_START,
    WIDE_END
}

enum class Colors {
    RED,
    GREEN,
    YELLOW,
    BLUE,
    BLACK,
    WHITE,
    MAGENTA,
    CYAN,
    GRAY,
    DARKGRAY,
    LIGHTGRAY,
    LIGHTGREEN,
    LIGHTBLUE,
    LIGHTRED,
    LIGHTCYAN,
    LIGHTMAGENTA
}

data class Attributes(var foregroundColor: Colors = Colors.WHITE,
                      var backgroundColor: Colors = Colors.BLACK,
                      var style: Int = 0)

enum class Directions {
    UP, DOWN, LEFT, RIGHT
}

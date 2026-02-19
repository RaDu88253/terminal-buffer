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

    fun getForegroundColor(): Colors {
        return currentAttributes.foregroundColor
    }

    fun getBackgroundColor(): Colors {
        return currentAttributes.backgroundColor
    }

    fun getStyle(): Int {
        return currentAttributes.style
    }

    fun getCursorPosition(): Pair<Int, Int> {
        return Pair(Cursor.x, Cursor.y)
    }

    fun setCursorPosition(x: Int, y: Int) {
        Cursor.x = x.coerceIn(0, width - 1)
        Cursor.y = y.coerceIn(0, height - 1)
    }

    fun moveCursor(direction: Directions, amount: Int = 1) {
        when (direction) {
            Directions.RIGHT -> {
                Cursor.x += amount
            }

            Directions.LEFT -> {
                Cursor.x -= amount
            }

            Directions.DOWN -> {
                Cursor.y += amount
            }

            Directions.UP -> {
                Cursor.y -= amount
            }
        }

        Cursor.x = Cursor.x.coerceIn(0, width - 1)
        Cursor.y = Cursor.y.coerceIn(0, height - 1)

    }

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
                    screen[Cursor.y][Cursor.x].character = char
                    screen[Cursor.y][Cursor.x].attributes = currentAttributes.copy()
                    Cursor.x++
                    wrapCursorUpdateScrollback()
                }
            }
        }
    }

    /**
     * Inserts a text containing no line breaks at the current cursor position, wrapping lines if necessary
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
        writeFromCells(cellsToRewrite)
        Cursor.x = x
        Cursor.y = y
        wrapCursorUpdateScrollback()
    }

    private fun writeFromCells(cells: ArrayDeque<Cell>) {
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

    fun fillCurrentLine(character: Char){
        write(character.toString().repeat(width - Cursor.x))
        wrapCursorUpdateScrollback()
    }

    fun wrapCursorUpdateScrollback() {
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

    fun getCharAtScreenPosition(x: Int, y: Int): Char {
        if (y >= height) {
            throw IndexOutOfBoundsException("Screen only has lines up to index $(height - 1)")
        }
        return screen[y][x].character
    }

    fun getAttributesAtScreenPosition(x: Int, y: Int): Attributes{
        return screen[y][x].attributes
    }

    fun getCharAtScrollbackPosition(x: Int, y: Int): Char {
        if (y >= scrollback.size) {
            throw IndexOutOfBoundsException("Scrollback only has lines up to index $(scrollback.size - 1)")
        }
        return scrollback[y][x].character
    }

    fun getScreenContentAsString(): String{
        val output =  screen.joinToString("") { row ->
            buildString {
                for (cell in row) append(cell.character)
            }.trimEnd { it == ' ' }
        }
        return output
    }

    fun getScrollbackAndScreenContentAsString(): String{

        //TODO: implement
        return ""

    }

    fun getScreenRowAsString(y: Int): String {
        if(y >= height)
            throw IndexOutOfBoundsException()
        val output = buildString {
            for (cell in screen[y]) append(cell.character)
        }.trimEnd { it == ' ' }
        return output
    }

    fun getScrollbackRowAsString(y: Int): String {
        if(y >= maximumScrollback)
            throw IndexOutOfBoundsException()
        val output = buildString {
            for (cell in scrollback[y]) append(cell.character)
        }.trimEnd { it == ' ' }
        return output
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

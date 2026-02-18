package org.terminalbuffer

class TerminalBuffer(val width: Int = 80, val height: Int = 24, val maximumScrollback: Int = 100) {

    private val content = Array(height) {
        Array(width) {Cell()}
    }
    private val scrollback = Array(maximumScrollback){
        Array(width) {Cell()}
    }
    private object Cursor{
        var x = 0
        var y = 0
        var maxY = 0
    }
    private object CurrentAttributes{
        var foregroundColor = Colors.WHITE
        var backgroundColor = Colors.BLACK
        var style = 0
    }

    val styleMap = mapOf("BOLD" to (1 shl 0), "ITALIC" to (1 shl 1), "UNDERLINE" to (1 shl 2), "DEFAULT" to 0)

    fun setCurrentAttributes(
        foregroundColor: Colors = CurrentAttributes.foregroundColor,
        backgroundColor: Colors = CurrentAttributes.backgroundColor,
        vararg styles: String
    ) {
        CurrentAttributes.foregroundColor = foregroundColor
        CurrentAttributes.backgroundColor = backgroundColor
        if (!styles.isEmpty())
            CurrentAttributes.style = 0;
        for (style in styles){
            CurrentAttributes.style = CurrentAttributes.style or styleMap.getOrDefault(style, 0)
        }
    }

    fun getForegroundColor() : Colors{
        return CurrentAttributes.foregroundColor
    }

    fun getBackgroundColor() : Colors{
        return CurrentAttributes.backgroundColor
    }

    fun getStyle() : Int{
        return CurrentAttributes.style
    }

    fun getCursorPosition() : Pair<Int, Int>{
        return Pair(Cursor.x, Cursor.y)
    }

    fun setCursorPosition(x: Int, y: Int){
        Cursor.x = x.coerceIn(0, width - 1)
        Cursor.y = y.coerceIn(0, height - 1)
    }

    fun moveCursor(direction: Directions, amount: Int = 1){
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


}

private data class Cell(
    val character: Char = ' ',
    val charType: CharacterWidthType = CharacterWidthType.SIMPLE,
    val foregroundColor: Colors = Colors.WHITE,
    val backgroundColor: Colors = Colors.BLACK,
    val style: Int = 0,
    val lineStart: Boolean = false
)

enum class CharacterWidthType{
    SIMPLE,
    WIDE_START,
    WIDE_END
}

enum class Colors{
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

enum class Directions{
    UP, DOWN, LEFT, RIGHT
}

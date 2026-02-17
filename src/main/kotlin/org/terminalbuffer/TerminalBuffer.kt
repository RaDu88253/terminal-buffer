package org.terminalbuffer

class TerminalBuffer(val width: Int, val height: Int, val maximumScrollback: Int = 100) {

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
    private object CurrentArributes{
        var foregroundColor = Colors.WHITE
        var backgroundColor = Colors.BLACK
        var style = 0
    }

}

private data class Cell(
    val character: Char = ' ',
    val charType: CharacterWidthType = CharacterWidthType.SIMPLE,
    val foregroundColor: Colors = Colors.WHITE,
    val backgroundColor: Colors = Colors.BLACK,
    val style: Int = 0)

private enum class CharacterWidthType{
    SIMPLE,
    WIDE_START,
    WIDE_END
}

private enum class Colors{
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

private object Style{
    const val BOLD = 1 shl 0
    const val ITALIC = 1 shl 1
    const val UNDERLINE = 1 shl 2
}
package com.lgtv.rs232.cmp.serial

/**
 * โปรโตคอล LG RS232
 * ส่ง:  "[cmd1][cmd2] [SetID hex] [Data hex]\r"  เช่น "ka 01 FF"
 * ตอบ: "[cmd2] [SetID] OK[data]x" หรือ "[cmd2] [SetID] NG[data]x"
 */
object LgProtocol {
    const val TERMINATOR = 'x'

    fun toHex(value: Int): String =
        value.coerceIn(0, 255).toString(16).uppercase().padStart(2, '0')

    fun command(cmd: String, setId: Int, dataHex: String): String =
        "$cmd ${toHex(setId)} $dataHex\r"

    data class Response(val cmd2: Char, val setId: String, val ok: Boolean, val dataHex: String)

    private val responseRegex =
        Regex("""^([a-z])\s+([0-9a-fA-F]{2})\s+(OK|NG)([0-9a-fA-F]{2,4})x$""", RegexOption.IGNORE_CASE)

    fun parse(message: String): Response? {
        val m = responseRegex.matchEntire(message.trim()) ?: return null
        return Response(
            cmd2 = m.groupValues[1].lowercase()[0],
            setId = m.groupValues[2],
            ok = m.groupValues[3].equals("OK", ignoreCase = true),
            dataHex = m.groupValues[4].uppercase()
        )
    }
}

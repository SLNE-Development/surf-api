package dev.slne.surf.api.common.font

private const val SMALL_CAPS_LETTERS = "ᴀʙᴄᴅᴇғɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ"

private val smallCapsTable = CharArray(128) { it.toChar() }.also { table ->
    SMALL_CAPS_LETTERS.forEachIndexed { i, smallCapsChar ->
        table['a'.code + i] = smallCapsChar
        table['A'.code + i] = smallCapsChar
    }
}

/**
 * Converts this character sequence to small caps formatting.
 *
 * Small caps are Unicode characters that resemble uppercase letters but are approximately
 * the height of lowercase letters.
 *
 * **Case is not preserved.** Every letter is lowercased before it is looked up, so `A` and `a`
 * both come out as `ᴀ` — the result reads as uniform small caps rather than as small caps with
 * real capitals standing above them. That is deliberate for a label, and worth knowing before
 * passing a name or a sentence through it. Digits stay as they are, and anything with no small
 * caps form is left alone.
 *
 * @return A new string with every letter converted to its small caps Unicode character.
 */
fun CharSequence.toSmallCaps(): String {
    val chars = CharArray(length)

    for (i in indices) {
        val c = this[i]
        chars[i] = if (c.code < 128) {
            smallCapsTable[c.code]
        } else {
            val lower = c.lowercaseChar()
            if (lower in 'a'..'z') smallCapsTable[lower.code] else c
        }
    }

    return String(chars)
}
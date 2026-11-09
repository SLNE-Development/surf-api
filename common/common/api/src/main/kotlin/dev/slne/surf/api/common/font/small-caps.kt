package dev.slne.surf.api.common.font

/**
 * A character mapping table from lowercase ASCII letters and digits to their Unicode small caps equivalents.
 *
 * Maps lowercase letters a-z to their corresponding small caps characters (ᴀ-ᴢ) and preserves digits 0-9.
 * Some letters like 's' and 'x' map to themselves as they lack distinct small caps Unicode characters.
 */
private val smallCapsMap = mapOf(
    'a' to 'ᴀ', 'b' to 'ʙ', 'c' to 'ᴄ', 'd' to 'ᴅ', 'e' to 'ᴇ', 'f' to 'ғ',
    'g' to 'ɢ', 'h' to 'ʜ', 'i' to 'ɪ', 'j' to 'ᴊ', 'k' to 'ᴋ', 'l' to 'ʟ',
    'm' to 'ᴍ', 'n' to 'ɴ', 'o' to 'ᴏ', 'p' to 'ᴘ', 'q' to 'ǫ', 'r' to 'ʀ',
    's' to 's', 't' to 'ᴛ', 'u' to 'ᴜ', 'v' to 'ᴠ', 'w' to 'ᴡ', 'x' to 'x',
    'y' to 'ʏ', 'z' to 'ᴢ',
    '0' to '0', '1' to '1', '2' to '2', '3' to '3', '4' to '4',
    '5' to '5', '6' to '6', '7' to '7', '8' to '8', '9' to '9'
)

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
        chars[i] = smallCapsMap[c.lowercaseChar()] ?: c
    }
    return String(chars)
}
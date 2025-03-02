package dev.slne.surf.surfapi.core.api.font

import dev.slne.surf.surfapi.core.api.util.char2CharMapOf

private val smallCapsMap = char2CharMapOf(
    'a' to 'ᴀ', 'b' to 'ʙ', 'c' to 'ᴄ', 'd' to 'ᴅ', 'e' to 'ᴇ', 'f' to 'ғ',
    'g' to 'ɢ', 'h' to 'ʜ', 'i' to 'ɪ', 'j' to 'ᴊ', 'k' to 'ᴋ', 'l' to 'ʟ',
    'm' to 'ᴍ', 'n' to 'ɴ', 'o' to 'ᴏ', 'p' to 'ᴘ', 'q' to 'ǫ', 'r' to 'ʀ',
    's' to 's', 't' to 'ᴛ', 'u' to 'ᴜ', 'v' to 'ᴠ', 'w' to 'ᴡ', 'x' to 'x',
    'y' to 'ʏ', 'z' to 'ᴢ',
    '0' to '0', '1' to '1', '2' to '2', '3' to '3', '4' to '4',
    '5' to '5', '6' to '6', '7' to '7', '8' to '8', '9' to '9'
)


fun String.toSmallCaps(): String {
    val result = StringBuilder(length)
    for (char in this) {
        result.append(smallCapsMap.getOrDefault(char.lowercaseChar(), char))
    }

    return result.toString()
}
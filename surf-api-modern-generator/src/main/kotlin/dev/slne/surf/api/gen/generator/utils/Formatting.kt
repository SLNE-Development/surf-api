package dev.slne.surf.api.gen.generator.utils

import org.apache.commons.lang3.math.NumberUtils
import java.util.*

object Formatting {

    val ALPHABETIC_KEY_ORDER = alphabeticKeyOrder<String> { it }
    private val ILLEGAL_FIELD_CHARACTERS = "[.-/]".toPattern()

    fun formatKeyAsField(path: String): String =
        ILLEGAL_FIELD_CHARACTERS.matcher(path.uppercase(Locale.ROOT)).replaceAll("_")

    fun <T> alphabeticKeyOrder(mapper: (T) -> String) = Comparator<T> { o1, o2 ->
        val path1 = mapper(o1)
        val path2 = mapper(o2)

        val trailingInt1 = tryParseTrailingInt(path1)
        val trailingInt2 = tryParseTrailingInt(path2)

        if (trailingInt1 != null && trailingInt2 != null) {
            trailingInt1.compareTo(trailingInt2)
        } else {
            path1.compareTo(path2)
        }
    }

    private fun tryParseTrailingInt(path: String): Int? {
        val delimiterIndex = path.lastIndexOf('_')
        if (delimiterIndex != -1) {
            val score = path.substring(delimiterIndex + 1)
            if (NumberUtils.isDigits(score)) {
                return Integer.parseInt(score)
            }
        }
        return null
    }
}
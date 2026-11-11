package dev.slne.surf.microservice.runtime.command


/**
 * Splits a console line into a label and its arguments.
 *
 * Whitespace separates tokens. Single quotes keep their content literally. Double quotes group their content, and a
 * backslash outside single quotes takes the next character literally, also inside double quotes. Quotes may form
 * part of a token (`a"b c"` is `ab c`), and `""` is an empty token.
 */
internal object CommandLineTokenizer {
    private const val SINGLE_QUOTE = '\''
    private const val DOUBLE_QUOTE = '"'
    private const val BACKSLASH = '\\'

    /**
     * @throws CommandLineSyntaxException if a quote is not closed or the line ends with a backslash
     */
    fun tokenize(line: String): List<String> {
        val tokens = ArrayList<String>()
        val token = StringBuilder()
        var hasToken = false
        var quote: Char? = null
        var index = 0

        while (index < line.length) {
            val char = line[index++]
            when {
                quote == SINGLE_QUOTE && char != SINGLE_QUOTE -> token.append(char)
                char == BACKSLASH -> {
                    if (index == line.length) {
                        throw CommandLineSyntaxException("The command ends with a backslash.")
                    }

                    token.append(line[index++])
                    hasToken = true
                }

                char == quote -> quote = null
                quote != null -> token.append(char)
                char == SINGLE_QUOTE || char == DOUBLE_QUOTE -> {
                    quote = char
                    hasToken = true
                }

                char.isWhitespace() -> if (hasToken) {
                    tokens += token.toString()
                    token.clear()
                    hasToken = false
                }

                else -> {
                    token.append(char)
                    hasToken = true
                }
            }
        }

        if (quote != null) {
            throw CommandLineSyntaxException("The command contains an unterminated $quote quote.")
        }

        if (hasToken) {
            tokens += token.toString()
        }

        return tokens
    }
}

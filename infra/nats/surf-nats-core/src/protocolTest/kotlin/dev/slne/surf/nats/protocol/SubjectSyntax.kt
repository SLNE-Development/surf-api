package dev.slne.surf.nats.protocol

/**
 * Checks NATS publish subjects; the tests use it as an oracle for the subjects the protocol builds.
 */
internal object SubjectSyntax {

    /**
     * Returns whether [subject] is a valid publish subject: non-empty tokens joined by single `.`, without `*`, `>`
     * or whitespace.
     */
    fun isValid(subject: String): Boolean {
        if (subject.isEmpty()) return false

        var atTokenStart = true

        for (char in subject) {
            when {
                char == '.' -> {
                    if (atTokenStart) return false
                    atTokenStart = true
                }

                char == '*' || char == '>' || isWhitespace(char) ->
                    return false

                else -> atTokenStart = false
            }
        }

        return !atTokenStart
    }

    // The whitespace set of java.util.regex `\s` without UNICODE_CHARACTER_CLASS.
    private fun isWhitespace(char: Char): Boolean {
        return char == ' ' ||
                char == '\t' ||
                char == '\n' ||
                char == '\u000B' ||
                char == '\u000C' ||
                char == '\r'
    }
}

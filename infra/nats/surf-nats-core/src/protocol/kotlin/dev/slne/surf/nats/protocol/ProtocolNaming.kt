package dev.slne.surf.nats.protocol

internal object ProtocolNaming {

    /**
     * Converts a declared name to kebab-case.
     *
     * Words split between an ASCII lowercase letter or digit and an ASCII uppercase letter, and before the last
     * uppercase letter of an uppercase run that is followed by a lowercase letter. All other characters, including
     * `_` and `-`, stay as they are; the result is lowercased.
     */
    fun kebabCase(name: String): String {
        val result = StringBuilder(name.length + 4)
        var start = 0
        for (index in 1 until name.length) {
            if (isWordBoundary(name, index)) {
                result.append(name.substring(start, index).lowercase()).append('-')
                start = index
            }
        }
        return result.append(name.substring(start).lowercase()).toString()
    }

    fun isValidToken(token: String): Boolean {
        if (token.isEmpty()) return false
        for (char in token) {
            if (char == '.' || char == '*' || char == '>' || isSubjectWhitespace(char)) return false
        }
        return true
    }

    fun isReservedToken(token: String): Boolean = token.startsWith('$')

    fun isValidNameToken(token: String): Boolean = isValidToken(token) && !isReservedToken(token)

    fun isValidSubject(subject: String): Boolean = subject.split('.').all(::isValidToken)

    fun isValidEventSubject(subject: String): Boolean = subject.split('.').all(::isValidNameToken)

    fun requireValidSubject(subject: String, source: Any): String {
        require(isValidSubject(subject)) { "Invalid NATS subject '$subject' for $source" }
        return subject
    }

    fun requireValidEventSubject(subject: String, source: Any): String {
        requireValidSubject(subject, source)
        require(subject.split('.').none(::isReservedToken)) {
            "Invalid event subject '$subject' for $source: tokens starting with '$' are reserved"
        }
        return subject
    }

    /**
     * Returns whether [name] consists of `[a-z0-9]` words joined by single `-`, the format of client names.
     */
    fun isKebabName(name: String): Boolean {
        if (name.isEmpty() || name.last() == '-') return false
        var previous = '-'
        for (char in name) {
            val valid = if (char == '-') previous != '-' else isAsciiLower(char) || isAsciiDigit(char)
            if (!valid) return false
            previous = char
        }
        return true
    }

    /**
     * Returns the major version of a proto package that ends with `.v<N>`, or `null` without such a suffix.
     *
     * `N` must be at least 1 and is written without leading zeros, so the package suffix and the version token of
     * subjects and streams are identical.
     */
    fun majorVersionOrNull(protoPackage: String): Int? {
        val dot = protoPackage.lastIndexOf('.')
        if (dot <= 0) return null
        val segment = protoPackage.substring(dot + 1)
        if (segment.length < 2 || segment[0] != 'v' || segment[1] == '0') return null
        for (index in 1 until segment.length) {
            if (!isAsciiDigit(segment[index])) return null
        }
        return segment.substring(1).toIntOrNull()
    }

    /**
     * Returns the major version like [majorVersionOrNull]; [source] names the declaration in the error message.
     */
    fun majorVersion(protoPackage: String, source: String? = null): Int = majorVersionOrNull(protoPackage)
        ?: throw IllegalArgumentException(
            "Proto package '$protoPackage'${if (source == null) "" else " of $source"} must end with a major version " +
                    "suffix '.v<N>' (N >= 1, no leading zeros)"
        )

    fun requireClientName(name: String, what: String = "NATS client name"): String {
        require(isKebabName(name)) { "Invalid $what '$name': must be kebab-case ([a-z0-9] separated by '-')" }
        return name
    }

    /**
     * Returns why [name] cannot be the resolved name of the service [label], or `null`.
     *
     * Service names are kebab-case; `dlq` and names ending in `-dlq` are reserved because their stream would collide
     * with the dead-letter stream of another service.
     */
    fun serviceNameProblem(name: String, label: String): String? = when {
        !isKebabName(name) -> "service $label resolves to the name '$name', which is not kebab-case " +
                "([a-z0-9] words joined by '-'); set (surf.nats.v1.service).name"

        name == "dlq" || name.endsWith("-dlq") ->
            "service name '$name' is reserved: its stream would collide with the dead-letter stream of another service"

        else -> null
    }

    private fun isWordBoundary(name: String, index: Int): Boolean {
        val previous = name[index - 1]
        val current = name[index]
        if ((isAsciiLower(previous) || isAsciiDigit(previous)) && isAsciiUpper(current)) return true
        return isAsciiUpper(previous) && isAsciiUpper(current) &&
                index + 1 < name.length && isAsciiLower(name[index + 1])
    }

    // The whitespace set of java.util.regex `\s` without UNICODE_CHARACTER_CLASS.
    private fun isSubjectWhitespace(char: Char): Boolean =
        char == ' ' || char == '\t' || char == '\n' || char == '\u000B' || char == '\u000C' || char == '\r'

    private fun isAsciiLower(char: Char): Boolean = char in 'a'..'z'

    private fun isAsciiUpper(char: Char): Boolean = char in 'A'..'Z'

    private fun isAsciiDigit(char: Char): Boolean = char in '0'..'9'
}

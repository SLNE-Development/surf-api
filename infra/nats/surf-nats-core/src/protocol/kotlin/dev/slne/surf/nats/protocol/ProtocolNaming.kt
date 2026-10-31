package dev.slne.surf.nats.protocol

internal object ProtocolNaming {

    /**
     * The rule of [isProtoName] for error messages.
     */
    const val PROTO_NAME_RULE =
        "every name component must start with an ASCII letter and contain only ASCII letters, digits and '_'"

    /**
     * Converts a declared name to kebab-case.
     *
     * Words split between an ASCII lowercase letter or digit and an ASCII uppercase letter, and before the last
     * uppercase letter of an uppercase run that is followed by a lowercase letter. All other characters, including
     * `_` and `-`, stay as they are; the result is lowercased.
     */
    fun kebabCase(name: String): String = buildString(name.length + 4) {
        for (i in name.indices) {
            if (i > 0 && isWordBoundary(name, i)) {
                append('-')
            }
            append(name[i])
        }
    }.lowercase()

    /**
     * Returns whether [name] is a proto full name: identifiers joined by single `.`, each starting with an ASCII
     * letter and containing only ASCII letters, digits and `_`.
     *
     * Subjects and stream names embed such names unchanged; the leading letter keeps the stream name encoding of
     * [ProtocolStreams] injective.
     */
    fun isProtoName(name: String): Boolean {
        if (name.isEmpty()) return false

        var atStart = true
        for (char in name) {
            if (char == '.') {
                if (atStart) return false
                atStart = true
            } else {
                val valid = isAsciiLetter(char) || !atStart && (isAsciiDigit(char) || char == '_')
                if (!valid) return false
                atStart = false
            }
        }
        return !atStart
    }

    /**
     * Returns whether [name] is a single component of a proto full name ([isProtoName]), without `.`.
     */
    fun isProtoIdentifier(name: String): Boolean = '.' !in name && isProtoName(name)

    /**
     * Returns why [fullName] cannot name a [kind] declaration, or `null`.
     *
     * [fullName] must be a proto full name ([isProtoName]). [declaredName], when given, is the name the declaration
     * itself declares and must not contain `.`: some parsers accept dotted declared names, which would add subject
     * tokens and collide with other full names.
     */
    fun nameProblem(kind: String, fullName: String, declaredName: String? = null): String? = when {
        declaredName != null && '.' in declaredName -> "$kind $fullName: the declared name '$declaredName' must not contain '.'"
        !isProtoName(fullName) -> "$kind $fullName: $PROTO_NAME_RULE"
        else -> null
    }

    /**
     * Returns [name] when [nameProblem] reports nothing for it, else throws [IllegalArgumentException] with that
     * problem.
     */
    fun requireProtoName(name: String, kind: String): String {
        nameProblem(kind, name)?.let { throw IllegalArgumentException(it) }
        return name
    }

    /**
     * Returns whether [name] consists of `[a-z0-9]` words joined by single `-`, the format of client names.
     */
    fun isKebabName(name: String): Boolean {
        if (name.isEmpty() || name.last() == '-') return false

        var previous = '-'
        for (char in name) {
            val valid = if (char == '-') {
                previous != '-'
            } else {
                isAsciiLower(char) || isAsciiDigit(char)
            }

            if (!valid) return false
            previous = char
        }
        return true
    }

    /**
     * Returns the major version of a proto package that ends with `.v<N>`, or `null` without such a suffix.
     *
     * `N` must be at least 1 and is written without leading zeros, so each major version has exactly one package
     * spelling in subjects and stream names.
     */
    fun majorVersionOrNull(protoPackage: String): Int? {
        val dot = protoPackage.lastIndexOf('.')
        if (dot <= 0 || dot + 2 >= protoPackage.length || protoPackage[dot + 1] != 'v') return null

        val digits = protoPackage.substring(dot + 2)
        if (digits[0] == '0' || digits.any { !isAsciiDigit(it) }) return null

        return digits.toIntOrNull()
    }

    /**
     * Returns why a file with the proto package [protoPackage] cannot declare events or services, or `null` when the
     * package has a major version suffix ([majorVersionOrNull]).
     */
    fun contractPackageProblem(protoPackage: String): String? {
        if (majorVersionOrNull(protoPackage) != null) return null
        return "package '$protoPackage' declares events or services and must end with a major version '.v<N>' " +
                "(N >= 1, no leading zeros)"
    }

    /**
     * Returns the major version like [majorVersionOrNull]; [source] names the declaration in the error message.
     */
    fun majorVersion(protoPackage: String, source: String? = null): Int {
        return majorVersionOrNull(protoPackage)
            ?: throw IllegalArgumentException(
                "Proto package '$protoPackage'${if (source == null) "" else " of $source"} must end with a major version " +
                        "suffix '.v<N>' (N >= 1, no leading zeros)"
            )
    }

    fun requireClientName(name: String, what: String = "NATS client name"): String {
        require(isKebabName(name)) { "Invalid $what '$name': must be kebab-case ([a-z0-9] separated by '-')" }
        return name
    }

    @Suppress("RedundantIf")
    private fun isWordBoundary(name: String, index: Int): Boolean {
        val previous = name[index - 1]
        val current = name[index]

        if (
            (isAsciiLower(previous) || isAsciiDigit(previous)) &&
            isAsciiUpper(current)
        ) return true

        return isAsciiUpper(previous) &&
                isAsciiUpper(current) &&
                index + 1 < name.length &&
                isAsciiLower(name[index + 1])
    }

    private fun isAsciiLower(char: Char): Boolean = char in 'a'..'z'

    private fun isAsciiLetter(char: Char): Boolean = isAsciiLower(char) || isAsciiUpper(char)

    private fun isAsciiUpper(char: Char): Boolean = char in 'A'..'Z'

    private fun isAsciiDigit(char: Char): Boolean = char in '0'..'9'
}

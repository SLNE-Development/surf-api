package dev.slne.surf.nats.gradle.model

import dev.slne.surf.nats.protocol.ProtocolNaming

/**
 * Provides naming conventions and identifier validation for generated Kotlin declarations.
 *
 * Converts Protobuf identifiers into Kotlin-compatible names and derives enum
 * naming conventions used by both the Kotlin generator and protobuf-es.
 */
internal object KotlinNaming {

    /**
     * Converts a Protobuf identifier into lowerCamelCase.
     *
     * Joins underscore-separated parts, lowercases the first character of the
     * first part, and uppercases the first character of subsequent parts.
     * Empty parts are ignored.
     *
     * For example, both `foo_bar` and `FooBar` become `fooBar`.
     *
     * @param name the Protobuf identifier to convert
     * @return the lowerCamelCase identifier, or the original name if no parts remain
     */
    fun lowerCamel(name: String): String {
        val parts = name.split('_').filter(String::isNotEmpty)
        if (parts.isEmpty()) return name

        return buildString {
            append(parts.first().replaceFirstChar(Char::lowercaseChar))
            for (part in parts.drop(1)) append(part.replaceFirstChar(Char::uppercaseChar))
        }
    }

    /**
     * Converts a Protobuf identifier into UpperCamelCase.
     *
     * Uses [lowerCamel] and uppercases the first character of the result.
     *
     * @param name the Protobuf identifier to convert
     * @return the UpperCamelCase identifier
     */
    fun upperCamel(name: String): String {
        return lowerCamel(name).replaceFirstChar(Char::uppercaseChar)
    }

    /**
     * Derives the mandatory prefix for Protobuf enum values.
     *
     * Converts the enum name into UPPER_SNAKE_CASE and appends a trailing
     * underscore. For example, `ResultKind` becomes `RESULT_KIND_`.
     *
     * @param enumName the Protobuf enum name
     * @return the required prefix for its enum values
     */
    fun enumValuePrefix(enumName: String): String {
        return ProtocolNaming.kebabCase(enumName)
            .uppercase()
            .replace('-', '_') + "_"
    }

    /**
     * Derives the enum value prefix removed by protobuf-es.
     *
     * Reproduces the `camelToSnakeCase` naming behavior of
     * `@bufbuild/protobuf` 2.16 by inserting an underscore before every
     * ASCII uppercase letter except the first character.
     *
     * All characters are uppercased, and a trailing underscore is appended.
     *
     * @param enumName the Protobuf enum name
     * @return the uppercase enum value prefix recognized by protobuf-es
     */
    fun protobufEsEnumValuePrefix(enumName: String): String = buildString {
        enumName.forEachIndexed { index, char ->
            if (index > 0 && char in 'A'..'Z') append('_')
            append(char.uppercaseChar())
        }
        append('_')
    }

    /**
     * Constructs an UpperCamelCase enum name from its expected value prefix.
     *
     * Splits the prefix at underscores, removes empty parts, and capitalizes
     * each remaining part after lowercasing it.
     *
     * The resulting name produces the given prefix under both the Kotlin
     * generator's naming rules and protobuf-es conventions.
     *
     * @param prefix the uppercase enum value prefix
     * @return the corresponding UpperCamelCase enum name
     */
    fun enumNameForPrefix(prefix: String): String {
        return prefix.split('_')
            .filter(String::isNotEmpty)
            .joinToString("") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }
    }

    /**
     * Checks whether a name is a valid identifier under the generator's naming rules.
     *
     * The identifier must begin with an ASCII letter and may contain only
     * ASCII letters, digits, and underscores.
     *
     * This check does not validate Kotlin keywords or reserved member names.
     *
     * @param name the identifier to validate
     * @return `true` if the identifier satisfies the naming rules, otherwise `false`
     */
    fun isIdentifier(name: String): Boolean {
        if (name.isEmpty() || !name.first().isAsciiLetter()) return false
        return name.all { it.isAsciiLetter() || it in '0'..'9' || it == '_' }
    }

    /**
     * Checks whether this character is an ASCII letter.
     *
     * @return `true` for characters in `a` through `z` or `A` through `Z`
     */
    private fun Char.isAsciiLetter(): Boolean {
        return this in 'a'..'z' || this in 'A'..'Z'
    }
}

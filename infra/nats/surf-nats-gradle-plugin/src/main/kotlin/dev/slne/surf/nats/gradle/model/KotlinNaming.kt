package dev.slne.surf.nats.gradle.model

import dev.slne.surf.nats.protocol.ProtocolNaming

internal object KotlinNaming {

    /**
     * Joins the `_`-separated parts of a proto identifier into `lowerCamelCase` (`foo_bar` and `FooBar` give `fooBar`).
     */
    fun lowerCamel(name: String): String {
        val parts = name.split('_').filter(String::isNotEmpty)
        if (parts.isEmpty()) return name
        return buildString {
            append(parts.first().replaceFirstChar(Char::lowercaseChar))
            for (part in parts.drop(1)) append(part.replaceFirstChar(Char::uppercaseChar))
        }
    }

    fun upperCamel(name: String): String = lowerCamel(name).replaceFirstChar(Char::uppercaseChar)

    /**
     * Returns the mandatory value prefix of an enum: its name in `UPPER_SNAKE_CASE` followed by `_`.
     */
    fun enumValuePrefix(enumName: String): String = ProtocolNaming.kebabCase(enumName).uppercase().replace('-', '_') + "_"

    /**
     * Returns the enum value prefix protoc-gen-es strips, upper-cased: `_` before every ASCII upper-case letter but
     * the first, plus a trailing `_` (`camelToSnakeCase` of `@bufbuild/protobuf` 2.16).
     */
    fun protobufEsEnumValuePrefix(enumName: String): String = buildString {
        enumName.forEachIndexed { index, char ->
            if (index > 0 && char in 'A'..'Z') append('_')
            append(char.uppercaseChar())
        }
        append('_')
    }

    /**
     * Returns the UpperCamelCase enum name whose value prefix is [prefix] for both the generator and protobuf-es.
     */
    fun enumNameForPrefix(prefix: String): String {
        return prefix.split('_').filter(String::isNotEmpty).joinToString("") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }
    }

    /**
     * Returns whether [name] starts with an ASCII letter and continues with ASCII letters, digits or `_`.
     */
    fun isIdentifier(name: String): Boolean {
        if (name.isEmpty() || !name.first().isAsciiLetter()) return false
        return name.all { it.isAsciiLetter() || it in '0'..'9' || it == '_' }
    }

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
}

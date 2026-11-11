package dev.slne.surf.nats.gradle.contract

import com.squareup.wire.schema.EnumConstant
import com.squareup.wire.schema.EnumType
import dev.slne.surf.nats.gradle.model.KotlinNaming
import dev.slne.surf.nats.gradle.validate.Problems

/**
 * Resolves the entries of generated enums.
 */
internal class EnumResolver(private val problems: Problems) {

    /**
     * Returns the Kotlin entry names of [type] with their constants in declaration order and reports enum and
     * value names that cannot be generated. The first constant per Kotlin name is kept.
     */
    fun resolve(type: EnumType): Map<String, EnumConstant> {
        val prefix = KotlinNaming.enumValuePrefix(type.name)

        validateEnum(type, prefix)
        return enumEntries(type, prefix)
    }

    /**
     * Validates enum-level rules: the TypeScript-compatible prefix, aliases, and first zero value.
     *
     * Errors are reported but do not prevent the remaining enum entries from being generated.
     */
    private fun validateEnum(type: EnumType, prefix: String) {
        val esPrefix = KotlinNaming.protobufEsEnumValuePrefix(type.name)
        if (esPrefix != prefix) {
            problems.error(
                type.location,
                "enum ${type.name}: protobuf-es derives the value prefix $esPrefix from the enum name, not $prefix, " +
                        "so TypeScript would keep the full value names; rename the enum to " +
                        "${KotlinNaming.enumNameForPrefix(prefix)} (abbreviations as one word, no '_')",
            )
        }
        if (type.allowAlias()) {
            problems.error(type.location, "enum ${type.name}: allow_alias is not supported")
        }
        val first = type.constants.firstOrNull()
        val zeroNames = listOf(prefix + "UNSPECIFIED", prefix + "UNKNOWN")
        if (first == null || first.tag != 0 || first.name !in zeroNames) {
            problems.error(
                type.location,
                "enum ${type.name} must declare ${zeroNames.joinToString(" or ")} = 0 as its first value"
            )
        }
    }

    /**
     * Collects valid enum entries in declaration order, retaining the first entry for each Kotlin name.
     *
     * Duplicate names are reported; entries with invalid prefixes, identifiers, or reserved
     * names are omitted. Negative numeric values are reported but retained where otherwise valid.
     *
     * @return Kotlin entry names paired with their original Protobuf constants
     */
    private fun enumEntries(type: EnumType, prefix: String): Map<String, EnumConstant> {
        val entries = LinkedHashMap<String, EnumConstant>()
        for (constant in type.constants) {
            val kotlinName = enumEntryName(constant, prefix) ?: continue
            entries.putIfAbsent(kotlinName, constant)?.let { previous ->
                problems.error(
                    constant.location,
                    "enum values ${previous.name} and ${constant.name} both map to the Kotlin entry $kotlinName"
                )
            }
        }
        return entries
    }

    /**
     * Validates an enum constant and returns its unprefixed Kotlin name.
     *
     * A negative numeric value is reported independently of whether the name is valid.
     *
     * @return the Kotlin name, or `null` if the constant is left out
     */
    private fun enumEntryName(constant: EnumConstant, prefix: String): String? {
        if (constant.tag < 0) {
            problems.error(
                constant.location,
                "enum value ${constant.name} has a negative number ${constant.tag}"
            )
        }
        if (!constant.name.startsWith(prefix)) {
            problems.error(
                constant.location,
                "enum value ${constant.name} must start with the prefix $prefix"
            )
            return null
        }
        val kotlinName = constant.name.removePrefix(prefix)
        if (!KotlinNaming.isIdentifier(kotlinName)) {
            problems.error(
                constant.location,
                "enum value ${constant.name} without its prefix ($kotlinName) must start with a letter and contain only letters, digits and '_'",
            )
            return null
        }
        if (kotlinName in RESERVED_ENTRY_NAMES) {
            problems.error(
                constant.location,
                "enum value ${constant.name} maps to the reserved Kotlin entry name $kotlinName"
            )
            return null
        }
        return kotlinName
    }

    private companion object {
        /**
         * Members of Kotlin enums and of the generated enum class that an entry name must not shadow.
         */
        val RESERVED_ENTRY_NAMES = setOf(
            "name",
            "ordinal",
            "entries",
            "values",
            "valueOf",
            GeneratedNames.ENUM_NUMBER,
            GeneratedNames.ENUM_SERIALIZER,
            GeneratedNames.COMPANION,
        )
    }
}

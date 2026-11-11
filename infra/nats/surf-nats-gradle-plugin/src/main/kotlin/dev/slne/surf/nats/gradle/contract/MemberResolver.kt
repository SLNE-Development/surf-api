package dev.slne.surf.nats.gradle.contract

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.model.*

/**
 * Resolves the constructor properties of generated messages.
 *
 * One instance serves one generator run: it keeps the source text read for `[default]` literals.
 */
internal class MemberResolver(private val types: KotlinTypeResolver) {
    private val problems get() = types.problems
    private val rawDefaults = RawDefaults()

    /**
     * Returns the constructor properties of [type] in constructor order and reports fields, defaults,
     * encodings, oneofs and property names that cannot be generated.
     *
     * A field that cannot be represented is left out; a member whose name clashes stays.
     */
    fun resolve(type: MessageType, className: ClassName): List<MessageMember> {
        val members = orderedMembers(
            type,
            field = ::fieldProperty,
            oneof = { oneOf, _ -> oneofProperty(oneOf, className) },
        ).filterNotNull()

        reportPropertyClashes(members)
        return members
    }

    /**
     * Reports members whose Kotlin property name an earlier member already uses.
     */
    private fun reportPropertyClashes(members: List<MessageMember>) {
        val owners = HashMap<String, MessageMember>()
        for (member in members) {
            val previous = owners.putIfAbsent(member.name, member) ?: continue
            problems.error(
                member.location,
                "'${member.protoName}' maps to the Kotlin property '${member.name}', which '${previous.protoName}' (${previous.location}) already uses",
            )
        }
    }

    /**
     * Resolves a field to a Kotlin constructor property, choosing its collection or scalar representation.
     *
     * @param field the Protobuf field to resolve
     * @return the property, or `null` when it cannot be generated
     */
    private fun fieldProperty(field: Field): FieldMember? {
        val protoType = field.type ?: return null
        return when {
            protoType.isMap -> mapProperty(field, protoType)
            field.isRepeated -> listProperty(field, protoType)
            else -> singleProperty(field, protoType)
        }
    }

    /**
     * Resolves a map property after resolving both element types and checking Protobuf encoding compatibility.
     *
     * Maps cannot declare defaults. Incompatible integer encodings prevent generation because
     * kotlinx-serialization-protobuf applies one encoding to both elements.
     *
     * @param field the original map field
     * @param protoType the Protobuf map type
     * @return the map property, or `null` if its types or encoding are unsupported
     */
    private fun mapProperty(field: Field, protoType: ProtoType): FieldMember? {
        val location = field.location
        if (field.default != null) problems.error(
            location,
            "[default] is not allowed on map field '${field.name}'"
        )
        val key = types.element(protoType.keyType!!, location)
        val value = types.element(protoType.valueType!!, location)
        if (key == null || value == null) return null
        if (!validateMapEncoding(field, key, value)) return null

        // kotlinx-serialization-protobuf applies one @ProtoType to both key and value.
        val encoding =
            listOfNotNull(key.encoding, value.encoding).firstOrNull { it != IntEncoding.DEFAULT }
        return FieldMember(
            field = field,
            name = KotlinNaming.lowerCamel(field.name),
            type = MAP.parameterizedBy(key.usageType, value.usageType),
            default = CodeBlock.of("emptyMap()"),
            comparison = if (value.isBytes) MemberComparison.CONTENT else MemberComparison.EQUALS,
            encoding = encoding,
            packed = false,
        )
    }

    /**
     * Reports incompatible integer encodings for a map's key and value.
     *
     * @return `true` if the map can be serialized with a shared integer encoding
     */
    private fun validateMapEncoding(field: Field, key: ElementType, value: ElementType): Boolean {
        val keyEncoding = key.encoding
        val valueEncoding = value.encoding
        if (keyEncoding != null && valueEncoding != null && keyEncoding != valueEncoding) {
            problems.error(
                field.location,
                "map<${key.protoType}, ${value.protoType}> '${field.name}' cannot be encoded by kotlinx-serialization-protobuf: " +
                        "key and value need the same integer encoding ($keyEncoding vs $valueEncoding)",
            )
            return false
        }
        return true
    }

    /**
     * Resolves a repeated Protobuf field as a Kotlin list.
     *
     * Defaults are reported as unsupported. The list is packed only for packable
     * element types; other packed declarations report an error.
     *
     * @return the list property, or `null` if the element type cannot be resolved
     */
    private fun listProperty(field: Field, protoType: ProtoType): FieldMember? {
        val location = field.location
        val element = types.element(protoType, location) ?: return null
        if (field.default != null) problems.error(
            location,
            "[default] is not allowed on repeated field '${field.name}'"
        )

        return FieldMember(
            field = field,
            name = KotlinNaming.lowerCamel(field.name),
            type = LIST.parameterizedBy(element.usageType),
            default = CodeBlock.of("emptyList()"),
            comparison = if (element.isBytes) MemberComparison.CONTENT else MemberComparison.EQUALS,
            encoding = element.encoding,
            packed = isPacked(field, element),
        )
    }

    /**
     * Returns whether a repeated field is encoded packed, and reports `[packed = true]` on an element type that
     * cannot be packed.
     */
    private fun isPacked(field: Field, element: ElementType): Boolean {
        if (!field.isPacked) return false
        if (element.packable) return true

        problems.error(
            field.location,
            "[packed = true] is not supported for repeated ${element.protoType} '${field.name}' " +
                    "(kotlinx-serialization-protobuf ignores the integer encoding of packed lists)",
        )
        return false
    }

    /**
     * Resolves a scalar or message-valued property, including its default and equality behavior.
     *
     * The proto2 label decides nullability: `required` fields are non-null without a default,
     * `optional` fields are nullable with a `null` default unless they declare a `[default]`.
     * Generated enum fields are always optional and non-null, defaulting to their zero value.
     * Floating-point properties use bitwise equality when content-aware equality methods are generated.
     *
     * @return the property, or `null` if its element type cannot be resolved
     */
    private fun singleProperty(field: Field, protoType: ProtoType): FieldMember? {
        val element = types.element(protoType, field.location) ?: return null
        val enumType = element.enumType
        val nullable = enumType == null && field.label != Field.Label.REQUIRED && field.default == null
        val default = when {
            enumType != null -> enumDefault(field, element, enumType)
            field.label == Field.Label.REQUIRED -> {
                reportDefaultOnRequired(field)
                null
            }

            nullable -> CodeBlock.of("null")
            else -> defaultValue(field, element)
        }
        return FieldMember(
            field = field,
            name = KotlinNaming.lowerCamel(field.name),
            type = if (nullable) element.usageType.copy(nullable = true) else element.usageType,
            default = default,
            comparison = singleComparison(element, nullable),
            encoding = element.encoding,
            packed = false,
        )
    }

    /**
     * Chooses the equality strategy for a non-collection property.
     *
     * Byte arrays use content equality, floating-point types compare their bit patterns,
     * and all other types use regular Kotlin equality.
     */
    private fun singleComparison(element: ElementType, nullable: Boolean): MemberComparison = when {
        element.isBytes -> MemberComparison.CONTENT
        element.kotlinType != FLOAT && element.kotlinType != DOUBLE -> MemberComparison.EQUALS
        nullable -> MemberComparison.NULLABLE_BITS
        else -> MemberComparison.BITS
    }

    /**
     * Reports a `[default]` on a `required` field.
     *
     * The encoder omits values equal to the default, so readers would reject the message as incomplete.
     */
    private fun reportDefaultOnRequired(field: Field) {
        if (field.default == null) return
        problems.error(
            field.location,
            "required field '${field.name}' cannot have a [default]; the encoder omits values equal to the default, " +
                    "so readers would reject the message (declare it optional)",
        )
    }

    /**
     * Resolves a Protobuf `[default]` literal of a scalar, string, or bytes field to the corresponding Kotlin
     * expression.
     *
     * Generated messages and mapped types do not support defaults.
     *
     * @param field the declaration containing the default literal
     * @param element the resolved type information
     * @return the default expression, or `null` if no usable default exists
     */
    private fun defaultValue(field: Field, element: ElementType): CodeBlock? {
        val literal = field.default ?: return null
        return when {
            element.isMapped || element.isMessage -> {
                problems.error(
                    field.location,
                    "[default] is not supported for ${element.protoType} field '${field.name}'"
                )
                null
            }

            element.protoType == ProtoType.STRING || element.protoType == ProtoType.BYTES ->
                rawDefault(field, element, literal)

            else -> scalarDefault(field, element, literal)
        }
    }

    /**
     * Resolves the Kotlin default of a generated enum field: the enum's zero-valued entry.
     *
     * Reports `required` and `[default]` on the field, since the zero value already means "not set". The generated
     * enum's prefix is removed from the Kotlin entry reference.
     *
     * @return the zero entry, or `null` if the enum declares no zero value
     */
    private fun enumDefault(field: Field, element: ElementType, enumType: EnumType): CodeBlock? {
        if (field.label == Field.Label.REQUIRED) {
            problems.error(
                field.location,
                "enum field '${field.name}' cannot be required; declare it optional, its zero value means \"not set\"",
            )
        }
        val zero = enumType.constants.firstOrNull { it.tag == 0 }
        if (field.default != null) {
            problems.error(
                field.location,
                "[default] is not allowed on enum field '${field.name}'; enum fields default to their zero value${
                    zero?.let { " ${it.name}" }.orEmpty()
                }",
            )
        }
        if (zero == null) return null
        return CodeBlock.of(
            "%T.%N",
            element.kotlinType,
            zero.name.removePrefix(KotlinNaming.enumValuePrefix(enumType.name))
        )
    }

    /**
     * Recovers the original bytes of a Protobuf string or bytes literal from the source file.
     *
     * The original literal is required to preserve escape sequences and their byte representation.
     * Missing or undecodable literals are reported and yield no default.
     */
    private fun rawDefault(field: Field, element: ElementType, literal: String): CodeBlock? {
        val bytes = rawDefaults.bytes(field.location, literal)
        if (bytes == null) {
            problems.error(
                field.location,
                "cannot read the [default] literal of '${field.name}' from its source file " +
                        "(no literal at this location decodes to the default wire-schema parsed)",
            )
            return null
        }
        return if (element.protoType == ProtoType.BYTES) DefaultValues.bytes(bytes)
        else DefaultValues.string(bytes.decodeToString())
    }

    /**
     * Parses a numeric or boolean default into a Kotlin source expression.
     *
     * @return the parsed expression, or `null` after reporting an invalid literal
     */
    private fun scalarDefault(field: Field, element: ElementType, literal: String): CodeBlock? =
        DefaultValues.scalar(element.protoType, literal) ?: run {
            problems.error(
                field.location,
                "invalid [default = $literal] for ${element.protoType} field '${field.name}'"
            )
            null
        }

    /**
     * Resolves the nullable property holding the active case of a oneof, with one case for each valid field.
     *
     * A null value represents an unset oneof. Cases follow their field numbers.
     *
     * @param oneOf the Protobuf oneof declaration
     * @param owner the enclosing message class
     * @return the oneof property and its cases
     */
    private fun oneofProperty(oneOf: OneOf, owner: ClassName): OneofMember {
        val interfaceName = owner.nestedClass(KotlinNaming.upperCamel(oneOf.name))
        validateOneofName(oneOf, interfaceName, owner)

        val caseOwners = HashMap<String, Field>()
        val cases = oneOf.fields.sortedBy { it.tag }
            .mapNotNull { oneofCase(oneOf, interfaceName, it, caseOwners) }
        return OneofMember(oneOf, KotlinNaming.lowerCamel(oneOf.name), interfaceName, cases)
    }

    /**
     * Reports collisions between a oneof interface and the enclosing message or companion object.
     */
    private fun validateOneofName(oneOf: OneOf, interfaceName: ClassName, owner: ClassName) {
        when (interfaceName.simpleName) {
            GeneratedNames.COMPANION -> problems.error(
                oneOf.location,
                "oneof '${oneOf.name}' clashes with the generated companion object"
            )

            owner.simpleName -> problems.error(
                oneOf.location,
                "oneof '${oneOf.name}' must not have the name of its enclosing message"
            )
        }
    }

    /**
     * Resolves a oneof case after checking field options, resolving its type, and validating its class name.
     *
     * Invalid names and options are reported and the case is kept; a case whose field type cannot be resolved is
     * left out.
     *
     * @param oneOf the containing oneof declaration
     * @param interfaceName the generated union interface
     * @param field the case field
     * @param caseOwners the first field claiming each generated case name
     * @return the case, or `null` if the field type cannot be resolved
     */
    private fun oneofCase(
        oneOf: OneOf,
        interfaceName: ClassName,
        field: Field,
        caseOwners: MutableMap<String, Field>,
    ): OneofCase? {
        val location = field.location
        if (field.default != null) problems.error(
            location,
            "[default] is not allowed on oneof field '${field.name}'"
        )
        val element = field.type?.let { types.element(it, location) } ?: return null

        val caseName = interfaceName.nestedClass(KotlinNaming.upperCamel(field.name))
        validateOneofCaseName(oneOf, interfaceName, field, caseName, caseOwners)

        val value = FieldMember(
            field = field,
            name = VALUE,
            type = element.usageType,
            default = null,
            comparison = if (element.isBytes) MemberComparison.CONTENT else MemberComparison.EQUALS,
            encoding = element.encoding,
            packed = false,
        )
        return OneofCase(caseName, value)
    }

    /**
     * Reports reserved or duplicate oneof case class names while retaining the first owner.
     *
     * A later field with the same name keeps its case.
     */
    private fun validateOneofCaseName(
        oneOf: OneOf,
        interfaceName: ClassName,
        field: Field,
        caseName: ClassName,
        caseOwners: MutableMap<String, Field>,
    ) {
        when (caseName.simpleName) {
            interfaceName.simpleName ->
                problems.error(
                    field.location,
                    "oneof field '${field.name}' has the same Kotlin name as its oneof '${oneOf.name}'"
                )

            GeneratedNames.COMPANION -> problems.error(
                field.location,
                "oneof field '${field.name}' clashes with the generated companion object"
            )
        }
        caseOwners.putIfAbsent(caseName.simpleName, field)?.let { previous ->
            problems.error(
                field.location,
                "oneof field '${field.name}' maps to the Kotlin class ${caseName.simpleName}, which '${previous.name}' " +
                        "(${previous.location}) already uses",
            )
        }
    }

    private companion object {
        const val VALUE = "value"
    }
}

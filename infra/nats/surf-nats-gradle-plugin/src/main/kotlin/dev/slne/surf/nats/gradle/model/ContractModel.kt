package dev.slne.surf.nats.gradle.model

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE_ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.OneOf
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import dev.slne.surf.nats.gradle.validate.Problems

internal enum class IntEncoding { DEFAULT, SIGNED, FIXED }

/**
 * Name of the companion object the serialization plugin generates for `@Serializable` classes and interfaces.
 */
internal const val COMPANION = "Companion"

internal class MappedClass(val kotlinClass: ClassName, val serializer: ClassName)

/**
 * The Kotlin view of a proto element type; [encoding] is `null` for kinds that ignore `@ProtoType`.
 */
internal class ElementType(
    val protoType: ProtoType,
    val kotlinType: ClassName,
    val serializer: ClassName?,
    val encoding: IntEncoding?,
    val packable: Boolean,
    val enumType: EnumType?,
    val isMessage: Boolean,
) {
    val isMapped: Boolean get() = serializer != null

    val isBytes: Boolean get() = protoType == ProtoType.BYTES

    /**
     * The type as written in a serializable position: mapped types carry `@Serializable(with = ...)`.
     */
    val usageType: TypeName
        get() = serializer?.let { kotlinType.copy(annotations = listOf(KotlinTypes.serializableWith(it))) } ?: kotlinType
}

/**
 * One constructor property of a generated message; [parameterType] omits the serialization annotations of [type].
 */
internal class PropertyModel(
    val name: String,
    val location: Location,
    val protoName: String,
    val type: TypeName,
    val parameterType: TypeName,
    val kClass: TypeName,
    val nullable: Boolean,
    val default: CodeBlock?,
    val annotations: List<AnnotationSpec>,
    val contentAware: Boolean,
    val oneof: OneofModel?,
)

internal class OneofModel(val interfaceName: ClassName, val location: Location, val cases: List<OneofCase>)

internal class OneofCase(
    val className: ClassName,
    val location: Location,
    val valueType: TypeName,
    val annotations: List<AnnotationSpec>,
    val contentAware: Boolean,
)

/**
 * Properties of a generated message by ascending field number; a oneof sits at the position of its lowest case number.
 */
internal class MessageShape(val className: ClassName, val properties: List<PropertyModel>) {
    val fieldless: Boolean get() = properties.isEmpty()
    val contentAware: Boolean get() = properties.any { it.contentAware }
    val oneofs: List<OneofModel> get() = properties.mapNotNull { it.oneof }
}

internal class EnumEntry(val kotlinName: String, val number: Int)

internal class EnumModel(val className: ClassName, val protoName: String, val entries: List<EnumEntry>) {
    val zero: EnumEntry? get() = entries.firstOrNull { it.number == 0 }
}

/**
 * Resolves and validates the proto types of one run; cached shapes report the problems of a declaration once.
 */
internal class ContractModel(
    val schema: Schema,
    private val mappings: Map<String, MappedClass>,
    val problems: Problems,
) {
    private val generated = HashMap<String, ClassName>()
    private val shapes = HashMap<String, MessageShape>()
    private val enums = HashMap<String, EnumModel>()
    private val rawDefaults = RawDefaults()

    fun register(protoType: ProtoType, className: ClassName) {
        generated[protoType.toString()] = className
    }

    fun isMapped(protoType: ProtoType): Boolean = protoType.toString() in mappings

    fun mapping(protoType: ProtoType): MappedClass? = mappings[protoType.toString()]

    fun generatedClass(protoType: ProtoType): ClassName? = generated[protoType.toString()]

    /**
     * Resolves a non-map element type; reports and returns `null` for unsupported or unknown types.
     */
    fun element(protoType: ProtoType, location: Location): ElementType? {
        if (protoType == ProtoType.EMPTY) {
            problems.error(location, "${ProtoType.EMPTY} can only be an rpc request or response, not a field type")
            return null
        }
        if (protoType.isScalar) return scalar(protoType, location)

        val enumType = schema.getType(protoType) as? EnumType
        val mapped = mappings[protoType.toString()]
        val kotlinType = mapped?.kotlinClass ?: generated[protoType.toString()]
        if (kotlinType == null) {
            reportUnknown(protoType, location)
            return null
        }
        return ElementType(
            protoType = protoType,
            kotlinType = kotlinType,
            serializer = mapped?.serializer,
            encoding = if (enumType != null) IntEncoding.DEFAULT else null,
            packable = enumType != null,
            // Mapped enums have no generated enum model.
            enumType = if (mapped == null) enumType else null,
            isMessage = enumType == null,
        )
    }

    fun reportUnknown(protoType: ProtoType, location: Location) {
        val file = schema.protoFile(protoType)?.location?.path ?: "an unknown file"
        problems.error(
            location,
            "$protoType from $file is neither generated nor mapped; add mapType(\"$protoType\", ...) or move the .proto into protoSourceDirs",
        )
    }

    fun shape(type: MessageType, className: ClassName): MessageShape {
        return shapes.getOrPut(type.type.toString()) { buildShape(type, className) }
    }

    fun enumModel(type: EnumType, className: ClassName): EnumModel {
        return enums.getOrPut(type.type.toString()) { buildEnum(type, className) }
    }

    private fun scalar(protoType: ProtoType, location: Location): ElementType? {
        val (kotlinType, encoding, packable) = when (protoType.simpleName) {
            "double" -> Triple(DOUBLE, null, true)
            "float" -> Triple(FLOAT, null, true)
            "int32", "uint32" -> Triple(INT, IntEncoding.DEFAULT, true)
            "int64", "uint64" -> Triple(LONG, IntEncoding.DEFAULT, true)
            "sint64" -> Triple(LONG, IntEncoding.SIGNED, false)
            "fixed32", "sfixed32" -> Triple(INT, IntEncoding.FIXED, false)
            "fixed64", "sfixed64" -> Triple(LONG, IntEncoding.FIXED, false)
            "bool" -> Triple(BOOLEAN, IntEncoding.DEFAULT, true)
            "string" -> Triple(STRING, null, false)
            "bytes" -> Triple(BYTE_ARRAY, null, false)
            "sint32" -> {
                problems.error(
                    location,
                    "sint32 is not supported (kotlinx-serialization-protobuf cannot decode values >= 2^30); use sint64 or int32",
                )
                return null
            }

            else -> {
                problems.error(location, "unsupported scalar type $protoType")
                return null
            }
        }
        return ElementType(protoType, kotlinType, null, encoding, packable, null, false)
    }

    private fun buildShape(type: MessageType, className: ClassName): MessageShape {
        val properties = orderedMembers(type, ::field) { oneOf, _ -> oneof(oneOf, className) }.filterNotNull()

        val names = HashMap<String, PropertyModel>()
        for (property in properties) {
            val previous = names.putIfAbsent(property.name, property)
            if (previous != null) {
                problems.error(
                    property.location,
                    "'${property.protoName}' maps to the Kotlin property '${property.name}', which '${previous.protoName}' " +
                            "(${previous.location}) already uses",
                )
            }
        }
        return MessageShape(className, properties)
    }

    private fun field(field: Field): PropertyModel? {
        val location = field.location
        if (field.label == Field.Label.REQUIRED) {
            problems.error(location, "required field '${field.name}' is not supported; declare it optional (non-null fields without [default] are required)")
            return null
        }
        val protoType = field.type ?: return null
        val nullable = SurfOptions.nullable(field.options, location, problems)
        val name = KotlinNaming.lowerCamel(field.name)
        val number = AnnotationSpec.builder(KotlinTypes.PROTO_NUMBER).addMember("%L", field.tag).build()

        if (protoType.isMap) {
            if (nullable) problems.error(location, "(surf.nats.v1.nullable) is not allowed on map field '${field.name}'")
            if (field.default != null) problems.error(location, "[default] is not allowed on map field '${field.name}'")
            val key = element(protoType.keyType!!, location)
            val value = element(protoType.valueType!!, location)
            if (key == null || value == null) return null
            val keyEncoding = key.encoding
            val valueEncoding = value.encoding
            if (keyEncoding != null && valueEncoding != null && keyEncoding != valueEncoding) {
                problems.error(
                    location,
                    "map<${key.protoType}, ${value.protoType}> '${field.name}' cannot be encoded by kotlinx-serialization-protobuf: " +
                            "key and value need the same integer encoding ($keyEncoding vs $valueEncoding)",
                )
                return null
            }
            val encoding = listOfNotNull(keyEncoding, valueEncoding).firstOrNull { it != IntEncoding.DEFAULT }
            return PropertyModel(
                name = name,
                location = location,
                protoName = field.name,
                type = MAP.parameterizedBy(key.usageType, value.usageType),
                parameterType = MAP.parameterizedBy(key.kotlinType, value.kotlinType),
                kClass = MAP,
                nullable = false,
                default = CodeBlock.of("emptyMap()"),
                annotations = listOfNotNull(number, encoding?.let(::protoType)),
                contentAware = value.isBytes,
                oneof = null,
            )
        }

        val element = element(protoType, location) ?: return null
        if (field.isRepeated) {
            if (nullable) problems.error(location, "(surf.nats.v1.nullable) is not allowed on repeated field '${field.name}'")
            if (field.default != null) problems.error(location, "[default] is not allowed on repeated field '${field.name}'")
            var packed: AnnotationSpec? = null
            if (field.isPacked) {
                if (!element.packable) {
                    problems.error(
                        location,
                        "[packed = true] is not supported for repeated ${element.protoType} '${field.name}' " +
                                "(kotlinx-serialization-protobuf ignores the integer encoding of packed lists)",
                    )
                } else {
                    packed = AnnotationSpec.builder(KotlinTypes.PROTO_PACKED).build()
                }
            }
            return PropertyModel(
                name = name,
                location = location,
                protoName = field.name,
                type = LIST.parameterizedBy(element.usageType),
                parameterType = LIST.parameterizedBy(element.kotlinType),
                kClass = LIST,
                nullable = false,
                default = CodeBlock.of("emptyList()"),
                annotations = listOfNotNull(number, encodingAnnotation(element), packed),
                contentAware = element.isBytes,
                oneof = null,
            )
        }

        val default = when {
            field.default == null -> null
            nullable -> {
                problems.error(location, "'${field.name}' cannot be (surf.nats.v1.nullable) and have a [default]")
                null
            }

            else -> defaultValue(field, element)
        }
        return PropertyModel(
            name = name,
            location = location,
            protoName = field.name,
            type = if (nullable) element.usageType.copy(nullable = true) else element.usageType,
            parameterType = element.kotlinType.copy(nullable = nullable),
            kClass = element.kotlinType,
            nullable = nullable,
            default = if (nullable) CodeBlock.of("null") else default,
            annotations = listOfNotNull(number, encodingAnnotation(element)),
            contentAware = element.isBytes,
            oneof = null,
        )
    }

    private fun defaultValue(field: Field, element: ElementType): CodeBlock? {
        val literal = field.default ?: return null
        val enumType = element.enumType
        return when {
            element.isMapped || element.isMessage -> {
                problems.error(field.location, "[default] is not supported for ${element.protoType} field '${field.name}'")
                null
            }

            enumType != null -> {
                val zero = enumType.constants.firstOrNull { it.tag == 0 }
                if (zero == null || zero.name != literal) {
                    problems.error(
                        field.location,
                        "enum field '${field.name}' may only default to its zero value${zero?.let { " ${it.name}" }.orEmpty()}, was $literal",
                    )
                    return null
                }
                val model = enumModel(enumType, element.kotlinType)
                val entry = model.zero ?: return null
                CodeBlock.of("%T.%N", element.kotlinType, entry.kotlinName)
            }

            element.protoType == ProtoType.STRING || element.protoType == ProtoType.BYTES -> {
                val bytes = rawDefaults.bytes(field.location, literal)
                when {
                    bytes == null -> {
                        problems.error(
                            field.location,
                            "cannot read the [default] literal of '${field.name}' from its source file " +
                                    "(no literal at this location decodes to the default wire-schema parsed)",
                        )
                        null
                    }

                    element.protoType == ProtoType.BYTES -> DefaultValues.bytes(bytes)
                    else -> DefaultValues.string(bytes.decodeToString())
                }
            }

            else -> DefaultValues.scalar(element.protoType.simpleName, literal) ?: run {
                problems.error(field.location, "invalid [default = $literal] for ${element.protoType} field '${field.name}'")
                null
            }
        }
    }

    private fun oneof(oneOf: OneOf, owner: ClassName): PropertyModel {
        val interfaceName = owner.nestedClass(KotlinNaming.upperCamel(oneOf.name))
        when (interfaceName.simpleName) {
            COMPANION -> problems.error(oneOf.location, "oneof '${oneOf.name}' clashes with the generated companion object")
            owner.simpleName -> problems.error(oneOf.location, "oneof '${oneOf.name}' must not have the name of its enclosing message")
        }
        val caseNames = HashMap<String, Field>()
        val cases = oneOf.fields.sortedBy { it.tag }.mapNotNull { field ->
            val location = field.location
            if (SurfOptions.nullable(field.options, location, problems)) {
                problems.error(location, "(surf.nats.v1.nullable) is not allowed on oneof field '${field.name}'; the oneof itself is nullable")
            }
            if (field.default != null) problems.error(location, "[default] is not allowed on oneof field '${field.name}'")
            val element = field.type?.let { element(it, location) } ?: return@mapNotNull null
            val caseName = interfaceName.nestedClass(KotlinNaming.upperCamel(field.name))
            when (caseName.simpleName) {
                interfaceName.simpleName ->
                    problems.error(location, "oneof field '${field.name}' has the same Kotlin name as its oneof '${oneOf.name}'")

                COMPANION -> problems.error(location, "oneof field '${field.name}' clashes with the generated companion object")
            }
            caseNames.putIfAbsent(caseName.simpleName, field)?.let { previous ->
                problems.error(
                    location,
                    "oneof field '${field.name}' maps to the Kotlin class ${caseName.simpleName}, which '${previous.name}' " +
                            "(${previous.location}) already uses",
                )
            }
            OneofCase(
                className = caseName,
                location = location,
                valueType = element.usageType,
                annotations = listOfNotNull(
                    AnnotationSpec.builder(KotlinTypes.PROTO_NUMBER).addMember("%L", field.tag).build(),
                    encodingAnnotation(element),
                ),
                contentAware = element.isBytes,
            )
        }
        val oneofModel = OneofModel(interfaceName, oneOf.location, cases)
        val nullableInterface = interfaceName.copy(nullable = true)
        return PropertyModel(
            name = KotlinNaming.lowerCamel(oneOf.name),
            location = oneOf.location,
            protoName = oneOf.name,
            type = nullableInterface,
            parameterType = nullableInterface,
            kClass = interfaceName,
            nullable = true,
            default = CodeBlock.of("null"),
            annotations = listOf(AnnotationSpec.builder(KotlinTypes.PROTO_ONE_OF).build()),
            contentAware = false,
            oneof = oneofModel,
        )
    }

    private fun encodingAnnotation(element: ElementType): AnnotationSpec? {
        val encoding = element.encoding ?: return null
        return if (encoding == IntEncoding.DEFAULT) null else protoType(encoding)
    }

    private fun protoType(encoding: IntEncoding): AnnotationSpec {
        return AnnotationSpec.builder(KotlinTypes.PROTO_TYPE)
            .addMember("%T.%L", KotlinTypes.PROTO_INTEGER_TYPE, encoding.name)
            .build()
    }

    private fun buildEnum(type: EnumType, className: ClassName): EnumModel {
        val prefix = KotlinNaming.enumValuePrefix(type.name)
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
                "enum ${type.name} must declare ${zeroNames.joinToString(" or ")} = 0 as its first value",
            )
        }

        val entries = mutableListOf<EnumEntry>()
        val byKotlinName = HashMap<String, String>()
        for (constant in type.constants) {
            if (constant.tag < 0) {
                problems.error(constant.location, "enum value ${constant.name} has a negative number ${constant.tag}")
            }
            if (!constant.name.startsWith(prefix)) {
                problems.error(constant.location, "enum value ${constant.name} must start with the prefix $prefix")
                continue
            }
            val kotlinName = constant.name.removePrefix(prefix)
            if (!KotlinNaming.isIdentifier(kotlinName)) {
                problems.error(
                    constant.location,
                    "enum value ${constant.name} without its prefix ($kotlinName) must start with a letter and contain only letters, digits and '_'",
                )
                continue
            }
            if (kotlinName in RESERVED_ENUM_ENTRY_NAMES) {
                problems.error(constant.location, "enum value ${constant.name} maps to the reserved Kotlin entry name $kotlinName")
                continue
            }
            val previous = byKotlinName.putIfAbsent(kotlinName, constant.name)
            if (previous != null) {
                problems.error(constant.location, "enum values $previous and ${constant.name} both map to the Kotlin entry $kotlinName")
                continue
            }
            entries += EnumEntry(kotlinName, constant.tag)
        }
        return EnumModel(className, type.type.toString(), entries)
    }

    private companion object {
        /**
         * Members of Kotlin enums and of the generated enum class that an entry name must not shadow.
         */
        val RESERVED_ENUM_ENTRY_NAMES = setOf("name", "ordinal", "entries", "values", "valueOf", "number", "Serializer", COMPANION)
    }
}

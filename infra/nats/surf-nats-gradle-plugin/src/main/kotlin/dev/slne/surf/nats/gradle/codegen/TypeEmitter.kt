package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.model.*
import dev.slne.surf.nats.protocol.ProtocolDescriptors
import dev.slne.surf.nats.protocol.ProtocolNaming

private const val NATS_DESCRIPTOR_PROPERTY = "natsDescriptor"

/**
 * Name of the companion object the serialization plugin generates for `@Serializable` classes and interfaces.
 */
private const val COMPANION = "Companion"

/**
 * Members of Kotlin enums and of the generated enum class that an entry name must not shadow.
 */
private val RESERVED_ENUM_ENTRY_NAMES = setOf(
    "name",
    "ordinal",
    "entries",
    "values",
    "valueOf",
    "number",
    "Serializer",
    COMPANION,
)

/**
 * Generates the Kotlin types of Protobuf messages and enums and reports what cannot be generated.
 *
 * Every rule that exists because of how a declaration is represented in Kotlin is checked here, next to the code that
 * emits it: supported field types and encodings, defaults, nullability, and Kotlin name clashes within a message or
 * enum. Each declaration is emitted, and therefore checked, exactly once.
 */
internal class TypeEmitter(
    private val types: KotlinTypeResolver,
    private val visibility: GeneratedVisibility,
) {
    private val problems get() = types.problems
    private val rawDefaults = RawDefaults()

    /**
     * Returns the generated type, or `null` for types mapped by `mapType`.
     */
    fun emit(type: Type): TypeSpec? = when (type) {
        is MessageType -> message(type)
        is EnumType -> enum(type)
        else -> null
    }

    /**
     * A constructor property of a generated data class.
     *
     * [comparison] is how the generated `equals` compares it when the class needs content-aware members.
     */
    private class Property(
        val name: String,
        val type: TypeName,
        val default: CodeBlock?,
        val annotations: List<AnnotationSpec>,
        val comparison: Comparison,
    )

    /** Equality strategy for properties when custom data-class members are required. */
    private enum class Comparison {
        CONTENT,
        EQUALS,
        BITS,
        NULLABLE_BITS
    }

    /**
     * Generates a serializable Kotlin message, including its oneofs, nested types, and event metadata.
     *
     * Mapped messages are skipped. Mapped event messages are additionally reported as invalid.
     * Field and oneof validation runs before properties and nested declarations are emitted.
     *
     * @param type the Protobuf message to generate
     * @return the generated message, or `null` if it is mapped
     */
    private fun message(type: MessageType): TypeSpec? {
        val isEvent = SurfProtoOptions.isEvent(type.options)
        if (types.isMapped(type.type)) {
            if (isEvent) {
                problems.error(
                    type.location,
                    "event message ${type.type} is mapped by mapType; events must be generated"
                )
            }

            return null
        }

        val className = checkNotNull(types.generatedClass(type.type))

        val oneofTypes = mutableListOf<TypeSpec>()
        val nestedNames = HashMap<String, Location>()
        val members = collectMembers(type, className, oneofTypes, nestedNames)
        val propertyOwners = validatePropertyNames(members)

        val classBuilder = messageBuilder(type, className, members.map { it.first })
        if (isEvent) {
            propertyOwners[NATS_DESCRIPTOR_PROPERTY]?.let { (_, location) ->
                problems.error(
                    location,
                    "event ${type.type} must not have a field named $NATS_DESCRIPTOR_PROPERTY"
                )
            }

            addEvent(classBuilder, type, className)
        }

        classBuilder.addTypes(oneofTypes)
        addNestedTypes(classBuilder, type, className, nestedNames)
        return classBuilder.build()
    }

    /**
     * Collects constructor properties in wire declaration order and generates oneof interfaces.
     *
     * The mutable collections receive generated oneofs and their names so nested message names
     * can subsequently be checked against them. Invalid fields are omitted from the result.
     *
     * @param type the message defining the fields and oneofs
     * @param className the enclosing generated class
     * @param oneofTypes destination for generated oneof interfaces
     * @param nestedNames destination for the oneof names and their locations
     * @return generated properties together with their original names and locations
     */
    private fun collectMembers(
        type: MessageType,
        className: ClassName,
        oneofTypes: MutableList<TypeSpec>,
        nestedNames: MutableMap<String, Location>,
    ): List<Triple<Property, String, Location>> = orderedMembers(
        type,
        field = { field -> fieldProperty(field)?.let { Triple(it, field.name, field.location) } },
        oneof = { oneOf, _ ->
            val interfaceName = className.nestedClass(KotlinNaming.upperCamel(oneOf.name))
            oneofTypes += oneofType(oneOf, interfaceName, className)
            nestedNames[interfaceName.simpleName] = oneOf.location
            Triple(oneofProperty(oneOf, interfaceName), oneOf.name, oneOf.location)
        },
    ).filterNotNull()

    /**
     * Reports collisions between generated Kotlin property names in declaration order.
     *
     * @param members generated properties and their original declarations
     * @return the first declaration owning each generated property name
     */
    private fun validatePropertyNames(
        members: List<Triple<Property, String, Location>>,
    ): Map<String, Pair<String, Location>> {
        val propertyOwners = HashMap<String, Pair<String, Location>>()
        for ((property, protoName, location) in members) {
            val previous =
                propertyOwners.putIfAbsent(property.name, protoName to location) ?: continue
            problems.error(
                location,
                "'$protoName' maps to the Kotlin property '${property.name}', which '${previous.first}' (${previous.second}) already uses",
            )
        }
        return propertyOwners
    }

    /**
     * Configures the serializable message class and its constructor or fieldless value members.
     *
     * @param type the original message, used for its serialized name
     * @param className the generated Kotlin class name
     * @param properties the properties produced from fields and oneofs
     * @return the unfinished class builder
     */
    private fun messageBuilder(
        type: MessageType,
        className: ClassName,
        properties: List<Property>,
    ): TypeSpec.Builder {
        val serialNameAnnotation = AnnotationSpec.builder(KotlinTypes.SERIAL_NAME)
            .addMember("%S", type.type.toString())
            .build()

        return TypeSpec.classBuilder(className)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .addAnnotation(serialNameAnnotation)
            .applyVisibility(visibility)
            .apply {
                if (properties.isEmpty()) {
                    addFunctions(fieldlessMembers(className))
                } else {
                    addDataProperties(className, properties)
                }
            }
    }

    /**
     * Adds nested messages and enums, checking conflicts with names reserved by oneofs.
     *
     * Name validation also runs for nested declarations that are mapped and not emitted.
     *
     * @param builder the enclosing message builder
     * @param type the message containing the nested declarations
     * @param className the enclosing Kotlin class
     * @param nestedNames names previously claimed by oneof interfaces
     */
    private fun addNestedTypes(
        builder: TypeSpec.Builder,
        type: MessageType,
        className: ClassName,
        nestedNames: MutableMap<String, Location>,
    ) {
        for (nested in type.nestedTypes) {
            checkNestedName(className, nested, nestedNames)
            val nestedSpec = emit(nested) ?: continue
            builder.addType(nestedSpec)
        }
    }

    /**
     * Resolves a field to a Kotlin constructor property, choosing its collection or scalar representation.
     *
     * Required Protobuf fields are unsupported and are reported without generating a property.
     *
     * @param field the Protobuf field to generate
     * @return the generated property, or `null` when it cannot be generated
     */
    private fun fieldProperty(field: Field): Property? {
        if (field.label == Field.Label.REQUIRED) {
            problems.error(
                field.location,
                "required field '${field.name}' is not supported; declare it optional (non-null fields without [default] are required)",
            )
            return null
        }
        val protoType = field.type ?: return null
        val nullable = SurfProtoOptions.isNullable(field.options)
        return when {
            protoType.isMap -> mapProperty(field, protoType, nullable)
            field.isRepeated -> listProperty(field, protoType, nullable)
            else -> singleProperty(field, protoType, nullable)
        }
    }

    /**
     * Generates a map property after resolving both element types and checking Protobuf encoding compatibility.
     *
     * Maps cannot declare custom nullability or defaults. Incompatible integer encodings prevent
     * generation because kotlinx-serialization-protobuf applies one encoding to both elements.
     *
     * @param field the original map field
     * @param protoType the Protobuf map type
     * @param nullable whether custom nullability was requested
     * @return the generated map property, or `null` if its types or encoding are unsupported
     */
    private fun mapProperty(field: Field, protoType: ProtoType, nullable: Boolean): Property? {
        val location = field.location
        if (nullable) problems.error(
            location,
            "(surf.nats.v1.nullable) is not allowed on map field '${field.name}'"
        )
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
        return Property(
            name = KotlinNaming.lowerCamel(field.name),
            type = MAP.parameterizedBy(key.usageType, value.usageType),
            default = CodeBlock.of("emptyMap()"),
            annotations = listOfNotNull(protoNumber(field), encodingAnnotation(encoding)),
            comparison = if (value.isBytes) Comparison.CONTENT else Comparison.EQUALS,
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
     * Generates a repeated Protobuf field as a Kotlin list.
     *
     * Custom nullability and defaults are reported as unsupported. Packed encoding is
     * added only for packable element types; other packed declarations report an error.
     *
     * @return the generated list property, or `null` if the element type cannot be resolved
     */
    private fun listProperty(field: Field, protoType: ProtoType, nullable: Boolean): Property? {
        val location = field.location
        val element = types.element(protoType, location) ?: return null
        if (nullable) problems.error(
            location,
            "(surf.nats.v1.nullable) is not allowed on repeated field '${field.name}'"
        )
        if (field.default != null) problems.error(
            location,
            "[default] is not allowed on repeated field '${field.name}'"
        )

        val packed = packedAnnotation(field, element)
        return Property(
            name = KotlinNaming.lowerCamel(field.name),
            type = LIST.parameterizedBy(element.usageType),
            default = CodeBlock.of("emptyList()"),
            annotations = listOfNotNull(
                protoNumber(field),
                encodingAnnotation(element.encoding),
                packed
            ),
            comparison = if (element.isBytes) Comparison.CONTENT else Comparison.EQUALS,
        )
    }

    /**
     * Resolves an optional `@ProtoPacked` annotation for a repeated field.
     *
     * @return the annotation for supported packed fields, or `null` otherwise
     */
    private fun packedAnnotation(field: Field, element: ElementType): AnnotationSpec? {
        if (!field.isPacked) return null
        if (element.packable) return AnnotationSpec.builder(KotlinTypes.PROTO_PACKED).build()

        problems.error(
            field.location,
            "[packed = true] is not supported for repeated ${element.protoType} '${field.name}' " +
                    "(kotlinx-serialization-protobuf ignores the integer encoding of packed lists)",
        )
        return null
    }

    /**
     * Generates a scalar or message-valued property, including its default and equality behavior.
     *
     * Nullable properties use `null` as their default. Non-null properties use their
     * explicitly declared defaults, if any. Floating-point properties use bitwise
     * equality when content-aware equality methods are generated.
     *
     * @return the property, or `null` if its element type cannot be resolved
     */
    private fun singleProperty(field: Field, protoType: ProtoType, nullable: Boolean): Property? {
        val element = types.element(protoType, field.location) ?: return null
        val default = if (nullable) {
            if (field.default != null) {
                problems.error(
                    field.location,
                    "'${field.name}' cannot be (surf.nats.v1.nullable) and have a [default]"
                )
            }
            CodeBlock.of("null")
        } else {
            defaultValue(field, element)
        }
        return Property(
            name = KotlinNaming.lowerCamel(field.name),
            type = if (nullable) element.usageType.copy(nullable = true) else element.usageType,
            default = default,
            annotations = listOfNotNull(protoNumber(field), encodingAnnotation(element.encoding)),
            comparison = singleComparison(element, nullable),
        )
    }

    /**
     * Chooses the equality strategy for a non-collection property.
     *
     * Byte arrays use content equality, floating-point types compare their bit patterns,
     * and all other types use regular Kotlin equality.
     */
    private fun singleComparison(element: ElementType, nullable: Boolean): Comparison = when {
        element.isBytes -> Comparison.CONTENT
        element.kotlinType != FLOAT && element.kotlinType != DOUBLE -> Comparison.EQUALS
        nullable -> Comparison.NULLABLE_BITS
        else -> Comparison.BITS
    }

    /**
     * Resolves a Protobuf `[default]` literal to the corresponding Kotlin expression.
     *
     * Generated messages and mapped types do not support defaults. Enum, string, byte,
     * and scalar defaults are delegated to their specific parsers and validators.
     *
     * @param field the declaration containing the default literal
     * @param element the resolved type information
     * @return the default expression, or `null` if no usable default exists
     */
    private fun defaultValue(field: Field, element: ElementType): CodeBlock? {
        val literal = field.default ?: return null
        val enumType = element.enumType
        return when {
            element.isMapped || element.isMessage -> {
                problems.error(
                    field.location,
                    "[default] is not supported for ${element.protoType} field '${field.name}'"
                )
                null
            }

            enumType != null -> enumDefault(field, element, enumType, literal)
            element.protoType == ProtoType.STRING || element.protoType == ProtoType.BYTES ->
                rawDefault(field, element, literal)

            else -> scalarDefault(field, element, literal)
        }
    }

    /**
     * Resolves a default enum constant, accepting only the enum's zero-valued member.
     *
     * The generated enum's prefix is removed from the Kotlin entry reference.
     * Enum entry naming is validated separately when the enum itself is generated.
     */
    private fun enumDefault(
        field: Field,
        element: ElementType,
        enumType: EnumType,
        literal: String
    ): CodeBlock? {
        val zero = enumType.constants.firstOrNull { it.tag == 0 }
        if (zero == null || zero.name != literal) {
            problems.error(
                field.location,
                "enum field '${field.name}' may only default to its zero value${
                    zero?.let { " ${it.name}" }.orEmpty()
                }, was $literal",
            )
            return null
        }
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
     * Missing or undecodable literals are reported and not emitted.
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
     * Builds the Protobuf field-number annotation used by kotlinx serialization.
     *
     * @param field the field supplying the number
     * @return a `@ProtoNumber` annotation
     */
    private fun protoNumber(field: Field): AnnotationSpec {
        return AnnotationSpec.builder(KotlinTypes.PROTO_NUMBER).addMember("%L", field.tag).build()
    }

    /**
     * Builds the `@ProtoType` annotation for a non-default Protobuf integer encoding.
     *
     * @return the annotation, or `null` for default or unspecified encoding
     */
    private fun encodingAnnotation(encoding: IntEncoding?): AnnotationSpec? {
        if (encoding == null || encoding == IntEncoding.DEFAULT) return null
        return AnnotationSpec.builder(KotlinTypes.PROTO_TYPE)
            .addMember("%T.%L", KotlinTypes.PROTO_INTEGER_TYPE, encoding.name)
            .build()
    }

    /**
     * Generates the nullable property holding the active case of a oneof.
     *
     * A null value represents an unset oneof and uses `@ProtoOneOf` for serialization.
     *
     * @param oneOf the Protobuf oneof declaration
     * @param interfaceName the generated sealed interface representing its cases
     * @return the generated property
     */
    private fun oneofProperty(oneOf: OneOf, interfaceName: ClassName): Property = Property(
        name = KotlinNaming.lowerCamel(oneOf.name),
        type = interfaceName.copy(nullable = true),
        default = CodeBlock.of("null"),
        annotations = listOf(AnnotationSpec.builder(KotlinTypes.PROTO_ONE_OF).build()),
        comparison = Comparison.EQUALS,
    )

    /**
     * Generates a sealed interface for a Protobuf oneof, with one data class for each valid case.
     *
     * The interface represents the optional union property in the enclosing message.
     * Case classes inherit the visibility of their enclosing sealed interface.
     *
     * @param oneOf the original oneof declaration
     * @param interfaceName the generated sealed interface name
     * @param owner the enclosing message class
     * @return the oneof interface and its case classes
     */
    private fun oneofType(oneOf: OneOf, interfaceName: ClassName, owner: ClassName): TypeSpec {
        validateOneofName(oneOf, interfaceName, owner)
        val interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
            .addModifiers(KModifier.SEALED)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .applyVisibility(visibility)

        val caseOwners = HashMap<String, Field>()
        for (field in oneOf.fields.sortedBy { it.tag }) {
            val case = oneofCase(oneOf, interfaceName, field, caseOwners) ?: continue
            interfaceBuilder.addType(case)
        }
        return interfaceBuilder.build()
    }

    /**
     * Reports collisions between a oneof interface and the enclosing message or companion object.
     */
    private fun validateOneofName(oneOf: OneOf, interfaceName: ClassName, owner: ClassName) {
        when (interfaceName.simpleName) {
            COMPANION -> problems.error(
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
     * Generates a oneof case after checking field options, resolving its type, and validating its class name.
     *
     * Invalid names and options are reported but do not prevent generation of a resolvable case;
     * an unresolved field type prevents the case from being emitted.
     *
     * @param oneOf the containing oneof declaration
     * @param interfaceName the generated union interface
     * @param field the case field
     * @param caseOwners the first field claiming each generated case name
     * @return the generated case class, or `null` if the field type cannot be resolved
     */
    private fun oneofCase(
        oneOf: OneOf,
        interfaceName: ClassName,
        field: Field,
        caseOwners: MutableMap<String, Field>,
    ): TypeSpec? {
        val location = field.location
        if (SurfProtoOptions.isNullable(field.options)) {
            problems.error(
                location,
                "(surf.nats.v1.nullable) is not allowed on oneof field '${field.name}'; the oneof itself is nullable"
            )
        }
        if (field.default != null) problems.error(
            location,
            "[default] is not allowed on oneof field '${field.name}'"
        )
        val element = field.type?.let { types.element(it, location) } ?: return null

        val caseName = interfaceName.nestedClass(KotlinNaming.upperCamel(field.name))
        validateOneofCaseName(oneOf, interfaceName, field, caseName, caseOwners)

        val value = Property(
            name = VALUE,
            type = element.usageType,
            default = null,
            annotations = listOfNotNull(protoNumber(field), encodingAnnotation(element.encoding)),
            comparison = if (element.isBytes) Comparison.CONTENT else Comparison.EQUALS,
        )
        // The case classes inherit the visibility of the oneof interface.
        return TypeSpec.classBuilder(caseName)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .addSuperinterface(interfaceName)
            .addDataProperties(caseName, listOf(value))
            .build()
    }

    /**
     * Reports reserved or duplicate oneof case class names while retaining the first owner.
     *
     * Subsequent fields still generate their case even if their name collides.
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

            COMPANION -> problems.error(
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

    /**
     * Reports nested type names that collide with the enclosing class, companion, or oneof types.
     *
     * Non-reserved nested names are recorded even when they collide with oneofs,
     * and the previous declaration location is reported for a collision.
     *
     * @param owner the enclosing message class
     * @param nested the nested Protobuf declaration
     * @param used locations of oneofs and previously visited nested type names
     */
    private fun checkNestedName(
        owner: ClassName,
        nested: Type,
        used: MutableMap<String, Location>
    ) {
        when (val name = nested.type.simpleName) {
            owner.simpleName ->
                problems.error(
                    nested.location,
                    "nested type $name must not have the name of its enclosing type"
                )

            COMPANION ->
                problems.error(
                    nested.location,
                    "nested type name Companion clashes with the generated companion object"
                )

            else -> used.put(name, nested.location)?.let { previousLocation ->
                problems.error(
                    nested.location,
                    "nested type $name clashes with the oneof declared at $previousLocation"
                )
            }
        }
    }

    /**
     * Implements the NATS event contract on a message and adds its descriptor companion.
     *
     * Reports invalid event names before adding the event interface, overridden descriptor
     * property, and static descriptor metadata to the generated class.
     *
     * @param builder the message class being generated
     * @param type the event's Protobuf declaration
     * @param className the generated event class
     */
    private fun addEvent(builder: TypeSpec.Builder, type: MessageType, className: ClassName) {
        val protoName = type.type.toString()
        ProtocolNaming.nameProblem("event", protoName, type.name)
            ?.let { problems.error(type.location, it) }

        builder.addSuperinterface(KotlinTypes.NATS_EVENT)
        builder.addProperty(eventDescriptorProperty())
        builder.addType(eventDescriptorCompanion(className, protoName))
    }

    /**
     * Generates the overridden event descriptor getter, which delegates to the companion descriptor.
     */
    private fun eventDescriptorProperty(): PropertySpec = PropertySpec.builder(
        NATS_DESCRIPTOR_PROPERTY,
        KotlinTypes.NATS_EVENT_DESCRIPTOR.parameterizedBy(STAR),
    )
        .addModifiers(KModifier.OVERRIDE)
        .getter(
            FunSpec.getterBuilder().addStatement("return %N", ProtocolDescriptors.FIELD).build()
        )
        .build()

    /**
     * Generates an event companion with the Protobuf name and the generated serializer.
     *
     * The descriptor property uses `@JvmField` to expose the static metadata to Java callers.
     */
    private fun eventDescriptorCompanion(className: ClassName, protoName: String): TypeSpec {
        val descriptorType = KotlinTypes.NATS_EVENT_DESCRIPTOR.parameterizedBy(className)
        val initializer = buildCodeBlock {
            add("%T(\n", descriptorType)
            withIndent {
                add("protoName = %S,\n", protoName)
                add("serializer = serializer(),\n")
            }
            add(")")
        }
        val property = PropertySpec.builder(ProtocolDescriptors.FIELD, descriptorType)
            .jvmField()
            .initializer(initializer)
            .build()
        return TypeSpec.companionObjectBuilder().addProperty(property).build()
    }

    /**
     * Generates a Kotlin enum whose entries carry Protobuf numeric values.
     *
     * Validates enum naming and numbering, collects valid entries in declaration order,
     * and generates a serializer that encodes and decodes the numeric values.
     *
     * @return the generated enum, or `null` for a mapped enum
     */
    private fun enum(type: EnumType): TypeSpec? {
        if (types.isMapped(type.type)) return null
        val className = checkNotNull(types.generatedClass(type.type))
        val prefix = KotlinNaming.enumValuePrefix(type.name)

        validateEnum(type, prefix)
        val entries = enumEntries(type, prefix)
        return enumType(type, className, entries)
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
     * @return the Kotlin name, or `null` if the constant name cannot be emitted
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
        if (kotlinName in RESERVED_ENUM_ENTRY_NAMES) {
            problems.error(
                constant.location,
                "enum value ${constant.name} maps to the reserved Kotlin entry name $kotlinName"
            )
            return null
        }
        return kotlinName
    }

    /**
     * Builds a numbered Kotlin enum and attaches its custom serializer.
     *
     * Each entry's constructor receives the Protobuf numeric value, preserving declaration order.
     */
    private fun enumType(
        type: EnumType,
        className: ClassName,
        entries: Map<String, EnumConstant>
    ): TypeSpec {
        val serializerName = className.nestedClass(SERIALIZER)
        val enumBuilder = TypeSpec.enumBuilder(className)
            .addAnnotation(KotlinTypes.serializableWith(serializerName))
            .primaryConstructor(FunSpec.constructorBuilder().addParameter(NUMBER, INT).build())
            .addProperty(PropertySpec.builder(NUMBER, INT).initializer("%N", NUMBER).build())
            .applyVisibility(visibility)

        for ((kotlinName, constant) in entries) {
            val enumConstant = TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%L", constant.tag)
                .build()
            enumBuilder.addEnumConstant(kotlinName, enumConstant)
        }
        enumBuilder.addType(
            enumSerializer(
                className,
                type.type.toString(),
                serializerName,
                entries
            )
        )
        return enumBuilder.build()
    }

    /**
     * Generates an enum serializer that writes Protobuf numeric values and resolves them on decoding.
     *
     * Unknown values fall back to the zero-valued entry or, if none exists, the first entry.
     * An enum without entries generates a decoding error instead of a fallback value.
     *
     * @param enumClass the generated enum class
     * @param protoName its serialized Protobuf name
     * @param serializerName the nested serializer object name
     * @param entries the accepted enum constants
     * @return the generated serializer object
     */
    private fun enumSerializer(
        enumClass: ClassName,
        protoName: String,
        serializerName: ClassName,
        entries: Map<String, EnumConstant>,
    ): TypeSpec {
        // TODO: generate an Int2Object mapping for lookup
        val fallback =
            entries.entries.firstOrNull { it.value.tag == 0 } ?: entries.entries.firstOrNull()
        return TypeSpec.objectBuilder(serializerName)
            .addSuperinterface(KotlinTypes.K_SERIALIZER.parameterizedBy(enumClass))
            .addProperty(enumSerializationDescriptor(protoName))
            .addFunction(enumSerializeFunction(enumClass))
            .addFunction(enumDeserializeFunction(enumClass, protoName, entries, fallback))
            .build()
    }

    /**
     * Describes an enum serializer as a primitive `Int` serializer under the Protobuf type name.
     */
    private fun enumSerializationDescriptor(protoName: String): PropertySpec =
        PropertySpec.builder("descriptor", KotlinTypes.SERIAL_DESCRIPTOR)
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                "%T(%S, %T.INT)",
                KotlinTypes.PRIMITIVE_SERIAL_DESCRIPTOR,
                protoName,
                KotlinTypes.PRIMITIVE_KIND
            )
            .build()

    /**
     * Generates the enum serializer's `serialize` implementation using its `number` property.
     */
    private fun enumSerializeFunction(enumClass: ClassName): FunSpec = FunSpec.builder("serialize")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("encoder", KotlinTypes.ENCODER)
        .addParameter("value", enumClass)
        .addStatement("encoder.encodeInt(value.%N)", NUMBER)
        .build()

    /**
     * Generates a numeric dispatch table for deserializing enum entries.
     *
     * Uses the selected fallback for unknown numbers, or an error when no entries exist.
     */
    private fun enumDeserializeFunction(
        enumClass: ClassName,
        protoName: String,
        entries: Map<String, EnumConstant>,
        fallback: Map.Entry<String, EnumConstant>?,
    ): FunSpec {
        val function = FunSpec.builder("deserialize")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("decoder", KotlinTypes.DECODER)
            .returns(enumClass)
            .beginControlFlow("return when (decoder.decodeInt())")
        for ((kotlinName, constant) in entries) {
            function.addCode("%L -> %T.%N\n", constant.tag, enumClass, kotlinName)
        }
        if (fallback != null) {
            function.addCode("else -> %T.%N\n", enumClass, fallback.key)
        } else {
            function.addCode("else -> error(%S)\n", "$protoName has no values")
        }
        return function.endControlFlow().build()
    }

    /**
     * Adds constructor properties to a data class and generates content-aware members when needed.
     *
     * Property order is preserved. `equals`, `hashCode`, and `toString` are generated
     * explicitly only if a property requires content-aware comparison.
     *
     * @param className the generated data class name
     * @param properties the constructor properties in declaration order
     * @return this builder
     */
    private fun TypeSpec.Builder.addDataProperties(
        className: ClassName,
        properties: List<Property>
    ): TypeSpec.Builder {
        addModifiers(KModifier.DATA)
        val constructor = FunSpec.constructorBuilder()
        for (property in properties) {
            constructor.addParameter(
                ParameterSpec.builder(property.name, property.type).defaultValue(property.default)
                    .build()
            )
            addProperty(
                PropertySpec.builder(property.name, property.type)
                    .initializer("%N", property.name)
                    .addAnnotations(property.annotations)
                    .build()
            )
        }
        primaryConstructor(constructor.build())
        if (properties.any { it.comparison == Comparison.CONTENT }) {
            addFunctions(contentMembers(className, properties))
        }
        return this
    }

    /**
     * Generates value semantics for a message without fields.
     *
     * All instances of the same generated class compare equal and share a stable
     * hash based on the class name. The string representation is `ClassName()`.
     *
     * @param className the fieldless message class
     * @return its equality, hashing, and string methods
     */
    private fun fieldlessMembers(className: ClassName): List<FunSpec> {
        val equalsBody = buildCodeBlock { addStatement("return other is %T", className) }
        val hashCodeBody =
            CodeBlock.of("return %L\n", DefaultValues.int(className.canonicalName.hashCode()))
        val toStringBody = buildCodeBlock { addStatement("return %S", "${className.simpleName}()") }
        return listOf(
            equalsFunction(equalsBody),
            hashCodeFunction(hashCodeBody),
            toStringFunction(toStringBody)
        )
    }

    /**
     * Generates content-aware `equals`, `hashCode`, and `toString` implementations.
     *
     * Byte arrays compare and hash by content, and floating-point values compare
     * by `toBits()` to preserve data-class semantics for NaN and signed zero.
     *
     * @param className the generated data class
     * @param properties the constructor properties and their comparison strategies
     * @return generated equality, hashing, and string functions in that order
     */
    private fun contentMembers(className: ClassName, properties: List<Property>): List<FunSpec> {
        val equalsBody = contentEqualsBody(className, properties)
        val hashCodeBody = contentHashCodeBody(properties)
        val toStringBody = CodeBlock.of("return %L\n", toStringExpression(className, properties))
        return listOf(
            equalsFunction(equalsBody),
            hashCodeFunction(hashCodeBody),
            toStringFunction(toStringBody)
        )
    }

    /**
     * Generates equality checks for all properties using their configured comparison strategies.
     *
     * Explicit `this.` references avoid name collisions with locally declared variables.
     */
    private fun contentEqualsBody(className: ClassName, properties: List<Property>): CodeBlock {
        val comparisons = properties.joinToCode(" && ") { property ->
            val name = property.name
            when (property.comparison) {
                Comparison.CONTENT -> CodeBlock.of(
                    "%M(this.%N, other.%N)",
                    KotlinTypes.CONTENT_EQUALS,
                    name,
                    name
                )

                Comparison.EQUALS -> CodeBlock.of("this.%N == other.%N", name, name)
                Comparison.BITS -> CodeBlock.of("this.%N.toBits() == other.%N.toBits()", name, name)
                Comparison.NULLABLE_BITS -> CodeBlock.of(
                    "this.%N?.toBits() == other.%N?.toBits()",
                    name,
                    name
                )
            }
        }
        return buildCodeBlock {
            addStatement("if (this === other) return true")
            addStatement("if (other !is %T) return false", className)
            add("return %L\n", comparisons)
        }
    }

    /**
     * Generates a hash-code accumulation with content hashing for byte arrays.
     *
     * The first property initializes the hash; each remaining property contributes
     * through the same `31 * result + hash` sequence used by data classes.
     */
    private fun contentHashCodeBody(properties: List<Property>): CodeBlock = buildCodeBlock {
        properties.forEachIndexed { index, property ->
            val hash = if (property.comparison == Comparison.CONTENT) {
                CodeBlock.of("%M(this.%N)", KotlinTypes.CONTENT_HASH_CODE, property.name)
            } else {
                CodeBlock.of("this.%N.hashCode()", property.name)
            }
            if (index == 0) add("var result = %L\n", hash) else add(
                "result = 31 * result + %L\n",
                hash
            )
        }
        addStatement("return result")
    }

    /**
     * Generates a data-class-style string expression with content rendering for byte arrays.
     *
     * The expression uses explicit `this.` references to avoid name collisions.
     *
     * @param className the generated class name used as the string prefix
     * @param properties the properties to render in constructor order
     * @return the Kotlin string concatenation expression
     */
    private fun toStringExpression(className: ClassName, properties: List<Property>): CodeBlock =
        buildCodeBlock {
            properties.forEachIndexed { index, property ->
                val name = property.name
                if (index == 0) add("%S + ", "${className.simpleName}($name=") else add(
                    " + %S + ",
                    ", $name="
                )
                if (property.comparison == Comparison.CONTENT) {
                    add("%M(this.%N)", KotlinTypes.CONTENT_TO_STRING, name)
                } else {
                    add("this.%N", name)
                }
            }
            add(" + %S", ")")
        }

    /**
     * Wraps a generated equality body in an override of `Any.equals`.
     *
     * @param body the Kotlin code implementing equality
     * @return the generated `equals` function
     */
    private fun equalsFunction(body: CodeBlock): FunSpec = FunSpec.builder("equals")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("other", ANY.copy(nullable = true))
        .returns(BOOLEAN)
        .addCode(body)
        .build()

    /**
     * Wraps a generated hashing body in an override of `Any.hashCode`.
     *
     * @param body the Kotlin code computing the hash
     * @return the generated `hashCode` function
     */
    private fun hashCodeFunction(body: CodeBlock): FunSpec = FunSpec.builder("hashCode")
        .addModifiers(KModifier.OVERRIDE)
        .returns(INT)
        .addCode(body)
        .build()

    /**
     * Wraps a generated string body in an override of `Any.toString`.
     *
     * @param body the Kotlin code producing the string representation
     * @return the generated `toString` function
     */
    private fun toStringFunction(body: CodeBlock): FunSpec = FunSpec.builder("toString")
        .addModifiers(KModifier.OVERRIDE)
        .returns(STRING)
        .addCode(body)
        .build()

    companion object {
        private const val VALUE = "value"
        private const val NUMBER = "number"
        private const val SERIALIZER = "Serializer"
    }
}

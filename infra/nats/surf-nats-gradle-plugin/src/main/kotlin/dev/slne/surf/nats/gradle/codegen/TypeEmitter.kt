package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.wire.schema.*
import dev.slne.surf.nats.gradle.contract.*
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.model.*
import dev.slne.surf.nats.protocol.ProtocolDescriptors

/**
 * Generates the Kotlin types of Protobuf messages and enums from a [ResolvedContract].
 *
 * Reports nothing: [ContractResolver] has checked every declaration before emission starts.
 */
internal class TypeEmitter(
    private val types: KotlinTypeResolver,
    private val contract: ResolvedContract,
    private val visibility: GeneratedVisibility,
) {

    /**
     * Returns the generated type, or `null` for types mapped by `mapType`.
     */
    fun emit(type: Type): TypeSpec? = when (type) {
        is MessageType -> message(type)
        is EnumType -> enum(type)
        else -> null
    }

    /**
     * Generates a serializable Kotlin message, including its oneofs, nested types, and event metadata.
     *
     * @param type the Protobuf message to generate
     * @return the generated message, or `null` if it is mapped
     */
    private fun message(type: MessageType): TypeSpec? {
        if (types.isMapped(type.type)) return null

        val className = checkNotNull(types.generatedClass(type.type))
        val members = contract.members(type)

        val classBuilder = messageBuilder(type, className, members)
        if (SurfProtoOptions.isEvent(type.options)) addEvent(classBuilder, type, className)

        classBuilder.addTypes(members.filterIsInstance<OneofMember>().map(::oneofType))
        classBuilder.addTypes(type.nestedTypes.mapNotNull(::emit))
        return classBuilder.build()
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
        properties: List<MessageMember>,
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
     * Builds the serialization annotations of a constructor property: `@ProtoOneOf` for a oneof, otherwise
     * `@ProtoNumber`, then `@ProtoType` and `@ProtoPacked` where the field needs them.
     */
    private fun annotations(member: MessageMember): List<AnnotationSpec> = when (member) {
        is OneofMember -> listOf(AnnotationSpec.builder(KotlinTypes.PROTO_ONE_OF).build())
        is FieldMember -> listOfNotNull(
            protoNumber(member.field),
            encodingAnnotation(member.encoding),
            packedAnnotation(member),
        )
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
     * Builds the `@ProtoPacked` annotation of a packed repeated field.
     *
     * @return the annotation, or `null` for a field that is not packed
     */
    private fun packedAnnotation(member: FieldMember): AnnotationSpec? {
        if (!member.packed) return null
        return AnnotationSpec.builder(KotlinTypes.PROTO_PACKED).build()
    }

    /**
     * Generates a sealed interface for a Protobuf oneof, with one data class for each case.
     *
     * The interface represents the optional union property in the enclosing message.
     * Case classes inherit the visibility of their enclosing sealed interface.
     */
    private fun oneofType(member: OneofMember): TypeSpec {
        val interfaceBuilder = TypeSpec.interfaceBuilder(member.interfaceName)
            .addModifiers(KModifier.SEALED)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .applyVisibility(visibility)

        for (case in member.cases) {
            interfaceBuilder.addType(oneofCase(member.interfaceName, case))
        }
        return interfaceBuilder.build()
    }

    /**
     * Generates the data class of a oneof case, which wraps the case field as its only property.
     */
    private fun oneofCase(interfaceName: ClassName, case: OneofCase): TypeSpec {
        // The case classes inherit the visibility of the oneof interface.
        return TypeSpec.classBuilder(case.className)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .addSuperinterface(interfaceName)
            .addDataProperties(case.className, listOf(case.value))
            .build()
    }

    /**
     * Implements the NATS event contract on a message and adds its descriptor companion.
     *
     * @param builder the message class being generated
     * @param type the event's Protobuf declaration
     * @param className the generated event class
     */
    private fun addEvent(builder: TypeSpec.Builder, type: MessageType, className: ClassName) {
        builder.addSuperinterface(KotlinTypes.NATS_EVENT)
        builder.addProperty(eventDescriptorProperty())
        builder.addType(eventDescriptorCompanion(className, type.type.toString()))
    }

    /**
     * Generates the overridden event descriptor getter, which delegates to the companion descriptor.
     */
    private fun eventDescriptorProperty(): PropertySpec = PropertySpec.builder(
        GeneratedNames.NATS_DESCRIPTOR,
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
     * Generates a Kotlin enum whose entries carry Protobuf numeric values, with a serializer that encodes and
     * decodes those values.
     *
     * @return the generated enum, or `null` for a mapped enum
     */
    private fun enum(type: EnumType): TypeSpec? {
        if (types.isMapped(type.type)) return null
        val className = checkNotNull(types.generatedClass(type.type))
        return enumType(type, className, contract.entries(type))
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
        val number = GeneratedNames.ENUM_NUMBER
        val serializerName = className.nestedClass(GeneratedNames.ENUM_SERIALIZER)
        val enumBuilder = TypeSpec.enumBuilder(className)
            .addAnnotation(KotlinTypes.serializableWith(serializerName))
            .primaryConstructor(FunSpec.constructorBuilder().addParameter(number, INT).build())
            .addProperty(PropertySpec.builder(number, INT).initializer("%N", number).build())
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
        // TODO: generate a fast lookup type
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
        .addStatement("encoder.encodeInt(value.%N)", GeneratedNames.ENUM_NUMBER)
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
        properties: List<MessageMember>
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
                    .addAnnotations(annotations(property))
                    .build()
            )
        }
        primaryConstructor(constructor.build())
        if (properties.any { it.comparison == MemberComparison.CONTENT }) {
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
    private fun contentMembers(className: ClassName, properties: List<MessageMember>): List<FunSpec> {
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
    private fun contentEqualsBody(className: ClassName, properties: List<MessageMember>): CodeBlock {
        val comparisons = properties.joinToCode(" && ") { property ->
            val name = property.name
            when (property.comparison) {
                MemberComparison.CONTENT -> CodeBlock.of(
                    "%M(this.%N, other.%N)",
                    KotlinTypes.CONTENT_EQUALS,
                    name,
                    name
                )

                MemberComparison.EQUALS -> CodeBlock.of("this.%N == other.%N", name, name)
                MemberComparison.BITS -> CodeBlock.of("this.%N.toBits() == other.%N.toBits()", name, name)
                MemberComparison.NULLABLE_BITS -> CodeBlock.of(
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
    private fun contentHashCodeBody(properties: List<MessageMember>): CodeBlock = buildCodeBlock {
        properties.forEachIndexed { index, property ->
            val hash = if (property.comparison == MemberComparison.CONTENT) {
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
    private fun toStringExpression(className: ClassName, properties: List<MessageMember>): CodeBlock =
        buildCodeBlock {
            properties.forEachIndexed { index, property ->
                val name = property.name
                if (index == 0) add("%S + ", "${className.simpleName}($name=") else add(
                    " + %S + ",
                    ", $name="
                )
                if (property.comparison == MemberComparison.CONTENT) {
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
}

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
 * Reports nothing: [ContractResolver] has checked every declaration before emission starts. Mapped types produce
 * nothing. Every generated class, interface and enum except oneof case classes gets the configured visibility
 * modifier. Members and case classes are emitted `public`, which an `internal` enclosing declaration limits to the
 * module. The runtime relies on these generated shapes:
 *
 * - **Message:** `@Serializable @SerialName("<proto full name>") data class` with one constructor property per
 *   member in [ResolvedContract.members] order. A field property carries `@ProtoNumber(<tag>)`, plus `@ProtoType`
 *   for a non-default integer encoding and `@ProtoPacked` for a packed repeated field. When a member compares by
 *   content ([MemberComparison.CONTENT]), the class overrides `equals`, `hashCode` and `toString` (see
 *   [contentMembers]). A message without members is a plain class whose instances are all equal.
 * - **Oneof:** a nested `@Serializable sealed interface`, held in a nullable `@ProtoOneOf` property that defaults to
 *   `null`. Each case is a nested `@Serializable data class` with the case field as its only property.
 * - **Event** (message with the `(surf.nats.v1.event)` option): implements `NatsEvent`, overrides
 *   `natsDescriptor`, and has a companion with
 *   `@JvmField val NATS_DESCRIPTOR = NatsEventDescriptor(protoName, serializer())`.
 * - **Enum:** `@Serializable(with = <Enum>.Serializer::class) enum class` with a `number: Int` constructor property
 *   and one entry per [ResolvedContract.entries] item in declaration order. The companion has
 *   `@JvmStatic fun forNumber(number: Int): <Enum>?`, a `when` over the numbers that returns `null` for unknown
 *   ones. The nested `Serializer` object uses a primitive `INT` descriptor named after the
 *   proto type, encodes [GeneratedNames.ENUM_NUMBER], and decodes through `forNumber`. An unknown number decodes to the
 *   entry with number 0, or to the first entry when there is none. A valid enum always starts with its zero value.
 *   Decoding an enum without entries throws [IllegalStateException].
 *
 * Nested messages and enums are nested classes of their parent message.
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
     * Generates a message class with its oneofs, nested types and event members, or `null` for a mapped message.
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
     * Starts the message class with its serialization annotations and either its data properties or the members of
     * a fieldless class.
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

    private fun protoNumber(field: Field): AnnotationSpec {
        return AnnotationSpec.builder(KotlinTypes.PROTO_NUMBER).addMember("%L", field.tag).build()
    }

    /**
     * Returns `@ProtoType` for a non-default integer encoding, or `null`.
     */
    private fun encodingAnnotation(encoding: IntEncoding?): AnnotationSpec? {
        if (encoding == null || encoding == IntEncoding.DEFAULT) return null
        return AnnotationSpec.builder(KotlinTypes.PROTO_TYPE)
            .addMember("%T.%L", KotlinTypes.PROTO_INTEGER_TYPE, encoding.name)
            .build()
    }

    private fun packedAnnotation(member: FieldMember): AnnotationSpec? {
        if (!member.packed) return null
        return AnnotationSpec.builder(KotlinTypes.PROTO_PACKED).build()
    }

    /**
     * Generates the sealed interface of a oneof with one data class per case.
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
        return TypeSpec.classBuilder(case.className)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .addSuperinterface(interfaceName)
            .addDataProperties(case.className, listOf(case.value))
            .build()
    }

    /**
     * Makes the message a `NatsEvent` with its descriptor companion.
     */
    private fun addEvent(builder: TypeSpec.Builder, type: MessageType, className: ClassName) {
        builder.addSuperinterface(KotlinTypes.NATS_EVENT)
        builder.addProperty(eventDescriptorProperty())
        builder.addType(eventDescriptorCompanion(className, type.type.toString()))
    }

    /**
     * Generates the `natsDescriptor` override that returns the companion descriptor.
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
     * Generates the event companion with the `@JvmField` descriptor of the Protobuf name and the generated
     * serializer.
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
     * Generates a numbered enum, or `null` for a mapped enum.
     */
    private fun enum(type: EnumType): TypeSpec? {
        if (types.isMapped(type.type)) return null
        val className = checkNotNull(types.generatedClass(type.type))
        return enumType(type, className, contract.entries(type))
    }

    /**
     * Builds the enum with one entry per item of [entries], its `forNumber` companion and its serializer.
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
        enumBuilder.addType(enumCompanion(className, entries))
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

    private fun enumCompanion(enumClass: ClassName, entries: Map<String, EnumConstant>): TypeSpec =
        TypeSpec.companionObjectBuilder()
            .addFunction(enumForNumberFunction(enumClass, entries))
            .build()

    /**
     * Generates `forNumber`, which maps a Protobuf number to its entry or `null`.
     *
     * Every branch is an `Int` constant, so the compiler emits a `tableswitch` or `lookupswitch` without boxing or
     * a map. `@JvmStatic` gives Java callers `Enum.forNumber(n)` as in protobuf-java.
     */
    private fun enumForNumberFunction(
        enumClass: ClassName,
        entries: Map<String, EnumConstant>,
    ): FunSpec {
        val number = GeneratedNames.ENUM_NUMBER
        val function = FunSpec.builder(GeneratedNames.ENUM_FOR_NUMBER)
            .addAnnotation(JvmStatic::class)
            .addKdoc("Returns the entry with the Protobuf number [%N], or `null` if there is none.", number)
            .addParameter(number, INT)
            .returns(enumClass.copy(nullable = true))
            .beginControlFlow("return when (%N)", number)
        for ((kotlinName, constant) in entries) {
            function.addCode("%L -> %T.%N\n", constant.tag, enumClass, kotlinName)
        }
        return function.addCode("else -> null\n").endControlFlow().build()
    }

    /**
     * Generates the enum serializer, which encodes the Protobuf number and decodes through `forNumber`.
     *
     * Unknown numbers decode to the zero-valued entry or, if none exists, the first entry. Without entries,
     * decoding fails with `error(...)`.
     */
    private fun enumSerializer(
        enumClass: ClassName,
        protoName: String,
        serializerName: ClassName,
        entries: Map<String, EnumConstant>,
    ): TypeSpec {
        val fallback =
            entries.entries.firstOrNull { it.value.tag == 0 } ?: entries.entries.firstOrNull()
        return TypeSpec.objectBuilder(serializerName)
            .addSuperinterface(KotlinTypes.K_SERIALIZER.parameterizedBy(enumClass))
            .addProperty(enumSerializationDescriptor(protoName))
            .addFunction(enumSerializeFunction(enumClass))
            .addFunction(enumDeserializeFunction(enumClass, protoName, fallback))
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

    private fun enumSerializeFunction(enumClass: ClassName): FunSpec = FunSpec.builder("serialize")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("encoder", KotlinTypes.ENCODER)
        .addParameter("value", enumClass)
        .addStatement("encoder.encodeInt(value.%N)", GeneratedNames.ENUM_NUMBER)
        .build()

    private fun enumDeserializeFunction(
        enumClass: ClassName,
        protoName: String,
        fallback: Map.Entry<String, EnumConstant>?,
    ): FunSpec {
        val function = FunSpec.builder("deserialize")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("decoder", KotlinTypes.DECODER)
            .returns(enumClass)
        val lookup = CodeBlock.of("%T.%N(decoder.decodeInt())", enumClass, GeneratedNames.ENUM_FOR_NUMBER)
        if (fallback != null) {
            function.addStatement("return %L ?: %T.%N", lookup, enumClass, fallback.key)
        } else {
            function.addStatement("return %L ?: error(%S)", lookup, "$protoName has no values")
        }
        return function.build()
    }

    /**
     * Makes this a data class with [properties] as constructor properties in the given order.
     *
     * Adds explicit `equals`, `hashCode` and `toString` only when a property compares by content.
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
     * Generates the members of a fieldless message: all instances are equal, the hash code is the hash of the
     * canonical class name, and `toString` returns `ClassName()`.
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
     * Generates `equals`, `hashCode` and `toString` with data-class semantics, except that `bytes` values compare,
     * hash and print by content.
     *
     * Content members (`ByteArray` and lists or maps of it) use `natsContentEquals`, `natsContentHashCode` and
     * `natsContentToString`. `Float` and `Double` compare by `toBits()`, as data classes do, so `NaN` equals `NaN`
     * and `0.0` differs from `-0.0`.
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
     * Generates the `equals` body. Explicit `this.` references avoid collisions with property names such as `other`.
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
     * Generates the `hashCode` body with the `31 * result + hash` sequence of data classes.
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
     * Generates the `toString` expression in data-class form, `Name(a=..., b=...)`.
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

    private fun equalsFunction(body: CodeBlock): FunSpec = FunSpec.builder("equals")
        .addModifiers(KModifier.OVERRIDE)
        .addParameter("other", ANY.copy(nullable = true))
        .returns(BOOLEAN)
        .addCode(body)
        .build()

    private fun hashCodeFunction(body: CodeBlock): FunSpec = FunSpec.builder("hashCode")
        .addModifiers(KModifier.OVERRIDE)
        .returns(INT)
        .addCode(body)
        .build()

    private fun toStringFunction(body: CodeBlock): FunSpec = FunSpec.builder("toString")
        .addModifiers(KModifier.OVERRIDE)
        .returns(STRING)
        .addCode(body)
        .build()
}

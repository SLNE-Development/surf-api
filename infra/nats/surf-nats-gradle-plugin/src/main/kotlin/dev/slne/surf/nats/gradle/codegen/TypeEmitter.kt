package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.jvm.jvmField
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.Type
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.model.*
import dev.slne.surf.nats.protocol.ProtocolDescriptors
import dev.slne.surf.nats.protocol.ProtocolNaming

private const val NATS_DESCRIPTOR_PROPERTY = "natsDescriptor"

/**
 * Generates Kotlin types for Protobuf messages and enums.
 *
 * Messages may be generated as data classes, fieldless classes, or NATS events.
 * The emitter also generates nested types, oneof interfaces, and enum serializers.
 *
 * @param model the contract model used to resolve types and report errors
 * @param visibility the visibility applied to generated top-level and oneof types
 */
internal class TypeEmitter(
    private val model: ContractModel,
    private val visibility: GeneratedVisibility,
) {
    private val problems get() = model.problems

    /**
     * Generates a Kotlin type specification for a Protobuf message or enum.
     *
     * @param type the Protobuf type to generate
     * @param className the Kotlin class name to generate
     * @return the generated type, or null if the type is unsupported or mapped externally
     */
    fun emit(type: Type, className: ClassName): TypeSpec? = when (type) {
        is MessageType -> message(type, className)
        is EnumType -> enum(type, className)
        else -> null
    }

    /**
     * Generates a serializable Kotlin class for a Protobuf message.
     *
     * Messages with fields become data classes, while fieldless messages receive
     * generated equality, hashing, and string representation methods. Events,
     * oneofs, and nested types are added when present.
     *
     * @return the generated class, or null if the message is mapped externally
     */
    private fun message(type: MessageType, className: ClassName): TypeSpec? {
        val isEvent = SurfProtoOptions.event(type.options, type.location, problems)

        if (model.isMapped(type.type)) {
            if (isEvent) {
                problems.error(
                    type.location,
                    "event message ${type.type} is mapped by mapType; events must be generated"
                )
            }
            return null
        }

        val shape = model.shape(type, className)

        val serialNameAnnotation = AnnotationSpec.builder(KotlinTypes.SERIAL_NAME)
            .addMember("%S", type.type.toString())
            .build()

        val classBuilder = TypeSpec.classBuilder(className)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .addAnnotation(serialNameAnnotation)
            .applyVisibility(visibility)

        if (shape.fieldless) {
            classBuilder.addFunctions(fieldlessMembers(className))
        } else {
            classBuilder.addModifiers(KModifier.DATA)

            val constructor = FunSpec.constructorBuilder()
            for (property in shape.properties) {
                val parameter = ParameterSpec.builder(property.name, property.type)
                    .defaultValue(property.default)
                    .build()

                val propertySpec = PropertySpec.builder(property.name, property.type)
                    .initializer("%N", property.name)
                    .addAnnotations(property.annotations)
                    .build()

                constructor.addParameter(parameter)
                classBuilder.addProperty(propertySpec)
            }

            classBuilder.primaryConstructor(constructor.build())
            if (shape.contentAware) { // e.g. byte array
                classBuilder.addFunctions(contentMembers(shape))
            }
        }

        if (isEvent) {
            addEvent(classBuilder, type, className, shape)
        }

        val nestedNames = HashMap<String, Location>()
        for (oneof in shape.oneofs) {
            nestedNames[oneof.interfaceName.simpleName] = oneof.location
            classBuilder.addType(oneofType(oneof))
        }

        for (nested in type.nestedTypes) {
            checkNestedName(className, nested, nestedNames)

            val nestedClassName = className.nestedClass(nested.type.simpleName)
            val nestedSpec = emit(nested, nestedClassName) ?: continue
            classBuilder.addType(nestedSpec)
        }

        return classBuilder.build()
    }

    /**
     * Reports names of nested types that conflict with the enclosing class,
     * the generated companion object, or a previously registered oneof type.
     *
     * @param owner the enclosing Kotlin class
     * @param nested the nested Protobuf type to validate
     * @param used previously registered nested names and their source locations
     */
    private fun checkNestedName(
        owner: ClassName,
        nested: Type,
        used: MutableMap<String, Location>,
    ) {
        when (val name = nested.type.simpleName) {
            owner.simpleName -> {
                problems.error(
                    nested.location,
                    "nested type $name must not have the name of its enclosing type",
                )
            }

            COMPANION -> {
                problems.error(
                    nested.location,
                    "nested type name Companion clashes with the generated companion object",
                )
            }

            else -> {
                val previousLocation = used.put(name, nested.location)
                if (previousLocation != null) {
                    problems.error(
                        nested.location,
                        "nested type $name clashes with the oneof declared at $previousLocation",
                    )
                }
            }
        }
    }

    /**
     * Adds the NATS event interface, descriptor property, and companion object
     * to a generated message class.
     *
     * Also reports conflicting field names and invalid event names.
     *
     * @param builder the message class builder to extend
     * @param type the source Protobuf message
     * @param className the generated Kotlin class name
     * @param shape the resolved message shape
     */
    private fun addEvent(
        builder: TypeSpec.Builder,
        type: MessageType,
        className: ClassName,
        shape: MessageShape
    ) {
        val conflictingProperty = shape.properties.find { it.name == NATS_DESCRIPTOR_PROPERTY }
        if (conflictingProperty != null) {
            problems.error(
                conflictingProperty.location,
                "event ${type.type} must not have a field named $NATS_DESCRIPTOR_PROPERTY",
            )
        }

        val protoName = type.type.toString()
        ProtocolNaming.nameProblem("event", protoName, type.name)?.let { problem ->
            problems.error(type.location, problem)
        }

        val descriptorType = KotlinTypes.NATS_EVENT_DESCRIPTOR.parameterizedBy(className)

        val descriptorGetter = FunSpec.getterBuilder()
            .addStatement("return %N", ProtocolDescriptors.FIELD)
            .build()

        val descriptorProperty = PropertySpec.builder(
            NATS_DESCRIPTOR_PROPERTY,
            KotlinTypes.NATS_EVENT_DESCRIPTOR.parameterizedBy(STAR),
        )
            .addModifiers(KModifier.OVERRIDE)
            .getter(descriptorGetter)
            .build()

        val descriptorInitializer = buildCodeBlock {
            add("%T(\n", descriptorType)
            withIndent {
                add("protoName = %S,\n", protoName)
                add("serializer = serializer(),\n")
            }
            add(")")
        }

        val companionProperty = PropertySpec.builder(ProtocolDescriptors.FIELD, descriptorType)
            .jvmField()
            .initializer(descriptorInitializer)
            .build()

        val companionObject = TypeSpec.companionObjectBuilder()
            .addProperty(companionProperty)
            .build()

        builder.addSuperinterface(KotlinTypes.NATS_EVENT)
        builder.addProperty(descriptorProperty)
        builder.addType(companionObject)
    }

    /**
     * Generates a sealed, serializable interface and its nested data classes
     * for a Protobuf oneof declaration.
     *
     * @param oneof the resolved oneof model
     * @return the generated sealed interface containing all oneof cases
     */
    private fun oneofType(oneof: OneofModel): TypeSpec {
        val interfaceBuilder = TypeSpec.interfaceBuilder(oneof.interfaceName)
            .addModifiers(KModifier.SEALED)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .applyVisibility(visibility)

        for (case in oneof.cases) {
            val constructor = FunSpec.constructorBuilder()
                .addParameter(VALUE, case.valueType)
                .build()

            val valueProperty = PropertySpec.builder(VALUE, case.valueType)
                .initializer("%N", VALUE)
                .addAnnotations(case.annotations)
                .build()

            val caseBuilder = TypeSpec.classBuilder(case.className)
                .addModifiers(KModifier.DATA)
                .addAnnotation(KotlinTypes.SERIALIZABLE)
                .addSuperinterface(oneof.interfaceName)
                .primaryConstructor(constructor)
                .addProperty(valueProperty)

            // Nested case classes inherit visibility from the oneof interface.
            if (case.contentAware) {
                val contentProperty = ContentProperty(VALUE, Comparison.CONTENT)
                caseBuilder.addFunctions(contentMembers(case.className, listOf(contentProperty)))
            }

            interfaceBuilder.addType(caseBuilder.build())
        }

        return interfaceBuilder.build()
    }

    /**
     * Generates a Kotlin enum and its nested serializer for a Protobuf enum.
     *
     * @return the generated enum, or null if the enum is mapped externally
     */
    private fun enum(type: EnumType, className: ClassName): TypeSpec? {
        if (model.isMapped(type.type)) return null

        val enumModel = model.enumModel(type, className)
        val serializerName = className.nestedClass(SERIALIZER)

        val constructor = FunSpec.constructorBuilder()
            .addParameter(NUMBER, INT)
            .build()

        val numberProperty = PropertySpec.builder(NUMBER, INT)
            .initializer("%N", NUMBER)
            .build()

        val enumBuilder = TypeSpec.enumBuilder(className)
            .addAnnotation(KotlinTypes.serializableWith(serializerName))
            .primaryConstructor(constructor)
            .addProperty(numberProperty)
            .applyVisibility(visibility)

        for (entry in enumModel.entries) {
            val enumConstant = TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%L", entry.number)
                .build()

            enumBuilder.addEnumConstant(entry.kotlinName, enumConstant)
        }


        enumBuilder.addType(enumSerializer(enumModel, serializerName))

        return enumBuilder.build()
    }

    /**
     * Generates a serializer that encodes enum values as integers and decodes
     * unknown values to the zero-valued entry or the first declared entry.
     *
     * If the enum has no entries, decoding an unknown value throws an error.
     *
     * @param enumModel the resolved enum model
     * @param serializerName the name of the nested serializer object
     * @return the generated serializer object
     */
    private fun enumSerializer(
        enumModel: EnumModel,
        serializerName: ClassName
    ): TypeSpec { // TODO: generate an Int2Object mapping for lookup
        val enumClass = enumModel.className
        val fallback = enumModel.zero ?: enumModel.entries.firstOrNull()

        val decodeBody = buildCodeBlock {
            add("return when (decoder.decodeInt()) {\n")
            withIndent {
                for (entry in enumModel.entries) {
                    add("%L -> %T.%N\n", entry.number, enumClass, entry.kotlinName)
                }

                if (fallback != null) {
                    add("else -> %T.%N\n", enumClass, fallback.kotlinName)
                } else {
                    add("else -> error(%S)\n", "${enumModel.protoName} has no values")
                }
            }
            add("}\n")
        }

        val descriptorProperty = PropertySpec.builder("descriptor", KotlinTypes.SERIAL_DESCRIPTOR)
            .addModifiers(KModifier.OVERRIDE)
            .initializer(
                "%T(%S, %T.INT)",
                KotlinTypes.PRIMITIVE_SERIAL_DESCRIPTOR,
                enumModel.protoName,
                KotlinTypes.PRIMITIVE_KIND,
            )
            .build()

        val serializeFunction = FunSpec.builder("serialize")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("encoder", KotlinTypes.ENCODER)
            .addParameter("value", enumClass)
            .addStatement("encoder.encodeInt(value.%N)", NUMBER)
            .build()

        val deserializeFunction = FunSpec.builder("deserialize")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("decoder", KotlinTypes.DECODER)
            .returns(enumClass)
            .addCode(decodeBody)
            .build()

        return TypeSpec.objectBuilder(serializerName)
            .addSuperinterface(KotlinTypes.K_SERIALIZER.parameterizedBy(enumClass))
            .addProperty(descriptorProperty)
            .addFunction(serializeFunction)
            .addFunction(deserializeFunction)
            .build()
    }

    /**
     * Generates value-like equality, hashCode, and toString implementations
     * for a message without fields.
     *
     * @param className the generated message class name
     * @return the generated equals, hashCode, and toString functions
     */
    private fun fieldlessMembers(className: ClassName): List<FunSpec> {
        val equalsFunction = FunSpec.builder("equals")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("other", ANY.copy(nullable = true))
            .returns(BOOLEAN)
            .addStatement("return other is %T", className)
            .build()

        val hashCodeBody = CodeBlock.builder()
            .add("return ")
            .add(DefaultValues.int(className.canonicalName.hashCode()))
            .add("\n")
            .build()

        val hashCodeFunction = FunSpec.builder("hashCode")
            .addModifiers(KModifier.OVERRIDE)
            .returns(INT)
            .addCode(hashCodeBody)
            .build()

        val toStringFunction = FunSpec.builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .returns(STRING)
            .addStatement("return %S", "${className.simpleName}()")
            .build()

        return listOf(equalsFunction, hashCodeFunction, toStringFunction)
    }

    /**
     * Selects the appropriate equality strategy for each message property
     * and generates content-aware data-class methods.
     *
     * @param shape the resolved message shape
     * @return the generated equals, hashCode, and toString functions
     */
    private fun contentMembers(shape: MessageShape): List<FunSpec> {
        val properties = shape.properties.map { property ->
            val comparison = when {
                property.contentAware -> Comparison.CONTENT
                property.kClass != FLOAT && property.kClass != DOUBLE -> Comparison.EQUALS
                property.nullable -> Comparison.NULLABLE_BITS
                else -> Comparison.BITS
            }

            ContentProperty(property.name, comparison)
        }

        return contentMembers(shape.className, properties)
    }

    /**
     * Generates data-class-like equals, hashCode, and toString functions with
     * content-based comparison for byte arrays.
     *
     * Floating-point values are compared using toBits(), matching data-class
     * equality semantics for NaN and signed zero. Explicit `this.` references
     * prevent collisions with local variable names in generated code.
     *
     * @param className the generated message class name
     * @param properties the properties and their comparison strategies
     * @return the generated equals, hashCode, and toString functions
     */
    private fun contentMembers(
        className: ClassName,
        properties: List<ContentProperty>,
    ): List<FunSpec> {
        val comparisons = properties.map { (name, comparison) ->
            when (comparison) {
                Comparison.CONTENT -> CodeBlock.of(
                    "%M(this.%N, other.%N)",
                    KotlinTypes.CONTENT_EQUALS,
                    name,
                    name,
                )

                Comparison.EQUALS -> CodeBlock.of(
                    "this.%N == other.%N",
                    name,
                    name,
                )

                Comparison.BITS -> CodeBlock.of(
                    "this.%N.toBits() == other.%N.toBits()",
                    name,
                    name,
                )

                Comparison.NULLABLE_BITS -> CodeBlock.of(
                    "this.%N?.toBits() == other.%N?.toBits()",
                    name,
                    name,
                )
            }
        }

        val equalsBody = CodeBlock.builder()
            .addStatement("if (this === other) return true")
            .addStatement("if (other !is %T) return false", className)
            .add("return ")
            .add(comparisons.joinToCode(" && "))
            .add("\n")
            .build()

        val hashCodeBody = buildCodeBlock {
            properties.forEachIndexed { index, (name, comparison) ->
                val hash = if (comparison == Comparison.CONTENT) {
                    CodeBlock.of("%M(this.%N)", KotlinTypes.CONTENT_HASH_CODE, name)
                } else {
                    CodeBlock.of("this.%N.hashCode()", name)
                }

                if (index == 0) {
                    add("var result = ")
                } else {
                    add("result = 31 * result + ")
                }

                add(hash)
                add("\n")
            }
        }

        val toStringBody = CodeBlock.builder()
            .add("return ")
            .add(buildToString(className, properties))
            .add("\n")
            .build()

        val equalsFunction = FunSpec.builder("equals")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("other", ANY.copy(nullable = true))
            .returns(BOOLEAN)
            .addCode(equalsBody)
            .build()

        val hashCodeFunction = FunSpec.builder("hashCode")
            .addModifiers(KModifier.OVERRIDE)
            .returns(INT)
            .addCode(hashCodeBody)
            .build()

        val toStringFunction = FunSpec.builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .returns(STRING)
            .addCode(toStringBody)
            .build()

        return listOf(equalsFunction, hashCodeFunction, toStringFunction)
    }

    /**
     * Builds the return expression for a data-class-like toString function.
     *
     * Content-aware values use the NATS content formatter, while other values
     * are appended directly. For example, a message with fields `a` and `b`
     * produces an expression resembling `"X(a=" + this.a + ", b=" + this.b + ")"`.
     *
     * @param className the generated message class name
     * @param properties the properties and their comparison strategies
     * @return the generated toString expression
     */
    private fun buildToString(className: ClassName, properties: List<ContentProperty>): CodeBlock {
        val builder = CodeBlock.builder()

        properties.forEachIndexed { index, (name, comparison) ->
            val prefix = if (index == 0) "${className.simpleName}(" else ", "
            val label = "$prefix$name="

            if (index == 0) {
                builder.add("%S + ", label)
            } else {
                builder.add(" + %S + ", label)
            }

            if (comparison == Comparison.CONTENT) {
                builder.add("%M(this.%N)", KotlinTypes.CONTENT_TO_STRING, name)
            } else {
                builder.add("this.%N", name)
            }
        }

        return builder.add(" + %S", ")").build()
    }

    private data class ContentProperty(val name: String, val comparison: Comparison)

    private enum class Comparison {
        CONTENT,
        EQUALS,
        BITS,
        NULLABLE_BITS,
    }

    companion object {
        private const val VALUE = "value"
        private const val NUMBER = "number"
        private const val SERIALIZER = "Serializer"
    }
}

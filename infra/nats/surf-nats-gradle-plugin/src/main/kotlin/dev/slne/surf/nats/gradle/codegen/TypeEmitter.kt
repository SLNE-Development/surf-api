package dev.slne.surf.nats.gradle.codegen

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.joinToCode
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.Type
import dev.slne.surf.nats.gradle.generator.GeneratedVisibility
import dev.slne.surf.nats.gradle.model.COMPANION
import dev.slne.surf.nats.gradle.model.ContractModel
import dev.slne.surf.nats.gradle.model.DefaultValues
import dev.slne.surf.nats.gradle.model.EnumModel
import dev.slne.surf.nats.gradle.model.KotlinTypes
import dev.slne.surf.nats.gradle.model.MessageShape
import dev.slne.surf.nats.gradle.model.OneofModel
import dev.slne.surf.nats.gradle.model.SurfOptions
import dev.slne.surf.nats.protocol.ProtocolDescriptors
import dev.slne.surf.nats.protocol.ProtocolNaming

private const val NATS_DESCRIPTOR_PROPERTY = "natsDescriptor"

/**
 * Emits messages (data classes, fieldless classes, events, oneofs) and enums.
 */
internal class TypeEmitter(
    private val model: ContractModel,
    private val visibility: GeneratedVisibility,
) {
    private val problems get() = model.problems

    fun emit(type: Type, className: ClassName): TypeSpec? = when (type) {
        is MessageType -> message(type, className)
        is EnumType -> enum(type, className)
        else -> null
    }

    private fun message(type: MessageType, className: ClassName): TypeSpec? {
        val event = SurfOptions.event(type.options, type.location, problems)
        if (model.isMapped(type.type)) {
            if (event) {
                problems.error(type.location, "event message ${type.type} is mapped by mapType; events must be generated")
            }
            return null
        }

        val shape = model.shape(type, className)
        val builder = TypeSpec.classBuilder(className)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .addAnnotation(AnnotationSpec.builder(KotlinTypes.SERIAL_NAME).addMember("%S", type.type.toString()).build())
            .applyVisibility(visibility)

        if (shape.fieldless) {
            builder.addFunctions(fieldlessMembers(className))
        } else {
            builder.addModifiers(KModifier.DATA)
            val constructor = FunSpec.constructorBuilder()
            for (property in shape.properties) {
                constructor.addParameter(
                    ParameterSpec.builder(property.name, property.type)
                        .apply { property.default?.let { defaultValue(it) } }
                        .build()
                )
                builder.addProperty(
                    PropertySpec.builder(property.name, property.type)
                        .initializer("%N", property.name)
                        .addAnnotations(property.annotations)
                        .build()
                )
            }
            builder.primaryConstructor(constructor.build())
            if (shape.contentAware) builder.addFunctions(contentMembers(shape))
        }

        if (event) addEvent(builder, type, className, shape)

        val nestedNames = HashMap<String, Location>()
        for (oneof in shape.oneofs) {
            nestedNames[oneof.interfaceName.simpleName] = oneof.location
            builder.addType(oneofType(oneof))
        }
        for (nested in type.nestedTypes) {
            checkNestedName(className, nested, nestedNames)
            emit(nested, className.nestedClass(nested.type.simpleName))?.let(builder::addType)
        }
        return builder.build()
    }

    private fun checkNestedName(owner: ClassName, nested: Type, used: MutableMap<String, Location>) {
        val name = nested.type.simpleName
        when {
            name == owner.simpleName ->
                problems.error(nested.location, "nested type $name must not have the name of its enclosing type")

            name == COMPANION -> problems.error(nested.location, "nested type name Companion clashes with the generated companion object")
            else -> used.put(name, nested.location)?.let { previous ->
                problems.error(nested.location, "nested type $name clashes with the oneof declared at $previous")
            }
        }
    }

    private fun addEvent(builder: TypeSpec.Builder, type: MessageType, className: ClassName, shape: MessageShape) {
        shape.properties.firstOrNull { it.name == NATS_DESCRIPTOR_PROPERTY }?.let {
            problems.error(it.location, "event ${type.type} must not have a field named $NATS_DESCRIPTOR_PROPERTY")
        }
        val protoName = type.type.toString()
        ProtocolNaming.nameProblem("event", protoName, type.name)?.let { problems.error(type.location, it) }

        val descriptorType = KotlinTypes.NATS_EVENT_DESCRIPTOR.parameterizedBy(className)
        builder.addSuperinterface(KotlinTypes.NATS_EVENT)
        builder.addProperty(
            PropertySpec.builder(NATS_DESCRIPTOR_PROPERTY, KotlinTypes.NATS_EVENT_DESCRIPTOR.parameterizedBy(STAR))
                .addModifiers(KModifier.OVERRIDE)
                .getter(FunSpec.getterBuilder().addStatement("return %N", ProtocolDescriptors.FIELD).build())
                .build()
        )
        val initializer = CodeBlock.builder()
            .add("%T(\n", descriptorType)
            .indent()
            .add("protoName = %S,\n", protoName)
            .add("serializer = serializer(),\n")
            .unindent()
            .add(")")
            .build()
        builder.addType(
            TypeSpec.companionObjectBuilder()
                .addProperty(
                    PropertySpec.builder(ProtocolDescriptors.FIELD, descriptorType)
                        .addAnnotation(JvmField::class)
                        .initializer(initializer)
                        .build()
                )
                .build()
        )
    }

    private fun oneofType(oneof: OneofModel): TypeSpec {
        val builder = TypeSpec.interfaceBuilder(oneof.interfaceName)
            .addModifiers(KModifier.SEALED)
            .addAnnotation(KotlinTypes.SERIALIZABLE)
            .applyVisibility(visibility)
        for (case in oneof.cases) {
            val caseBuilder = TypeSpec.classBuilder(case.className)
                .addModifiers(KModifier.DATA)
                .addAnnotation(KotlinTypes.SERIALIZABLE)
                .addSuperinterface(oneof.interfaceName)
                .primaryConstructor(FunSpec.constructorBuilder().addParameter(VALUE, case.valueType).build())
                .addProperty(
                    PropertySpec.builder(VALUE, case.valueType)
                        .initializer("%N", VALUE)
                        .addAnnotations(case.annotations)
                        .build()
                )
            // Interface members cannot be internal; the case classes inherit the visibility of their oneof interface.
            if (case.contentAware) {
                caseBuilder.addFunctions(contentMembers(case.className, listOf(ContentProperty(VALUE, Comparison.CONTENT))))
            }
            builder.addType(caseBuilder.build())
        }
        return builder.build()
    }

    private fun enum(type: EnumType, className: ClassName): TypeSpec? {
        if (model.isMapped(type.type)) return null
        val enumModel = model.enumModel(type, className)
        val serializerName = className.nestedClass(SERIALIZER)
        val builder = TypeSpec.enumBuilder(className)
            .addAnnotation(KotlinTypes.serializableWith(serializerName))
            .primaryConstructor(FunSpec.constructorBuilder().addParameter(NUMBER, INT).build())
            .addProperty(PropertySpec.builder(NUMBER, INT).initializer("%N", NUMBER).build())
            .applyVisibility(visibility)
        for (entry in enumModel.entries) {
            builder.addEnumConstant(
                entry.kotlinName,
                TypeSpec.anonymousClassBuilder().addSuperclassConstructorParameter("%L", entry.number).build(),
            )
        }
        builder.addType(enumSerializer(enumModel, serializerName))
        return builder.build()
    }

    private fun enumSerializer(enumModel: EnumModel, serializerName: ClassName): TypeSpec {
        val enumClass = enumModel.className
        val fallback = enumModel.zero ?: enumModel.entries.firstOrNull()
        val decode = CodeBlock.builder().add("return when (decoder.decodeInt()) {\n").indent()
        for (entry in enumModel.entries) decode.add("%L -> %T.%N\n", entry.number, enumClass, entry.kotlinName)
        if (fallback != null) {
            decode.add("else -> %T.%N\n", enumClass, fallback.kotlinName)
        } else {
            decode.add("else -> error(%S)\n", "${enumModel.protoName} has no values")
        }
        decode.unindent().add("}\n")

        return TypeSpec.objectBuilder(serializerName)
            .addSuperinterface(KotlinTypes.K_SERIALIZER.parameterizedBy(enumClass))
            .addProperty(
                PropertySpec.builder("descriptor", KotlinTypes.SERIAL_DESCRIPTOR)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer(
                        "%T(%S, %T.INT)",
                        KotlinTypes.PRIMITIVE_SERIAL_DESCRIPTOR,
                        enumModel.protoName,
                        KotlinTypes.PRIMITIVE_KIND,
                    )
                    .build()
            )
            .addFunction(
                FunSpec.builder("serialize")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("encoder", KotlinTypes.ENCODER)
                    .addParameter("value", enumClass)
                    .addStatement("encoder.encodeInt(value.%N)", NUMBER)
                    .build()
            )
            .addFunction(
                FunSpec.builder("deserialize")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("decoder", KotlinTypes.DECODER)
                    .returns(enumClass)
                    .addCode(decode.build())
                    .build()
            )
            .build()
    }

    private fun fieldlessMembers(className: ClassName): List<FunSpec> = listOf(
        FunSpec.builder("equals")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("other", ANY.copy(nullable = true))
            .returns(BOOLEAN)
            .addStatement("return other is %T", className)
            .build(),
        FunSpec.builder("hashCode")
            .addModifiers(KModifier.OVERRIDE)
            .returns(INT)
            .addCode(CodeBlock.builder().add("return ").add(DefaultValues.int(className.canonicalName.hashCode())).add("\n").build())
            .build(),
        FunSpec.builder("toString")
            .addModifiers(KModifier.OVERRIDE)
            .returns(STRING)
            .addStatement("return %S", "${className.simpleName}()")
            .build(),
    )

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
     * Builds `equals`/`hashCode`/`toString` like a data class but comparing byte arrays by content; `this.` avoids
     * clashes with locals.
     *
     * Floating point properties compare by `toBits()`, the total order data classes use (`NaN` equals `NaN`, `0.0`
     * differs from `-0.0`), which keeps `equals` consistent with `hashCode`.
     */
    private fun contentMembers(className: ClassName, properties: List<ContentProperty>): List<FunSpec> {
        val equals = CodeBlock.builder()
            .addStatement("if (this === other) return true")
            .addStatement("if (other !is %T) return false", className)
        val comparisons = properties.map { (name, comparison) ->
            when (comparison) {
                Comparison.CONTENT -> CodeBlock.of("%M(this.%N, other.%N)", KotlinTypes.CONTENT_EQUALS, name, name)
                Comparison.EQUALS -> CodeBlock.of("this.%N == other.%N", name, name)
                Comparison.BITS -> CodeBlock.of("this.%N.toBits() == other.%N.toBits()", name, name)
                Comparison.NULLABLE_BITS -> CodeBlock.of("this.%N?.toBits() == other.%N?.toBits()", name, name)
            }
        }
        equals.add("return ").add(comparisons.joinToCode(" && ")).add("\n")

        val hashCode = CodeBlock.builder()
        properties.forEachIndexed { index, (name, comparison) ->
            val hash = if (comparison == Comparison.CONTENT) {
                CodeBlock.of("%M(this.%N)", KotlinTypes.CONTENT_HASH_CODE, name)
            } else {
                CodeBlock.of("this.%N.hashCode()", name)
            }
            if (index == 0) hashCode.add("var result = ").add(hash).add("\n") else hashCode.add("result = 31 * result + ").add(hash).add("\n")
        }
        hashCode.addStatement("return result")

        val toString = CodeBlock.builder().add("return ").add(buildToString(className, properties)).add("\n")

        return listOf(
            FunSpec.builder("equals")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("other", ANY.copy(nullable = true))
                .returns(BOOLEAN)
                .addCode(equals.build())
                .build(),
            FunSpec.builder("hashCode").addModifiers(KModifier.OVERRIDE).returns(INT).addCode(hashCode.build()).build(),
            FunSpec.builder("toString").addModifiers(KModifier.OVERRIDE).returns(STRING).addCode(toString.build()).build(),
        )
    }

    /**
     * Builds `"X(a=" + this.a + ", b=" + natsContentToString(this.b) + ")"`.
     */
    private fun buildToString(className: ClassName, properties: List<ContentProperty>): CodeBlock {
        val builder = CodeBlock.builder()
        properties.forEachIndexed { index, (name, comparison) ->
            val label = (if (index == 0) "${className.simpleName}(" else ", ") + "$name="
            builder.add(if (index == 0) "%S + " else " + %S + ", label)
            if (comparison == Comparison.CONTENT) {
                builder.add("%M(this.%N)", KotlinTypes.CONTENT_TO_STRING, name)
            } else {
                builder.add("this.%N", name)
            }
        }
        return builder.add(" + %S", ")").build()
    }

    private data class ContentProperty(val name: String, val comparison: Comparison)

    private enum class Comparison { CONTENT, EQUALS, BITS, NULLABLE_BITS }

    companion object {
        private const val VALUE = "value"
        private const val NUMBER = "number"
        private const val SERIALIZER = "Serializer"
    }
}

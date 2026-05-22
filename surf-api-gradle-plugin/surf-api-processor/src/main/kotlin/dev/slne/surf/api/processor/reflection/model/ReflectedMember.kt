package dev.slne.surf.api.processor.reflection.model

import dev.slne.surf.api.processor.reflection.ReflectionSymbolProcessor
import java.lang.invoke.VarHandle

sealed interface ReflectedMember {
    val id: String
    val wrapperName: String
    val target: ReflectionSymbolProcessor.TypeRef
    val wrapperParameters: List<JavaParameter>
    val returnType: ReflectionSymbolProcessor.JavaType
    val constants: List<ConstantArgument>

    sealed interface WithStaticModifier : ReflectedMember {
        val isStatic: Boolean
    }

    data class MethodMember(
        override val id: String,
        override val wrapperName: String,
        override val target: ReflectionSymbolProcessor.TypeRef,
        val targetName: String,
        override val isStatic: Boolean,
        val descriptor: String,
        val invocation: String,
        override val wrapperParameters: List<JavaParameter>,
        val targetParameters: List<JavaParameter>,
        override val returnType: ReflectionSymbolProcessor.JavaType,
        override val constants: List<ConstantArgument>,
    ) : WithStaticModifier

    data class ConstructorMember(
        override val id: String,
        override val wrapperName: String,
        override val target: ReflectionSymbolProcessor.TypeRef,
        val descriptor: String,
        val invocation: String,
        override val wrapperParameters: List<JavaParameter>,
        override val returnType: ReflectionSymbolProcessor.JavaType,
        override val constants: List<ConstantArgument>,
    ) : ReflectedMember

    data class FieldMember(
        override val id: String,
        override val wrapperName: String,
        override val target: ReflectionSymbolProcessor.TypeRef,
        val fieldName: String,
        override val isStatic: Boolean,
        val access: String,
        val fieldType: ReflectionSymbolProcessor.TypeRef,
        override val wrapperParameters: List<JavaParameter>,
        override val returnType: ReflectionSymbolProcessor.JavaType,
        override val constants: List<ConstantArgument>,
    ) : WithStaticModifier

    data class VarHandleMember(
        override val id: String,
        override val wrapperName: String,
        override val target: ReflectionSymbolProcessor.TypeRef,
        val fieldName: String,
        override val isStatic: Boolean,
        val mode: VarHandle.AccessMode,
        val invocation: String,
        val fieldType: ReflectionSymbolProcessor.TypeRef,
        override val wrapperParameters: List<JavaParameter>,
        override val returnType: ReflectionSymbolProcessor.JavaType,
        override val constants: List<ConstantArgument>,
    ) : WithStaticModifier

}
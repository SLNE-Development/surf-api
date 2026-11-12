package dev.slne.surf.api.processor.reflection.emitter

import com.palantir.javapoet.*
import dev.slne.surf.api.processor.reflection.ReflectionSymbolProcessor
import dev.slne.surf.api.processor.reflection.model.JavaNullability
import dev.slne.surf.api.processor.reflection.model.ReflectedMember
import dev.slne.surf.api.processor.reflection.model.ReflectedMember.*
import dev.slne.surf.api.processor.reflection.model.ReflectionModel
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.VarHandle
import java.lang.reflect.Type
import javax.lang.model.element.Modifier
import javax.lang.model.element.Modifier.*

@Suppress("CanConvertToMultiDollarString", "CanUnescapeDollarLiteral")
class ReflectionJavaEmitter(private val model: ReflectionModel) {
    companion object {
        private val nullableAnnotationClassName = ClassName.get("org.jspecify.annotations", "Nullable")
        private val nonNullAnnotationClassName = ClassName.get("org.jspecify.annotations", "NonNull")

        private val nullableAnnotationSpec = AnnotationSpec.builder(nullableAnnotationClassName).build()
        private val nonNullAnnotationSpec = AnnotationSpec.builder(nonNullAnnotationClassName).build()
    }

    private val modelInterfaceClassName = ClassName.bestGuess(model.interfaceName)
    private val modelClassName = ClassName.get(model.packageName, model.className)

    fun emit(): String {
        val javaClassBuilder = TypeSpec.classBuilder(model.className)
            .addModifiers(PUBLIC, FINAL)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings::class.java)
                    .addMember("value", "{\$S, \$S}", "unchecked", "rawtypes")
                    .build()
            )
            .addSuperinterface(modelInterfaceClassName)
            .addFields(emitFields())
            .addMethod(emitConstructor())
            .addStaticBlock(emitStaticInitializer())
            .addMethods(emitMethods())
            .addMethods(emitHelperMethods())

        return JavaFile.builder(model.packageName, javaClassBuilder.build())
            .build()
            .toString()
    }

    private fun emitFields(): List<FieldSpec> {
        val fields = mutableListOf<FieldSpec>()

        if (model.singleton) {
            fields += FieldSpec.builder(modelInterfaceClassName, "INSTANCE")
                .addModifiers(PUBLIC, STATIC, FINAL)
                .initializer("new \$T()", modelClassName)
                .build()
        }

        for (member in model.members) {
            val fieldType = when (member) {
                is MethodMember -> TypeName.get(MethodHandle::class.java)
                is ConstructorMember -> TypeName.get(MethodHandle::class.java)
                is FieldMember -> TypeName.get(VarHandle::class.java)
                is VarHandleMember -> {
                    if (member.invocation == "EXACT") {
                        TypeName.get(VarHandle::class.java)
                    } else {
                        TypeName.get(MethodHandle::class.java)
                    }
                }
            }

            fields += FieldSpec.builder(fieldType, member.id)
                .addModifiers(PRIVATE, STATIC, FINAL)
                .build()
        }

        return fields
    }

    private fun emitConstructor(): MethodSpec {
        val visibility = if (model.singleton) PRIVATE else PUBLIC

        return MethodSpec.constructorBuilder()
            .addModifiers(visibility)
            .apply {
                if (model.singleton) {
                    addComment("Singleton instance is exposed through INSTANCE.")
                }
            }
            .build()
    }

    private fun emitStaticInitializer(): CodeBlock {
        val block = CodeBlock.builder()

        block.addStatement(
            "\$T lookup = \$T.lookup()",
            MethodHandles.Lookup::class.java,
            MethodHandles::class.java,
        )
        block.addStatement("\$T classLoader = \$T.class.getClassLoader()", ClassLoader::class.java, modelClassName)

        block.beginControlFlow("try")

        model.members.forEachIndexed { index, member ->
            emitMemberInitializer(block, index, member)
            block.add("\n")
        }

        block.nextControlFlow("catch (\$T throwable)", Throwable::class.java)
        block.addStatement("throw new \$T(throwable)", ExceptionInInitializerError::class.java)
        block.endControlFlow()

        return block.build()
    }

    private fun emitMemberInitializer(
        block: CodeBlock.Builder,
        index: Int,
        member: ReflectedMember,
    ) {
        val ownerVariable = "owner\$$index"
        val lookupVariable = "lookup\$$index"

        block.addStatement(
            "\$T<?> \$L = \$L",
            Class::class.java,
            ownerVariable,
            member.target.classExpression(),
        )
        block.addStatement(
            "\$T \$L = \$T.privateLookupIn(\$L, lookup)",
            MethodHandles.Lookup::class.java,
            lookupVariable,
            MethodHandles::class.java,
            ownerVariable,
        )

        when (member) {
            is MethodMember -> emitMethodInitializer(block, member, ownerVariable, lookupVariable)
            is ConstructorMember -> emitConstructorInitializer(block, member, ownerVariable, lookupVariable)
            is FieldMember -> emitFieldInitializer(block, member, ownerVariable, lookupVariable)
            is VarHandleMember -> emitVarHandleInitializer(block, index, member, ownerVariable, lookupVariable)
        }
    }

    private fun emitMethodInitializer(
        block: CodeBlock.Builder,
        member: MethodMember,
        ownerVariable: String,
        lookupVariable: String,
    ) {
        val targetMethodType = if (member.descriptor.isNotBlank()) {
            CodeBlock.of(
                "\$T.fromMethodDescriptorString(\$S, classLoader)",
                MethodType::class.java,
                member.descriptor,
            )
        } else {
            methodTypeExpression(
                returnType = member.returnType,
                parameterTypes = member.targetParameters.map { it.type } + member.constants.map { it.type },
            )
        }

        val rawHandle = if (member.isStatic) {
            CodeBlock.of(
                "\$L.findStatic(\$L, \$S, \$L)",
                lookupVariable,
                ownerVariable,
                member.targetName,
                targetMethodType,
            )
        } else {
            CodeBlock.of(
                "\$L.findVirtual(\$L, \$S, \$L)",
                lookupVariable,
                ownerVariable,
                member.targetName,
                targetMethodType,
            )
        }

        if (member.invocation == "AS_TYPE_EXACT") {
            block.addStatement(
                "\$N = \$L.asType(\$L)",
                member.id,
                rawHandle,
                methodTypeExpression(
                    returnType = member.returnType,
                    parameterTypes = member.wrapperParameters.map { it.type } + member.constants.map { it.type },
                ),
            )
        } else {
            block.addStatement("\$N = \$L", member.id, rawHandle)
        }
    }

    private fun emitConstructorInitializer(
        block: CodeBlock.Builder,
        member: ConstructorMember,
        ownerVariable: String,
        lookupVariable: String,
    ) {
        val constructorType = if (member.descriptor.isNotBlank()) {
            CodeBlock.of(
                "\$T.fromMethodDescriptorString(\$S, classLoader)",
                MethodType::class.java,
                member.descriptor,
            )
        } else {
            methodTypeExpression(
                returnType = ReflectionSymbolProcessor.JavaType.Void,
                parameterTypes = member.wrapperParameters.map { it.type } + member.constants.map { it.type },
            )
        }

        val rawHandle = CodeBlock.of(
            "\$L.findConstructor(\$L, \$L)",
            lookupVariable,
            ownerVariable,
            constructorType,
        )

        if (member.invocation == "AS_TYPE_EXACT") {
            block.addStatement(
                "\$N = \$L.asType(\$L)",
                member.id,
                rawHandle,
                methodTypeExpression(
                    returnType = member.returnType,
                    parameterTypes = member.wrapperParameters.map { it.type } + member.constants.map { it.type },
                ),
            )
        } else {
            block.addStatement("\$N = \$L", member.id, rawHandle)
        }
    }

    private fun emitFieldInitializer(
        block: CodeBlock.Builder,
        member: FieldMember,
        ownerVariable: String,
        lookupVariable: String,
    ) {
        val finderName = if (member.isStatic) "findStaticVarHandle" else "findVarHandle"

        block.addStatement(
            "\$N = \$L.\$L(\$L, \$S, \$L)",
            member.id,
            lookupVariable,
            finderName,
            ownerVariable,
            member.fieldName,
            member.fieldType.classExpression(),
        )
    }

    private fun emitVarHandleInitializer(
        block: CodeBlock.Builder,
        index: Int,
        member: VarHandleMember,
        ownerVariable: String,
        lookupVariable: String,
    ) {
        val finderName = if (member.isStatic) "findStaticVarHandle" else "findVarHandle"

        if (member.invocation == "EXACT") {
            block.addStatement(
                "\$N = \$L.\$L(\$L, \$S, \$L)",
                member.id,
                lookupVariable,
                finderName,
                ownerVariable,
                member.fieldName,
                member.fieldType.classExpression(),
            )
            return
        }

        val rawVarHandleName = "rawVarHandle\$$index"

        block.addStatement(
            "\$T \$L = \$L.\$L(\$L, \$S, \$L)",
            VarHandle::class.java,
            rawVarHandleName,
            lookupVariable,
            finderName,
            ownerVariable,
            member.fieldName,
            member.fieldType.classExpression(),
        )
        block.addStatement(
            "\$N = \$L.toMethodHandle(\$T.AccessMode.\$L).asType(\$L)",
            member.id,
            rawVarHandleName,
            VarHandle::class.java,
            member.mode,
            methodTypeExpression(
                returnType = member.returnType,
                parameterTypes = member.wrapperParameters.map { it.type } + member.constants.map { it.type },
            ),
        )
    }

    private fun emitMethods(): List<MethodSpec> {
        val baseMethods = model.members.map { member ->
            MethodSpec.methodBuilder(member.wrapperName)
                .returns(member.returnType.asDeclarationType())
                .addModifiers(PUBLIC)
                .addParameters(
                    member.wrapperParameters.mapIndexed { index, parameter ->
                        ParameterSpec.builder(
                            parameter.type.asDeclarationType(
                                forceNonNull = member.isInstanceReceiverParameter(index),
                            ),
                            parameter.name,
                        )
                            .addModifiers(FINAL)
                            .build()
                    },
                )
                .beginControlFlow("try")
                .apply {
                    when (member) {
                        is MethodMember -> emitMethodBody(member)
                        is ConstructorMember -> emitConstructorBody(member)
                        is FieldMember -> emitFieldBody(member)
                        is VarHandleMember -> emitVarHandleBody(member)
                    }
                }
                .nextControlFlow("catch (\$T e)", Throwable::class.java)
                .invokeSneakyThrow(member.returnType)
                .endControlFlow()
                .build()
        }

        val instanceMethods = baseMethods.map { method ->
            method.toBuilder()
                .addAnnotation(Override::class.java)
                .build()
        }

        // Not possible — another method name would be required
//        val staticAccessors = baseMethods.map {method ->
//            method.toBuilder()
//                .addModifiers(STATIC)
//                .build()
//        }

        return instanceMethods
    }

    private fun MethodSpec.Builder.emitMethodBody(member: MethodMember) {
        val invocation = if (member.invocation == "INVOKE") "invoke" else "invokeExact"

        emitCall(
            returnType = member.returnType,
            invocation = CodeBlock.of("\$N.\$L", member.id, invocation),
            arguments = member.callArguments(),
        )
    }

    private fun MethodSpec.Builder.emitConstructorBody(member: ConstructorMember) {
        val invocation = if (member.invocation == "INVOKE") "invoke" else "invokeExact"

        emitCall(
            returnType = member.returnType,
            invocation = CodeBlock.of("\$N.\$L", member.id, invocation),
            arguments = member.callArguments(),
        )
    }

    private fun MethodSpec.Builder.emitFieldBody(member: FieldMember) {
        when (member.access) {
            "GET" -> emitCall(
                returnType = member.returnType,
                invocation = CodeBlock.of("\$N.get", member.id),
                arguments = member.callArguments(),
            )

            "SET" -> {
                require(member.returnType.isVoid) {
                    "Field SET wrapper '${member.wrapperName}' must return void / Unit"
                }

                addStatement("\$N.set(\$L)", member.id, member.callArguments().joinToCode())
            }

            else -> error("Unsupported field access mode '${member.access}' on '${member.wrapperName}'")
        }
    }

    private fun MethodSpec.Builder.emitVarHandleBody(member: VarHandleMember) {
        if (member.invocation == "EXACT") {
            emitCall(
                returnType = member.returnType,
                invocation = CodeBlock.of("\$N.\$L", member.id, member.mode.methodName()),
                arguments = member.callArguments(),
            )
            return
        }

        val invocation = if (member.invocation == "INVOKE") "invoke" else "invokeExact"

        emitCall(
            returnType = member.returnType,
            invocation = CodeBlock.of("\$N.\$L", member.id, invocation),
            arguments = member.callArguments(),
        )
    }

    private fun MethodSpec.Builder.emitCall(
        returnType: ReflectionSymbolProcessor.JavaType,
        invocation: CodeBlock,
        arguments: List<CodeBlock>,
    ) {
        val argumentCode = arguments.joinToCode()

        if (returnType.isVoid) {
            addStatement("\$L(\$L)", invocation, argumentCode)
            return
        }

        addStatement("return (\$T) \$L(\$L)", returnType.className, invocation, argumentCode)
    }

    private fun emitHelperMethods(): List<MethodSpec> {
        return listOf(
            emitClassForNameMethod(),
            emitClassFromDescriptorMethod(),
            emitSneakyThrowMethod(),
        )
    }

    private fun emitClassForNameMethod(): MethodSpec {
        val classWildcard = ParameterizedTypeName.get(
            ClassName.get(Class::class.java),
            WildcardTypeName.subtypeOf(Any::class.java),
        )

        return MethodSpec.methodBuilder("classForName")
            .addModifiers(PRIVATE, STATIC)
            .returns(classWildcard.annotated(nonNullAnnotationSpec))
            .addNullableParameter(ClassLoader::class.java, "classLoader")
            .addNonNullParameter(String::class.java, "name")
            .addException(ClassNotFoundException::class.java)
            .beginControlFlow("return switch (name)")
            .addStatement("case \$S -> boolean.class", "boolean")
            .addStatement("case \$S -> byte.class", "byte")
            .addStatement("case \$S -> short.class", "short")
            .addStatement("case \$S -> int.class", "int")
            .addStatement("case \$S -> long.class", "long")
            .addStatement("case \$S -> float.class", "float")
            .addStatement("case \$S -> double.class", "double")
            .addStatement("case \$S -> char.class", "char")
            .addStatement("case \$S -> void.class", "void")
            .addStatement("default -> \$T.forName(name, false, classLoader)", Class::class.java)
            .endControlFlow("")
            .build()
    }

    private fun emitClassFromDescriptorMethod(): MethodSpec {
        val classWildcard = ParameterizedTypeName.get(
            ClassName.get(Class::class.java),
            WildcardTypeName.subtypeOf(Any::class.java),
        )

        return MethodSpec.methodBuilder("classFromDescriptor")
            .addModifiers(PRIVATE, STATIC)
            .returns(classWildcard.annotated(nonNullAnnotationSpec))
            .addNullableParameter(ClassLoader::class.java, "classLoader")
            .addNonNullParameter(String::class.java, "descriptor")
            .addStatement(
                "return \$T.fromMethodDescriptorString(\$S + descriptor, classLoader).returnType()",
                MethodType::class.java,
                "()",
            )
            .build()
    }

    private fun emitSneakyThrowMethod(): MethodSpec {
        val throwableType = TypeVariableName.get("E", Throwable::class.java)
        val returnType = TypeVariableName.get("R")

        return MethodSpec.methodBuilder("sneakyThrow")
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings::class.java)
                    .addMember("value", "\$S", "unchecked")
                    .build()
            )
            .addModifiers(PRIVATE, STATIC)
            .addTypeVariable(throwableType)
            .addTypeVariable(returnType)
            .returns(returnType)
            .addParameter(Throwable::class.java, "throwable")
            .addException(throwableType)
            .addStatement("throw (E) throwable")
            .build()
    }

    private fun MethodSpec.Builder.invokeSneakyThrow(
        returnType: ReflectionSymbolProcessor.JavaType,
    ): MethodSpec.Builder = apply {
        if (returnType.isVoid) {
            addStatement("\$T.sneakyThrow(e)", modelClassName)
        } else {
            addStatement("return \$T.sneakyThrow(e)", modelClassName)
        }
    }

    private fun MethodSpec.Builder.addNullableParameter(
        type: Type,
        name: String,
        vararg modifier: Modifier
    ) = apply {
        addParameter(
            ParameterSpec.builder(type, name, *modifier)
                .addAnnotation(nullableAnnotationClassName)
                .build()
        )
    }

    private fun MethodSpec.Builder.addNonNullParameter(
        type: Type,
        name: String,
        vararg modifier: Modifier
    ) = apply {
        addParameter(
            ParameterSpec.builder(type, name, *modifier)
                .addAnnotation(nonNullAnnotationClassName)
                .build()
        )
    }

    private fun methodTypeExpression(
        returnType: ReflectionSymbolProcessor.JavaType,
        parameterTypes: List<ReflectionSymbolProcessor.JavaType>,
    ): CodeBlock {
        val block = CodeBlock.builder()
        block.add("\$T.methodType(\$T.class", MethodType::class.java, returnType.erasedClassName)

        for (parameterType in parameterTypes) {
            block.add(", \$T.class", parameterType.erasedClassName)
        }

        block.add(")")
        return block.build()
    }

    private fun ReflectionSymbolProcessor.JavaType.asDeclarationType(
        forceNonNull: Boolean = false,
    ): TypeName {
        if (isPrimitiveOrVoid) {
            return className
        }

        val effectiveNullability = if (forceNonNull) {
            JavaNullability.NON_NULL
        } else {
            nullability
        }

        return when (effectiveNullability) {
            JavaNullability.NON_NULL -> className.annotated(nonNullAnnotationSpec)
            JavaNullability.NULLABLE -> className.annotated(nullableAnnotationSpec)
            JavaNullability.UNKNOWN -> className
        }
    }

    private fun ReflectedMember.isInstanceReceiverParameter(index: Int): Boolean {
        return index == 0 && this is WithStaticModifier && !isStatic
    }

    private fun ReflectionSymbolProcessor.TypeRef.classExpression(): CodeBlock {
        return when (this) {
            is ReflectionSymbolProcessor.TypeRef.Known -> CodeBlock.of("\$T.class", type.erasedClassName)
            is ReflectionSymbolProcessor.TypeRef.Named -> CodeBlock.of("classForName(classLoader, \$S)", binaryName)
            is ReflectionSymbolProcessor.TypeRef.Descriptor -> CodeBlock.of(
                "classFromDescriptor(classLoader, \$S)",
                descriptor
            )
        }
    }

    private fun ReflectedMember.callArguments(): List<CodeBlock> {
        return wrapperParameters.map { CodeBlock.of("\$N", it.name) } +
                constants.map { CodeBlock.of("\$L", it.javaExpression) }
    }

    private fun List<CodeBlock>.joinToCode(separator: String = ", "): CodeBlock {
        val block = CodeBlock.builder()

        forEachIndexed { index, codeBlock ->
            if (index > 0) {
                block.add(separator)
            }

            block.add("\$L", codeBlock)
        }

        return block.build()
    }
}
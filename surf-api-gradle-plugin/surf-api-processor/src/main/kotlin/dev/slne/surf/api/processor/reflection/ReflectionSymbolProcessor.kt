package dev.slne.surf.api.processor.reflection

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.palantir.javapoet.ClassName
import com.palantir.javapoet.ParameterizedTypeName
import com.palantir.javapoet.TypeName
import com.palantir.javapoet.WildcardTypeName
import dev.slne.surf.api.processor.ClassNames
import dev.slne.surf.api.processor.ShortClassNames
import dev.slne.surf.api.processor.reflection.emitter.ReflectionJavaEmitter
import dev.slne.surf.api.processor.reflection.exception.ReflectionProcessorException
import dev.slne.surf.api.processor.reflection.model.*
import dev.slne.surf.api.processor.util.*
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.name.FqNameUnsafe
import java.lang.invoke.VarHandle
import java.util.*

class ReflectionSymbolProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private companion object {
        private val ignoredInterfaceMethods = setOf("equals", "hashCode", "toString")
    }

    private val codeGenerator = environment.codeGenerator
    private val logger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()

        resolver.getSymbolsWithAnnotation(ClassNames.GENERATE_REFLECTION)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { declaration ->
                if (!declaration.validate()) {
                    deferred += declaration
                    return@forEach
                }

                try {
                    processInterface(declaration)
                } catch (exception: ReflectionProcessorException) {
                    logger.error(exception.message ?: "Invalid reflection declaration", declaration)
                } catch (exception: Throwable) {
                    exception.printStackTrace()
                    logger.exception(exception)
                }
            }

        return deferred
    }

    private fun processInterface(declaration: KSClassDeclaration) {
        if (declaration.classKind != ClassKind.INTERFACE) {
            throw ReflectionProcessorException("@${ShortClassNames.GENERATE_REFLECTION} can only be used on interfaces")
        }

        val annotation = declaration.requireAnnotation(ClassNames.GENERATE_REFLECTION)
        val packageName = annotation.stringArg("packageName")
            .ifBlank { declaration.packageName.asString() }

        val className = annotation.stringArg("className")
            .ifBlank { "${declaration.simpleName.asString()}Impl" }

        val defaultTarget = annotation.targetRef()

        val singleton = annotation.booleanArg("singleton", true)

        val model = ReflectionModel(
            packageName = packageName,
            className = className,
            interfaceName = declaration.qualifiedName?.asString()
                ?: throw ReflectionProcessorException("Reflection interface must have a qualified name"),
            singleton = singleton,
            members = declaration.getDeclaredFunctions()
                .filterNot { it.simpleName.asString() in ignoredInterfaceMethods }
                .map { parseMember(defaultTarget, it) }
                .toList(),
            source = declaration,
        )

        generateJava(model)
    }

    private fun parseMember(defaultTarget: TypeRef?, function: KSFunctionDeclaration): ReflectedMember {
        if (Modifier.SUSPEND in function.modifiers) {
            throw ReflectionProcessorException("Suspend functions are not supported: ${function.simpleName.asString()}")
        }

        val annotations = listOfNotNull(
            function.findAnnotation(ClassNames.REFLECTED_METHOD),
            function.findAnnotation(ClassNames.REFLECTED_CONSTRUCTOR),
            function.findAnnotation(ClassNames.REFLECTED_FIELD),
            function.findAnnotation(ClassNames.REFLECTED_VAR_HANDLE),
        )

        if (annotations.size != 1) {
            throw ReflectionProcessorException(
                "Exactly one reflection member annotation is required on ${function.simpleName.asString()}"
            )
        }

        val wrapperParameters = function.parameters.mapIndexed { index, parameter ->
            val resolvedType = parameter.type.resolve()

            JavaParameter(
                name = parameter.name?.asString()?.sanitizeJavaIdentifier() ?: "p$index",
                type = resolvedType.toJavaType(),
                nullability = resolvedType.toJavaNullability(),
            )
        }

        val returnType = function.returnType?.resolve()?.toJavaType() ?: JavaType.Void
        val constants = function.constantArguments()

        function.findAnnotation(ClassNames.REFLECTED_METHOD)?.let { annotation ->
            val isStatic = annotation.booleanArg("isStatic", false)
            return ReflectedMember.MethodMember(
                id = "mh\$${function.simpleName.asString().sanitizeJavaIdentifier()}",
                wrapperName = function.simpleName.asString(),
                target = resolveTarget(
                    explicitTarget = annotation.targetRef(),
                    defaultTarget = defaultTarget,
                    function = function,
                    wrapperParameters = wrapperParameters,
                    returnType = returnType,
                    isStatic = isStatic,
                ),
                targetName = annotation.stringArg("name").ifBlank { function.simpleName.asString() },
                isStatic = isStatic,
                descriptor = annotation.stringArg("descriptor"),
                invocation = annotation.enumArg("invocation", "EXACT"),
                wrapperParameters = wrapperParameters,
                targetParameters = if (isStatic) wrapperParameters else wrapperParameters.drop(1),
                returnType = returnType,
                constants = constants,
            )
        }

        function.findAnnotation(ClassNames.REFLECTED_CONSTRUCTOR)?.let { annotation ->
            return ReflectedMember.ConstructorMember(
                id = "mh\$${function.simpleName.asString().sanitizeJavaIdentifier()}",
                wrapperName = function.simpleName.asString(),
                target = resolveTarget(
                    explicitTarget = annotation.targetRef(),
                    defaultTarget = defaultTarget,
                    function = function,
                    wrapperParameters = wrapperParameters,
                    returnType = returnType,
                    isStatic = false,
                    isConstructor = true,
                ),
                descriptor = annotation.stringArg("descriptor"),
                invocation = annotation.enumArg("invocation", "EXACT"),
                wrapperParameters = wrapperParameters,
                returnType = returnType,
                constants = constants,
            )
        }

        function.findAnnotation(ClassNames.REFLECTED_FIELD)?.let { annotation ->
            val isStatic = annotation.booleanArg("isStatic", false)
            val access = annotation.enumArg("access", "GET")
            return ReflectedMember.FieldMember(
                id = "vh\$${function.simpleName.asString().sanitizeJavaIdentifier()}",
                wrapperName = function.simpleName.asString(),
                target = resolveTarget(
                    explicitTarget = annotation.targetRef(),
                    defaultTarget = defaultTarget,
                    function = function,
                    wrapperParameters = wrapperParameters,
                    returnType = returnType,
                    isStatic = isStatic,
                ),
                fieldName = annotation.stringArg("name").ifBlank { function.simpleName.asString() },
                isStatic = isStatic,
                access = access,
                fieldType = annotation.fieldTypeRef()
                    ?: inferFieldType(access, isStatic, wrapperParameters, returnType),
                wrapperParameters = wrapperParameters,
                returnType = returnType,
                constants = constants,
            )
        }

        function.findAnnotation(ClassNames.REFLECTED_VAR_HANDLE)?.let { annotation ->
            val isStatic = annotation.booleanArg("isStatic", false)
            val modeString = annotation.enumArg("mode", VarHandle.AccessMode.GET.name)

            val mode = try {
                VarHandle.AccessMode.valueOf(modeString)
            } catch (_: IllegalArgumentException) {
                throw ReflectionProcessorException("Invalid VarHandle mode: $modeString")
            }

            return ReflectedMember.VarHandleMember(
                id = "vh\$${function.simpleName.asString().sanitizeJavaIdentifier()}",
                wrapperName = function.simpleName.asString(),
                target = resolveTarget(
                    explicitTarget = annotation.targetRef(),
                    defaultTarget = defaultTarget,
                    function = function,
                    wrapperParameters = wrapperParameters,
                    returnType = returnType,
                    isStatic = isStatic,
                ),
                fieldName = annotation.stringArg("name").ifBlank { function.simpleName.asString() },
                isStatic = isStatic,
                mode = mode,
                invocation = annotation.enumArg("invocation", "EXACT"),
                fieldType = annotation.fieldTypeRef()
                    ?: inferVarHandleFieldType(mode, isStatic, wrapperParameters, returnType, constants),
                wrapperParameters = wrapperParameters,
                returnType = returnType,
                constants = constants,
            )
        }

        throw ReflectionProcessorException("Unsupported reflected member: ${function.simpleName.asString()}")
    }

    private fun generateJava(model: ReflectionModel) {
        val code = ReflectionJavaEmitter(model).emit()

        codeGenerator.createNewFile(
            dependencies = Dependencies(
                aggregating = false,
                sources = listOfNotNull(model.source.containingFile).toTypedArray(),
            ),
            packageName = model.packageName,
            fileName = model.className,
            extensionName = "java",
        ).bufferedWriter().use { writer ->
            writer.write(code)
        }
    }

    private fun inferFieldType(
        access: String,
        isStatic: Boolean,
        parameters: List<JavaParameter>,
        returnType: JavaType,
    ): TypeRef {
        return when (access) {
            "GET" -> TypeRef.Known(returnType)
            "SET" -> {
                val valueIndex = if (isStatic) 0 else 1
                TypeRef.Known(parameters.getOrNull(valueIndex)?.type ?: JavaType.Object)
            }

            else -> throw ReflectionProcessorException("Unsupported field access mode: $access")
        }
    }

    private fun inferVarHandleFieldType(
        mode: VarHandle.AccessMode,
        isStatic: Boolean,
        parameters: List<JavaParameter>,
        returnType: JavaType,
        constants: List<ConstantArgument>,
    ): TypeRef {
        if (returnType != JavaType.Void && returnType != JavaType.Boolean) {
            return TypeRef.Known(returnType)
        }

        val receiverOffset = if (isStatic) 0 else 1
        val valueParameter = parameters.drop(receiverOffset).lastOrNull()?.type
        if (valueParameter != null) {
            return TypeRef.Known(valueParameter)
        }

        val constantType = constants.lastOrNull()?.type
        if (constantType != null) {
            return TypeRef.Known(constantType)
        }

        val getter = EnumSet.of(
            VarHandle.AccessMode.GET,
            VarHandle.AccessMode.GET_VOLATILE,
            VarHandle.AccessMode.GET_OPAQUE,
            VarHandle.AccessMode.GET_ACQUIRE
        )

        return when (mode) {
            in getter -> TypeRef.Known(returnType)
            else -> throw ReflectionProcessorException(
                "Unable to infer VarHandle field type for mode $mode. Specify type, typeName or typeDescriptor."
            )
        }
    }

    private fun resolveTarget(
        explicitTarget: TypeRef?,
        defaultTarget: TypeRef?,
        function: KSFunctionDeclaration,
        wrapperParameters: List<JavaParameter>,
        returnType: JavaType,
        isStatic: Boolean,
        isConstructor: Boolean = false,
    ): TypeRef {
        if (explicitTarget != null) return explicitTarget
        if (defaultTarget != null) return defaultTarget

        if (isConstructor) {
            if (returnType.isVoid || returnType == JavaType.Object) {
                throw ReflectionProcessorException(
                    "Target class not specified for constructor '${function.simpleName.asString()}': " +
                            "provide 'target'/'targetName' or use a concrete return type"
                )
            }

            return TypeRef.Known(returnType)
        }

        if (!isStatic) {
            val receiverType = wrapperParameters.firstOrNull()?.type
                ?: throw ReflectionProcessorException(
                    "Target class not specified for '${function.simpleName.asString()}': " +
                            "instance members need either a receiver parameter or an explicit target"
                )

            if (receiverType == JavaType.Object) {
                throw ReflectionProcessorException(
                    "Target class not specified for '${function.simpleName.asString()}': " +
                            "receiver type is Object/Any, so target cannot be inferred"
                )
            }

            return TypeRef.Known(receiverType)
        }

        throw ReflectionProcessorException(
            "Target class not specified for static member '${function.simpleName.asString()}': " +
                    "provide 'target'/'targetName' in the member annotation or @${ShortClassNames.GENERATE_REFLECTION}"
        )
    }

    private fun KSFunctionDeclaration.constantArguments(): List<ConstantArgument> {
        val result = mutableListOf<ConstantArgument>()

        annotations
            .filter { it.shortName.asString() == ShortClassNames.CONSTANT_INT_ARGUMENT }
            .forEach { result += ConstantArgument.IntValue(it.intArg("value", 0)) }

        annotations
            .filter { it.shortName.asString() == ShortClassNames.CONSTANT_LONG_ARGUMENT }
            .forEach { result += ConstantArgument.LongValue(it.longArg("value", 0L)) }

        annotations
            .filter { it.shortName.asString() == ShortClassNames.CONSTANT_BOOLEAN_ARGUMENT }
            .forEach { result += ConstantArgument.BooleanValue(it.booleanArg("value", false)) }

        annotations
            .filter { it.shortName.asString() == ShortClassNames.CONSTANT_STRING_ARGUMENT }
            .forEach { result += ConstantArgument.StringValue(it.stringArg("value")) }

        return result
    }


    private fun KSAnnotated.findAnnotation(fqName: String): KSAnnotation? {
        return annotations.firstOrNull {
            val declaration = it.annotationType.resolve().declaration as? KSClassDeclaration
            declaration?.qualifiedName?.asString() == fqName
        }
    }

    private fun KSAnnotated.requireAnnotation(fqName: String): KSAnnotation {
        return findAnnotation(fqName) ?: throw ReflectionProcessorException("Missing annotation $fqName")
    }

    private fun KSAnnotation.targetRef(): TypeRef? {
        return typeArg("target")
            ?: stringArg("targetName").takeIf { it.isNotBlank() }?.let(TypeRef::Named)
    }

    private fun KSAnnotation.fieldTypeRef(): TypeRef? {
        return typeArg("type")
            ?: stringArg("typeName").takeIf { it.isNotBlank() }?.let(TypeRef::Named)
            ?: stringArg("typeDescriptor").takeIf { it.isNotBlank() }?.let(TypeRef::Descriptor)
    }

    private fun KSAnnotation.typeArg(name: String): TypeRef? {
        val type = getArgumentValueAs<KSType>(name) ?: return null
        val declaration = type.declaration as? KSClassDeclaration ?: return null
        val qualifiedName = declaration.qualifiedName?.asString() ?: return null

        if (qualifiedName == ClassNames.UNSPECIFIED_REFLECTION_TARGET) {
            return null
        }

        return TypeRef.Known(type.toJavaType())
    }

    private fun KSType.toJavaType(): JavaType {
        val declaration = declaration as? KSClassDeclaration ?: return JavaType.Object
        val qualifiedName = declaration.qualifiedName?.asString() ?: return JavaType.Object
        val javaNullability = toJavaNullability()
        val isNullable = javaNullability == JavaNullability.NULLABLE

        return when (qualifiedName) {
            "kotlin.Unit" -> JavaType.Void

            "kotlin.Boolean" -> if (isNullable) boxed("java.lang.Boolean", javaNullability) else JavaType.Boolean
            "kotlin.Byte" -> if (isNullable) boxed("java.lang.Byte", javaNullability) else JavaType.Byte
            "kotlin.Short" -> if (isNullable) boxed("java.lang.Short", javaNullability) else JavaType.Short
            "kotlin.Int" -> if (isNullable) boxed("java.lang.Integer", javaNullability) else JavaType.Int
            "kotlin.Long" -> if (isNullable) boxed("java.lang.Long", javaNullability) else JavaType.Long
            "kotlin.Float" -> if (isNullable) boxed("java.lang.Float", javaNullability) else JavaType.Float
            "kotlin.Double" -> if (isNullable) boxed("java.lang.Double", javaNullability) else JavaType.Double
            "kotlin.Char" -> if (isNullable) boxed("java.lang.Character", javaNullability) else JavaType.Char

            else -> {
                val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(FqNameUnsafe(qualifiedName))
                val javaFqName = javaClassId?.asSingleFqName()?.asString() ?: qualifiedName
                val erasedClassName = ClassName.bestGuess(javaFqName)

                val declarationTypeName = if (arguments.isEmpty()) {
                    erasedClassName
                } else {
                    ParameterizedTypeName.get(
                        erasedClassName,
                        *arguments.map { it.toJavaTypeArgumentName() }.toTypedArray(),
                    )
                }

                JavaType(
                    sourceName = erasedClassName.canonicalName(),
                    className = declarationTypeName,
                    erasedClassName = erasedClassName,
                    nullability = javaNullability,
                )
            }
        }
    }

    private fun boxed(
        qualifiedName: String,
        nullability: JavaNullability,
    ): JavaType {
        val className = ClassName.bestGuess(qualifiedName)

        return JavaType(
            sourceName = qualifiedName,
            className = className,
            erasedClassName = className,
            nullability = nullability,
        )
    }

    private fun KSTypeArgument.toJavaTypeArgumentName(): TypeName {
        val resolvedType = type?.resolve()

        if (variance == Variance.STAR || resolvedType == null) {
            return WildcardTypeName.subtypeOf(ClassName.OBJECT)
        }

        val typeName = resolvedType.toJavaType().className.box()

        return when (variance) {
            Variance.INVARIANT -> typeName
            Variance.COVARIANT -> WildcardTypeName.subtypeOf(typeName)
            Variance.CONTRAVARIANT -> WildcardTypeName.supertypeOf(typeName)
            Variance.STAR -> WildcardTypeName.subtypeOf(ClassName.OBJECT)
        }
    }

    private fun KSType.toJavaNullability(): JavaNullability {
        return when (nullability) {
            Nullability.NULLABLE -> JavaNullability.NULLABLE
            Nullability.NOT_NULL -> JavaNullability.NON_NULL
            Nullability.PLATFORM -> JavaNullability.UNKNOWN
        }
    }

    private fun String.sanitizeJavaIdentifier(): String {
        if (isEmpty()) return "_"

        val builder = StringBuilder(length)
        forEachIndexed { index, char ->
            val valid = if (index == 0) Character.isJavaIdentifierStart(char)
            else Character.isJavaIdentifierPart(char)

            builder.append(if (valid) char else '_')
        }

        return builder.toString()
    }

    sealed interface TypeRef {
        data class Known(val type: JavaType) : TypeRef
        data class Named(val binaryName: String) : TypeRef
        data class Descriptor(val descriptor: String) : TypeRef
    }

    data class JavaType(
        val sourceName: String,
        /**
         * Type used in Java declarations and casts.
         *
         * This may be parameterized, for example `java.util.List<ChannelFuture>`.
         */
        val className: TypeName = ClassName.bestGuess(sourceName),

        /**
         * Type used for `.class` literals and MethodType construction.
         *
         * This must always be erased, for example `java.util.List.class`, not
         * `java.util.List<ChannelFuture>.class`.
         */
        val erasedClassName: TypeName = className,

        val nullability: JavaNullability = JavaNullability.UNKNOWN,
    ) {
        val isVoid: Boolean
            get() = erasedClassName == TypeName.VOID

        val isPrimitiveOrVoid: Boolean
            get() = erasedClassName == TypeName.VOID ||
                    erasedClassName == TypeName.BOOLEAN ||
                    erasedClassName == TypeName.BYTE ||
                    erasedClassName == TypeName.SHORT ||
                    erasedClassName == TypeName.INT ||
                    erasedClassName == TypeName.LONG ||
                    erasedClassName == TypeName.FLOAT ||
                    erasedClassName == TypeName.DOUBLE ||
                    erasedClassName == TypeName.CHAR

        companion object {
            val Void = JavaType("void", TypeName.VOID, TypeName.VOID, JavaNullability.NON_NULL)
            val Object = JavaType("java.lang.Object", ClassName.OBJECT, ClassName.OBJECT)
            val Boolean = JavaType("boolean", TypeName.BOOLEAN, TypeName.BOOLEAN, JavaNullability.NON_NULL)
            val Byte = JavaType("byte", TypeName.BYTE, TypeName.BYTE, JavaNullability.NON_NULL)
            val Short = JavaType("short", TypeName.SHORT, TypeName.SHORT, JavaNullability.NON_NULL)
            val Int = JavaType("int", TypeName.INT, TypeName.INT, JavaNullability.NON_NULL)
            val Long = JavaType("long", TypeName.LONG, TypeName.LONG, JavaNullability.NON_NULL)
            val Float = JavaType("float", TypeName.FLOAT, TypeName.FLOAT, JavaNullability.NON_NULL)
            val Double = JavaType("double", TypeName.DOUBLE, TypeName.DOUBLE, JavaNullability.NON_NULL)
            val Char = JavaType("char", TypeName.CHAR, TypeName.CHAR, JavaNullability.NON_NULL)
            val String = JavaType(
                sourceName = "java.lang.String",
                className = ClassName.get("java.lang", "String"),
                erasedClassName = ClassName.get("java.lang", "String"),
            )
        }
    }
}

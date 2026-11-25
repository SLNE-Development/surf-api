package dev.slne.surf.api.reflection

import dev.slne.surf.api.common.invoker.InvokerMethods
import java.lang.constant.ClassDesc
import java.lang.invoke.*
import java.lang.reflect.*

/**
 * Binds the methods of the proxy interface [proxyType] to members of [target].
 *
 * Every handle [bind] returns has exactly the type of the proxy method it was bound for. A
 * [ReflectedField] method that returns a field handle interface binds to a constant handle
 * instance, defined through [definingLookup] in the package of [proxyType].
 */
internal class ProxyMemberBinder(
    private val target: Class<*>,
    private val proxyType: Class<*>,
    private val definingLookup: Lazy<MethodHandles.Lookup>,
) {

    /**
     * Returns the handle that implements [method].
     *
     * @param method an abstract method of the proxy interface
     * @return a handle of exactly [method]'s type
     * @throws ReflectionProxyException if [method] is declared wrongly, matches no member, or the
     * members of [target] name a class that cannot be loaded
     */
    fun bind(method: Method): MethodHandle {
        if (InvokerMethods.isSuspendFunction(method)) {
            fail("suspend functions are not supported")
        }

        val handle = try {
            unadaptedHandle(method)
        } catch (failure: ReflectiveOperationException) {
            fail("cannot access the member of ${target.name}: $failure", failure)
        } catch (failure: LinkageError) {
            fail("cannot resolve the members of ${target.name}: $failure", failure)
        }

        return try {
            handle.asType(MethodType.methodType(method.returnType, method.parameterTypes))
        } catch (failure: WrongMethodTypeException) {
            fail("the member's type ${handle.type()} does not fit the proxy method", failure)
        }
    }

    private fun unadaptedHandle(method: Method): MethodHandle {
        val reflectedMethod = annotationOf<ReflectedMethod>(method)
        val reflectedField = annotationOf<ReflectedField>(method)
        val reflectedConstructor = annotationOf<ReflectedConstructor>(method)
        val isStatic = annotationOf<ReflectedStatic>(method) != null

        val kinds = listOfNotNull(reflectedMethod, reflectedField, reflectedConstructor)

        if (kinds.size != 1) {
            fail(
                "annotate it with exactly one of @ReflectedMethod, @ReflectedField and " +
                    "@ReflectedConstructor",
            )
        }

        return when {
            reflectedMethod != null -> methodHandle(method, reflectedMethod, isStatic)
            reflectedField != null -> fieldBinding(method, reflectedField, isStatic)
            else -> {
                if (isStatic) {
                    fail("@ReflectedStatic does not apply to @ReflectedConstructor")
                }

                constructorHandle(method)
            }
        }
    }

    private fun methodHandle(
        method: Method,
        annotation: ReflectedMethod,
        isStatic: Boolean,
    ): MethodHandle {
        val name = annotation.name.ifEmpty { method.name }
        val parameterTypes = memberParameterTypes(method, isStatic)
        val candidates = hierarchy()
            .flatMap { it.declaredMethods.asSequence() }
            .filter { it.name == name && !it.isBridge }
            .filter { Modifier.isStatic(it.modifiers) == isStatic }
            .toList()

        val member = select(candidates, parameterTypes, "${describe(isStatic)}method $name")

        return privateLookup(member.declaringClass).unreflect(member)
    }

    private fun fieldBinding(
        method: Method,
        annotation: ReflectedField,
        isStatic: Boolean,
    ): MethodHandle {
        val handleKind = FieldHandleKind.of(method.returnType)
            ?: return fieldAccessor(method, annotation, isStatic)

        return fieldHandleConstant(method, annotation, handleKind, isStatic)
    }

    private fun fieldAccessor(
        method: Method,
        annotation: ReflectedField,
        isStatic: Boolean,
    ): MethodHandle {
        val name = annotation.name.ifEmpty { method.name }
        val receiverCount = if (isStatic) 0 else 1
        val valueCount = if (annotation.access == FieldAccess.SET) 1 else 0
        checkParameterCount(method, receiverCount + valueCount)

        val field = findField(name, isStatic)
        val lookup = privateLookup(field.declaringClass)

        return when (annotation.access) {
            FieldAccess.GET -> lookup.unreflectGetter(field)
            FieldAccess.SET -> {
                checkWritable(field)
                lookup.unreflectSetter(field)
            }
        }
    }

    private fun fieldHandleConstant(
        method: Method,
        annotation: ReflectedField,
        kind: FieldHandleKind,
        isStatic: Boolean,
    ): MethodHandle {
        val handleName = kind.handleType.simpleName
        if (annotation.access != FieldAccess.GET) {
            fail("access does not apply to field handles")
        }

        if (kind.isStatic != isStatic) {
            val fix = if (isStatic) "drop @ReflectedStatic" else "mark it @ReflectedStatic"
            fail("$handleName binds ${describeKind(kind.isStatic)} field; $fix")
        }

        checkParameterCount(method, 0)

        val explicitName = annotation.name
        val names = if (explicitName.isEmpty()) propertyNames(method) else listOf(explicitName)
        val field = findField(names, isStatic)
        if (!kind.accepts(field.type)) {
            val expected = kind.describeFieldType()
            fail("field ${field.name} is ${field.type.name}; $handleName needs $expected")
        }

        checkWritable(field)
        checkTypeArguments(method, kind, field)

        val varHandle = privateLookup(field.declaringClass).unreflectVarHandle(field)
        val unsupported = FieldHandleClasses.accessModes(kind)
            .filterNot { varHandle.isAccessModeSupported(it) }

        if (unsupported.isNotEmpty()) {
            fail("field ${field.name} does not support the access modes $unsupported")
        }

        return MethodHandles.constant(kind.handleType, defineFieldHandle(kind, field, varHandle))
    }

    private fun checkTypeArguments(method: Method, kind: FieldHandleKind, field: Field) {
        val arguments = (method.genericReturnType as? ParameterizedType)?.actualTypeArguments
            ?: return

        val receiverType = if (kind.isStatic) null else rawClassOf(arguments.first())
        if (receiverType != null && !isCompatible(receiverType, target)) {
            fail("receiver type ${receiverType.name} does not fit ${target.name}")
        }

        val valueType = if (kind.fieldType == null) rawClassOf(arguments.last()) else null
        if (valueType != null && !valueType.isAssignableFrom(field.type)) {
            fail("value type ${valueType.name} does not fit ${field.name} of ${field.type.name}")
        }
    }

    private fun defineFieldHandle(kind: FieldHandleKind, field: Field, varHandle: VarHandle): Any {
        val name = ClassDesc.of(proxyType.name + "$" + field.name + FIELD_HANDLE_SUFFIX)

        return try {
            FieldHandleClasses.instantiate(definingLookup.value, name, kind, varHandle)
        } catch (failure: IllegalStateException) {
            fail("cannot define the handle class: $failure", failure)
        }
    }

    private fun findField(name: String, isStatic: Boolean): Field {
        return findField(listOf(name), isStatic)
    }

    private fun findField(names: List<String>, isStatic: Boolean): Field {
        for (name in names) {
            val field = hierarchy()
                .mapNotNull { type -> type.declaredFields.firstOrNull { it.name == name } }
                .firstOrNull { Modifier.isStatic(it.modifiers) == isStatic }
            if (field != null) return field
        }

        fail("${target.name} has no ${describe(isStatic)}field ${names.joinToString(" or ")}")
    }

    private fun constructorHandle(method: Method): MethodHandle {
        if (Modifier.isAbstract(target.modifiers)) {
            fail("${target.name} cannot be instantiated")
        }

        val constructor = select(
            target.declaredConstructors.toList(),
            method.parameterTypes.toList(),
            "constructor",
        )

        return privateLookup(target).unreflectConstructor(constructor)
    }

    private fun memberParameterTypes(method: Method, isStatic: Boolean): List<Class<*>> {
        val parameterTypes = method.parameterTypes.toList()
        if (isStatic) return parameterTypes

        if (parameterTypes.isEmpty()) {
            fail("an instance member takes the receiver first; mark it @ReflectedStatic if static")
        }

        if (!isCompatible(parameterTypes.first(), target)) {
            fail("receiver type ${parameterTypes.first().name} does not fit ${target.name}")
        }

        return parameterTypes.drop(1)
    }

    private fun <E : Executable> select(
        candidates: List<E>,
        parameterTypes: List<Class<*>>,
        description: String,
    ): E {
        val sameCount = candidates
            .filter { it.parameterCount == parameterTypes.size }
            .distinctBy { it.parameterTypes.toList() }

        sameCount.firstOrNull { it.parameterTypes.toList() == parameterTypes }?.let { return it }

        val compatible = sameCount.filter { candidate ->
            candidate.parameterTypes.zip(parameterTypes).all { (member, proxy) ->
                isCompatible(proxy, member)
            }
        }

        return when (compatible.size) {
            1 -> compatible.single()
            0 -> fail(
                "${target.name} has no $description${parameterTypes.signature()}; " +
                    "candidates: ${candidates.map { it.parameterTypes.toList().signature() }}",
            )

            else -> fail(
                "$description${parameterTypes.signature()} is ambiguous in ${target.name}: " +
                    compatible.map { it.parameterTypes.toList().signature() },
            )
        }
    }

    private fun checkParameterCount(method: Method, expected: Int) {
        if (method.parameterCount != expected) {
            fail("expected $expected parameters, found ${method.parameterCount}")
        }
    }

    private fun checkWritable(field: Field) {
        if (Modifier.isFinal(field.modifiers)) {
            val name = "${field.declaringClass.name}.${field.name}"
            fail("$name is final; final writes are unsupported")
        }
    }

    private fun hierarchy(): Sequence<Class<*>> {
        val classes = generateSequence(target) { it.superclass }.toList()
        val interfaces = classes
            .flatMap { allInterfaces(it) }
            .distinct()

        return (classes + interfaces).asSequence()
    }

    private fun allInterfaces(type: Class<*>): List<Class<*>> {
        return type.interfaces.flatMap { listOf(it) + allInterfaces(it) }
    }

    /**
     * Returns a private lookup in [type], or surf-api's own lookup when [type]'s module does not
     * open its package, as for JDK classes; that lookup reaches public members only.
     */
    private fun privateLookup(type: Class<*>): MethodHandles.Lookup {
        return try {
            MethodHandles.privateLookupIn(type, ProxyLookups.LOOKUP)
        } catch (_: IllegalAccessException) {
            ProxyLookups.LOOKUP
        }
    }

    private fun isCompatible(proxy: Class<*>, member: Class<*>): Boolean {
        if (proxy.isPrimitive || member.isPrimitive) {
            return proxy.kotlin.javaObjectType == member.kotlin.javaObjectType
        }

        return proxy.isAssignableFrom(member) || member.isAssignableFrom(proxy)
    }

    private fun describe(isStatic: Boolean) = if (isStatic) "static " else ""

    private fun describeKind(isStatic: Boolean) = if (isStatic) "a static" else "an instance"

    private fun List<Class<*>>.signature() = joinToString(prefix = "(", postfix = ")") { it.name }

    private fun fail(message: String, cause: Throwable? = null): Nothing {
        throw ReflectionProxyException(message, cause)
    }

    private companion object {
        const val FIELD_HANDLE_SUFFIX = $$"$FieldHandle"
        const val PROPERTY_ANNOTATIONS_SUFFIX = $$"$annotations"
        const val DEFAULT_IMPLS = "DefaultImpls"

        /**
         * Returns the annotation [A] of the proxy method [method].
         *
         * Kotlin compiles an annotation on an interface property without a use-site target onto
         * a synthetic `<getter>$annotations` method instead of the getter: in the interface's
         * `DefaultImpls` class, or in the interface itself when it is compiled without
         * `DefaultImpls`. That method counts as well.
         */
        inline fun <reified A : Annotation> annotationOf(method: Method): A? {
            return method.getAnnotation(A::class.java)
                ?: propertyAnnotationHolder(method)?.getAnnotation(A::class.java)
        }

        fun propertyAnnotationHolder(getter: Method): Method? {
            if (getter.parameterCount != 0) return null

            val holderName = getter.name + PROPERTY_ANNOTATIONS_SUFFIX
            val owner = getter.declaringClass
            val candidates = owner.declaredClasses
                .filter { it.simpleName == DEFAULT_IMPLS }
                .plus(owner)

            return candidates
                .flatMap { it.declaredMethods.asList() }
                .firstOrNull { it.name == holderName && it.isSynthetic }
        }

        /**
         * Returns the property names [getter] can belong to, the one Kotlin's naming rule yields
         * first, or only its own name if it is no `get` accessor.
         *
         * Kotlin capitalises only the first character of a property name for its getter, so
         * `getXValue` belongs to `xValue`, and `getURL` to `uRL` or `URL`.
         */
        fun propertyNames(getter: Method): List<String> {
            val name = getter.name
            if (name.length <= 3 || !name.startsWith("get") || !name[3].isUpperCase()) {
                return listOf(name)
            }

            val property = name.substring(3)

            return listOf(property.replaceFirstChar { it.lowercaseChar() }, property)
        }

        /**
         * Returns the class [type] erases to, or `null` for a type variable or wildcard.
         */
        fun rawClassOf(type: Type): Class<*>? {
            return when (type) {
                is Class<*> -> type
                is ParameterizedType -> type.rawType as Class<*>
                is GenericArrayType -> rawClassOf(type.genericComponentType)?.arrayType()
                else -> null
            }
        }
    }
}

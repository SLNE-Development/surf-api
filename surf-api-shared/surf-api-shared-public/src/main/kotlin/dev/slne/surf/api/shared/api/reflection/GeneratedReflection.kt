package dev.slne.surf.api.shared.api.reflection

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.lang.invoke.VarHandle
import kotlin.reflect.KClass

/**
 * Internal sentinel type used as the default value for annotation members that accept a [KClass].
 *
 * Kotlin annotations cannot use `null` as a default value for `KClass` properties. This type is
 * therefore used to express "no class was specified" for properties such as
 * [GenerateReflection.target], [ReflectedMethod.target], [ReflectedField.type], and similar
 * members.
 *
 * This type is an implementation detail of the reflection generator API and should never be used
 * directly by user code.
 */
@InternalSurfApi
class UnspecifiedReflectionTarget private constructor()

/**
 * Marks an interface as a compile-time reflection proxy.
 *
 * The KSP processor generates a Java implementation class for every interface annotated with
 * [GenerateReflection]. The generated class implements the annotated interface and resolves all
 * configured reflected members once in a static initializer. Reflected methods are backed by
 * [java.lang.invoke.MethodHandle] fields, while reflected fields and VarHandle operations are
 * backed by [java.lang.invoke.VarHandle] or derived [java.lang.invoke.MethodHandle] fields.
 *
 * A minimal proxy looks like this:
 *
 * ```kotlin
 * @GenerateReflection
 * interface ExampleReflection {
 *     @ReflectedField("somePrivateField")
 *     fun getSomePrivateField(instance: ExampleTarget): String
 *
 *     companion object : ExampleReflection by generatedReflectionAccessor<ExampleReflection>()
 * }
 * ```
 *
 * The generated implementation can then be used through the companion object:
 *
 * ```kotlin
 * val value = ExampleReflection.getSomePrivateField(target)
 * ```
 *
 * The generator resolves the target class for each member in the following order:
 *
 * 1. The member annotation, for example [ReflectedField.target] or [ReflectedMethod.target].
 * 2. The default [target] or [targetName] configured on [GenerateReflection].
 * 3. For instance members, the first function parameter.
 * 4. For constructors, the function return type.
 *
 * Static members cannot be inferred from parameters because they do not have a receiver. They
 * therefore require either a default target on this annotation or an explicit target on the member
 * annotation.
 *
 * Prefer [target] when the reflected class is available on the compile classpath. Use [targetName]
 * when the reflected class should only be resolved by name at runtime.
 *
 * @property target Default target class for all reflected members in the interface. This is useful
 * when most or all members operate on the same class.
 * @property targetName Binary name of the default target class, for example
 * `"net.minecraft.server.SomeClass"`. Use this when the class is not available as a [KClass] during
 * compilation.
 * @property packageName Package of the generated implementation class. If blank, the interface
 * package is used.
 * @property className Simple name of the generated implementation class. If blank, the generated
 * name is `<InterfaceSimpleName>Impl`.
 * @property singleton Whether the generated implementation should expose a public static
 * `INSTANCE` field and use a private constructor. This should normally stay enabled because
 * reflection handles are immutable after initialization.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GenerateReflection(
    val target: KClass<*> = UnspecifiedReflectionTarget::class,
    val targetName: String = "",
    val packageName: String = "",
    val className: String = "",
    val singleton: Boolean = true,
)


/**
 * Controls how the generated Java implementation invokes a [java.lang.invoke.MethodHandle].
 *
 * The default mode is [EXACT], which emits `invokeExact(...)`. This is the fastest and strictest
 * mode, but it requires the generated Java callsite to match the handle type exactly.
 *
 * Use [AS_TYPE_EXACT] when the raw reflected member is resolved dynamically, for example through a
 * JVM descriptor or runtime class name, but the public proxy method still has a stable signature.
 * In that mode, the handle is adapted once in the generated static initializer and then invoked via
 * `invokeExact(...)`.
 *
 * Use [INVOKE] only as a fallback for intentionally dynamic cases where method-handle adaptation
 * should happen at the callsite.
 */
enum class ReflectionInvocationMode {
    /**
     * Emits a direct `invokeExact(...)` call.
     *
     * Use this when all involved types are available at compile time and the generated method
     * signature exactly matches the reflected member after JVM erasure.
     *
     * Example:
     *
     * ```kotlin
     * @ReflectedMethod("mask")
     * fun getMask(instance: FilterMask): BitSet
     * ```
     */
    EXACT,

    /**
     * Emits `asType(...)` once during static initialization and then calls `invokeExact(...)`.
     *
     * This is useful when the target class or method descriptor is only known as a string, but the
     * proxy API itself should still be strongly typed.
     *
     * Example:
     *
     * ```kotlin
     * @ReflectedMethod(
     *     name = "mask",
     *     targetName = "net.minecraft.network.chat.FilterMask",
     *     descriptor = "()Ljava/util/BitSet;",
     *     invocation = ReflectionInvocationMode.AS_TYPE_EXACT,
     * )
     * fun getMask(instance: Any): BitSet
     * ```
     */
    AS_TYPE_EXACT,

    /**
     * Emits plain `invoke(...)`.
     *
     * This mode allows the JVM to perform method-handle conversions at the callsite. It is more
     * permissive than [EXACT], but should only be used when strict callsite typing is not possible
     * or not desired.
     */
    INVOKE,
}

/**
 * Describes a reflected method.
 *
 * Use this annotation on an interface function to call a private, package-private, protected, or
 * otherwise inaccessible method through a generated [java.lang.invoke.MethodHandle].
 *
 * For instance methods, the first proxy function parameter is treated as the receiver:
 *
 * ```kotlin
 * @ReflectedMethod("mask")
 * fun getMask(instance: FilterMask): BitSet
 * ```
 *
 * This resolves a method similar to:
 *
 * ```java
 * BitSet FilterMask.mask()
 * ```
 *
 * For static methods, set [isStatic] to `true`. Static methods do not use a receiver parameter:
 *
 * ```kotlin
 * @ReflectedMethod(name = "create", isStatic = true, target = SomeFactory::class)
 * fun create(value: String): SomeObject
 * ```
 *
 * If [descriptor] is blank, the generator infers the JVM method type from the proxy function
 * signature. If [descriptor] is specified, the descriptor is used for lookup instead. This is useful
 * for overloaded methods or for classes that are only available by [targetName].
 *
 * Target resolution follows the common generator rules:
 *
 * 1. [target] or [targetName] on this annotation.
 * 2. [GenerateReflection.target] or [GenerateReflection.targetName].
 * 3. For instance methods, the first proxy parameter.
 *
 * @property name Name of the reflected method. If blank, the proxy function name is used.
 * @property isStatic Whether the reflected method is static.
 * @property target Compile-time target class override for this method.
 * @property targetName Runtime target class name override for this method.
 * @property descriptor JVM method descriptor used for lookup. If blank, the descriptor is inferred
 * from the proxy function signature.
 * @property invocation Method-handle invocation strategy used by the generated wrapper.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ReflectedMethod(
    val name: String = "",
    val isStatic: Boolean = false,
    val target: KClass<*> = UnspecifiedReflectionTarget::class,
    val targetName: String = "",
    val descriptor: String = "",
    val invocation: ReflectionInvocationMode = ReflectionInvocationMode.EXACT,
)

/**
 * Describes a reflected constructor.
 *
 * Use this annotation on an interface function to call a constructor through a generated
 * [java.lang.invoke.MethodHandle].
 *
 * Constructor proxy functions do not have a receiver parameter. The proxy function parameters are
 * treated as constructor arguments, and the return type is treated as the constructed type:
 *
 * ```kotlin
 * @ReflectedConstructor
 * fun createFilterMask(bitSet: BitSet): FilterMask
 * ```
 *
 * If [target] and [targetName] are not specified, the generator attempts to infer the target class
 * from the proxy function return type. This works for normal strongly typed constructors. If the
 * return type is `Any`, or if the target class is not available at compile time, specify [targetName]
 * and usually also [descriptor].
 *
 * For constructors, [descriptor] must be a JVM constructor descriptor whose return type is `V`,
 * for example:
 *
 * ```kotlin
 * @ReflectedConstructor(
 *     targetName = "net.minecraft.network.chat.FilterMask",
 *     descriptor = "(Ljava/util/BitSet;)V",
 *     invocation = ReflectionInvocationMode.AS_TYPE_EXACT,
 * )
 * fun createFilterMask(bitSet: BitSet): Any
 * ```
 *
 * @property target Compile-time constructor owner class.
 * @property targetName Runtime constructor owner class name.
 * @property descriptor JVM constructor descriptor. If blank, the descriptor is inferred from the
 * proxy function parameters.
 * @property invocation Method-handle invocation strategy used by the generated wrapper.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ReflectedConstructor(
    val target: KClass<*> = UnspecifiedReflectionTarget::class,
    val targetName: String = "",
    val descriptor: String = "",
    val invocation: ReflectionInvocationMode = ReflectionInvocationMode.EXACT,
)

/**
 * Defines the supported simple field operations for [ReflectedField].
 *
 * For atomic operations, volatile operations, compare-and-set, get-and-add, and related access
 * modes, use [ReflectedVarHandle] instead.
 */
enum class ReflectedFieldAccess {
    /**
     * Reads the field via [java.lang.invoke.VarHandle.get].
     *
     * Instance fields require the proxy function to declare the receiver as the first parameter.
     * Static fields do not require a receiver parameter.
     */
    GET,

    /**
     * Writes the field via [java.lang.invoke.VarHandle.set].
     *
     * The proxy function should return `Unit` and provide the new field value as its last argument.
     * Instance fields require the receiver as the first parameter; static fields do not.
     *
     * Final fields are intentionally not supported.
     */
    SET,
}

/**
 * Describes a reflected field get or set operation.
 *
 * The generated implementation resolves the field as a [java.lang.invoke.VarHandle] and then emits
 * a simple access operation such as `get(...)` or `set(...)`.
 *
 * Instance field getter example:
 *
 * ```kotlin
 * @ReflectedField("chatMessageChain")
 * fun getChatMessageChain(instance: ServerGamePacketListenerImpl): FutureChain
 * ```
 *
 * Instance field setter example:
 *
 * ```kotlin
 * @ReflectedField(name = "someField", access = ReflectedFieldAccess.SET)
 * fun setSomeField(instance: SomeTarget, value: String)
 * ```
 *
 * Static field getter example:
 *
 * ```kotlin
 * @ReflectedField(
 *     name = "PAPER_RAW",
 *     isStatic = true,
 *     target = ChatProcessor::class,
 *     type = ResourceKey::class,
 * )
 * fun getPaperRawChatTypeKey(): ResourceKey<ChatType>
 * ```
 *
 * If [type], [typeName], and [typeDescriptor] are blank, the generator infers the field type from
 * the proxy function:
 *
 * - For [ReflectedFieldAccess.GET], the return type is used.
 * - For [ReflectedFieldAccess.SET], the value parameter type is used.
 *
 * Specify [type], [typeName], or [typeDescriptor] when the proxy type is generic, erased, dynamic,
 * or otherwise not suitable for the actual field lookup. For example, a field declared as raw
 * `ResourceKey` can be exposed as `ResourceKey<ChatType>` while using `type = ResourceKey::class`
 * for lookup.
 *
 * Target resolution follows the common generator rules:
 *
 * 1. [target] or [targetName] on this annotation.
 * 2. [GenerateReflection.target] or [GenerateReflection.targetName].
 * 3. For instance fields, the first proxy parameter.
 *
 * Static fields cannot be inferred from a receiver and therefore need an explicit target unless the
 * interface has a default target.
 *
 * @property name Name of the reflected field. If blank, the proxy function name is used.
 * @property isStatic Whether the reflected field is static.
 * @property access Field operation emitted by the generated wrapper.
 * @property target Compile-time field owner class.
 * @property targetName Runtime field owner class name.
 * @property type Compile-time field type used for VarHandle lookup.
 * @property typeName Runtime field type name used for VarHandle lookup.
 * @property typeDescriptor JVM field descriptor used for VarHandle lookup.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ReflectedField(
    val name: String = "",
    val isStatic: Boolean = false,
    val access: ReflectedFieldAccess = ReflectedFieldAccess.GET,
    val target: KClass<*> = UnspecifiedReflectionTarget::class,
    val targetName: String = "",
    val type: KClass<*> = UnspecifiedReflectionTarget::class,
    val typeName: String = "",
    val typeDescriptor: String = "",
)

/**
 * Describes a VarHandle access-mode operation.
 *
 * Use this annotation when a simple [ReflectedField] get or set is not enough. This includes
 * volatile reads/writes, acquire/release operations, compare-and-set operations, compare-and-exchange
 * operations, atomic get-and-set, atomic get-and-add, and bitwise atomic operations.
 *
 * Example for an atomic increment:
 *
 * ```kotlin
 * @ReflectedVarHandle(
 *     name = "nextChatIndex",
 *     mode = VarHandle.AccessMode.GET_AND_ADD,
 * )
 * @ConstantIntArgument(1)
 * fun getAndIncreaseNextChatIndex(instance: ServerGamePacketListenerImpl): Int
 * ```
 *
 * This generates a wrapper equivalent to:
 *
 * ```java
 * return (int) nextChatIndex.getAndAdd(instance, 1);
 * ```
 *
 * For instance fields, the first proxy function parameter is treated as the receiver. For static
 * fields, set [isStatic] to `true` and omit the receiver parameter.
 *
 * If [invocation] is [ReflectionInvocationMode.EXACT], the generated Java code directly calls the
 * corresponding VarHandle access-mode method, for example `getAndAdd(...)` or
 * `compareAndSet(...)`.
 *
 * If [invocation] is [ReflectionInvocationMode.AS_TYPE_EXACT] or [ReflectionInvocationMode.INVOKE],
 * the generated implementation first converts the VarHandle access mode to a
 * [java.lang.invoke.MethodHandle] using `toMethodHandle(...)`. This is useful for more dynamic
 * signatures where the exact VarHandle callsite cannot be emitted directly.
 *
 * If [type], [typeName], and [typeDescriptor] are blank, the generator tries to infer the field type
 * from the proxy signature or constant arguments. Specify an explicit field type when inference
 * would be ambiguous, especially for `compareAndSet`, erased generic fields, or dynamically loaded
 * target classes.
 *
 * Target resolution follows the common generator rules:
 *
 * 1. [target] or [targetName] on this annotation.
 * 2. [GenerateReflection.target] or [GenerateReflection.targetName].
 * 3. For instance fields, the first proxy parameter.
 *
 * @property name Name of the reflected field. If blank, the proxy function name is used.
 * @property isStatic Whether the reflected field is static.
 * @property mode [VarHandle.AccessMode] to invoke.
 * @property target Compile-time field owner class.
 * @property targetName Runtime field owner class name.
 * @property type Compile-time field type used for VarHandle lookup.
 * @property typeName Runtime field type name used for VarHandle lookup.
 * @property typeDescriptor JVM field descriptor used for VarHandle lookup.
 * @property invocation Invocation strategy. [ReflectionInvocationMode.EXACT] emits direct VarHandle
 * access-mode calls. Other modes convert the access mode to a MethodHandle first.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ReflectedVarHandle(
    val name: String = "",
    val isStatic: Boolean = false,
    val mode: VarHandle.AccessMode,
    val target: KClass<*> = UnspecifiedReflectionTarget::class,
    val targetName: String = "",
    val type: KClass<*> = UnspecifiedReflectionTarget::class,
    val typeName: String = "",
    val typeDescriptor: String = "",
    val invocation: ReflectionInvocationMode = ReflectionInvocationMode.EXACT,
)

/**
 * Appends a constant `int` argument to the generated reflective call.
 *
 * Constant arguments are appended after all proxy function parameters. They are useful when the
 * public proxy API should hide a fixed argument that is always passed to the reflected member.
 *
 * Example:
 *
 * ```kotlin
 * @ReflectedVarHandle(
 *     name = "nextChatIndex",
 *     mode = VarHandle.AccessMode.GET_AND_ADD,
 * )
 * @ConstantIntArgument(1)
 * fun getAndIncreaseNextChatIndex(instance: ServerGamePacketListenerImpl): Int
 * ```
 *
 * The generated call receives both the receiver and the constant:
 *
 * ```java
 * nextChatIndex.getAndAdd(instance, 1)
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class ConstantIntArgument(val value: Int)

/**
 * Appends a constant `long` argument to the generated reflective call.
 *
 * Constant arguments are appended after all proxy function parameters, in annotation processing
 * order. Use this for fixed numeric arguments that should not be exposed in the proxy method
 * signature.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class ConstantLongArgument(val value: Long)

/**
 * Appends a constant `boolean` argument to the generated reflective call.
 *
 * Constant arguments are appended after all proxy function parameters. This is useful for reflected
 * members that always need the same flag value but should expose a simpler proxy API.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class ConstantBooleanArgument(val value: Boolean)

/**
 * Appends a constant [String] argument to the generated reflective call.
 *
 * Constant arguments are appended after all proxy function parameters. The generated Java source
 * emits the value as a normal escaped string literal.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class ConstantStringArgument(val value: String)
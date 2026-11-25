package dev.slne.surf.api.reflection

const val TARGET = "dev.slne.surf.api.reflection.fixture.ProxyTarget"

@ReflectionProxy(targetName = TARGET)
interface TargetProxy {

    @ReflectedConstructor
    fun create(name: String): Any

    @ReflectedConstructor
    fun create(name: String, fixed: Int): Any

    @ReflectedMethod
    fun greet(instance: Any, other: String): String

    @ReflectedMethod("greet")
    fun greetAsCharSequence(instance: Any, other: CharSequence): CharSequence

    @ReflectedMethod
    fun sum(instance: Any, a: Long, b: Int): Long

    @ReflectedStatic
    @ReflectedMethod
    fun twice(value: Int): Int

    @ReflectedMethod
    fun fail(instance: Any)

    @ReflectedMethod
    fun inherited(instance: Any): String

    @ReflectedMethod("overloaded")
    fun overloadedString(instance: Any, value: String): String

    @ReflectedField("name")
    fun getName(instance: Any): String

    @ReflectedField("name", FieldAccess.SET)
    fun setName(instance: Any, name: String)

    @ReflectedField
    fun fixed(instance: Any): Int

    @ReflectedField
    fun baseValue(instance: Any): String

    @ReflectedStatic
    @ReflectedField
    fun counter(): Int

    @ReflectedStatic
    @ReflectedField("counter", FieldAccess.SET)
    fun setCounter(value: Int)

    @ReflectedStatic
    @ReflectedField("CONSTANT")
    fun constant(): String

    fun describe(instance: Any): String = "${getName(instance)}!"
}

@ReflectionProxy(targetName = TARGET)
interface PropertyProxy {

    @get:ReflectedStatic
    @get:ReflectedField("CONSTANT")
    val constant: String

    companion object : PropertyProxy by reflectionProxy<PropertyProxy>()
}

@ReflectionProxy(StringBuilder::class)
interface StringBuilderProxy {

    @ReflectedConstructor
    fun create(initial: String): StringBuilder

    @ReflectedMethod
    fun append(instance: StringBuilder, value: String): StringBuilder
}

@ReflectionProxy(targetName = TARGET)
interface FinalWriteProxy {

    @ReflectedField("fixed", FieldAccess.SET)
    fun setFixed(instance: Any, value: Int)
}

@ReflectionProxy(targetName = TARGET)
interface StaticFinalWriteProxy {

    @ReflectedStatic
    @ReflectedField("CONSTANT", FieldAccess.SET)
    fun setConstant(value: String)
}

@ReflectionProxy(targetName = TARGET)
interface BrokenProxy {

    @ReflectedMethod
    fun missing(instance: Any)

    fun unannotated()

    @ReflectedMethod
    @ReflectedField
    fun twoKinds(instance: Any)

    @ReflectedMethod("overloaded")
    fun ambiguous(instance: Any, value: Any): String

    @ReflectedMethod("greet")
    fun wrongReceiver(instance: String, other: String): String

    @ReflectedField("name")
    fun noReceiver(): String

    @ReflectedMethod("greet")
    fun noMethodReceiver(): String

    @ReflectedStatic
    @ReflectedConstructor
    fun staticConstructor(name: String): Any

    @ReflectedMethod
    suspend fun greet(instance: Any, other: String): String
}

@ReflectionProxy(targetName = "dev.slne.surf.api.reflection.fixture.Missing")
interface MissingTargetProxy

@ReflectionProxy(target = StringBuilder::class, targetName = TARGET)
interface DoubleTargetProxy

interface UnannotatedProxy

@ReflectionProxy(targetName = TARGET)
abstract class ClassProxy

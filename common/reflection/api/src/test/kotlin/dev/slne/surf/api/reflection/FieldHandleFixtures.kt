package dev.slne.surf.api.reflection

import dev.slne.surf.api.reflection.fixture.FieldHandleTarget

@ReflectionProxy(FieldHandleTarget::class)
interface FieldHandleProxy {

    @ReflectedField
    val count: IntFieldHandle<FieldHandleTarget>

    @ReflectedField("total")
    val total: LongFieldHandle<FieldHandleTarget>

    @get:ReflectedField("flag")
    val flag: BooleanFieldHandle<FieldHandleTarget>

    @ReflectedField("label")
    val label: ObjectFieldHandle<FieldHandleTarget, String>

    @ReflectedField("hidden")
    val hidden: ObjectFieldHandle<Any, Any>

    @ReflectedField
    val xValue: IntFieldHandle<FieldHandleTarget>

    @ReflectedStatic
    @ReflectedField
    val staticCount: StaticIntFieldHandle

    @ReflectedStatic
    @ReflectedField("staticTotal")
    val staticTotal: StaticLongFieldHandle

    @ReflectedStatic
    @ReflectedField("staticFlag")
    val staticFlag: StaticBooleanFieldHandle

    @ReflectedStatic
    @ReflectedField("staticLabel")
    val staticLabel: StaticObjectFieldHandle<String>

    @ReflectedField("count")
    fun readCount(instance: FieldHandleTarget): Int
}

@ReflectionProxy(targetName = TARGET)
interface PackagePrivateFieldHandleProxy {

    @ReflectedField("name")
    val name: ObjectFieldHandle<Any, String>
}

@ReflectionProxy(FieldHandleTarget::class)
interface BrokenFieldHandleProxy {

    @ReflectedField("total")
    val longAsInt: IntFieldHandle<FieldHandleTarget>

    @ReflectedField("count")
    val intAsObject: ObjectFieldHandle<FieldHandleTarget, Any>

    @ReflectedField("label")
    val wrongValue: ObjectFieldHandle<FieldHandleTarget, Int>

    @ReflectedField("payload")
    val narrowValue: ObjectFieldHandle<FieldHandleTarget, String>

    @ReflectedField("count")
    val wrongReceiver: IntFieldHandle<String>

    @ReflectedField("fixed")
    val finalField: IntFieldHandle<FieldHandleTarget>

    @ReflectedStatic
    @ReflectedField("STATIC_FIXED")
    val staticFinalField: StaticIntFieldHandle

    @ReflectedStatic
    @ReflectedField("count")
    val instanceAsStatic: IntFieldHandle<FieldHandleTarget>

    @ReflectedField("staticCount")
    val staticAsInstance: StaticIntFieldHandle

    @ReflectedField("count", FieldAccess.SET)
    val withAccess: IntFieldHandle<FieldHandleTarget>
}

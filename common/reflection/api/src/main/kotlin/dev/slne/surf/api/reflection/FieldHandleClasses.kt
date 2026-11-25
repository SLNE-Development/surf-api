package dev.slne.surf.api.reflection

import java.lang.classfile.ClassFile
import java.lang.classfile.CodeBuilder
import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.DynamicConstantDesc
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.VarHandle
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Implements the field handle interfaces of [FieldHandleKind] with hidden classes over a constant
 * [VarHandle].
 *
 * Each abstract method of a handle interface is named after a [VarHandle.AccessMode] and calls
 * that access mode on the `VarHandle` loaded from the hidden class's class data. The call site
 * names the access mode's exact type, so the `VarHandle` links without adaptation, and the handle
 * is put into the class data with [VarHandle.withInvokeExactBehavior] to enforce that.
 *
 * A reference type that the defining lookup cannot name, such as a package-private target class,
 * is erased to `Object` at the call site instead. The `VarHandle` then keeps its default invoke
 * behaviour, which casts the reference without boxing.
 */
internal object FieldHandleClasses {

    private val CD_VAR_HANDLE = ClassDesc.of(VarHandle::class.java.name)

    /**
     * Returns the [VarHandle.AccessMode]s a handle of [kind] calls.
     */
    fun accessModes(kind: FieldHandleKind): List<VarHandle.AccessMode> {
        return operations(kind).map { VarHandle.AccessMode.valueFromMethodName(it.name) }
    }

    /**
     * Defines a hidden class implementing [kind]'s handle interface over [varHandle] and returns
     * a new instance of it.
     *
     * @param lookup a lookup with full privilege access in the package of [name]
     * @param name the binary name of the hidden class
     * @param kind the handle to implement
     * @param varHandle a handle supporting every access mode of [accessModes]
     * @return the handle instance
     * @throws IllegalAccessException if [lookup] lacks full privilege access
     */
    fun instantiate(
        lookup: MethodHandles.Lookup,
        name: ClassDesc,
        kind: FieldHandleKind,
        varHandle: VarHandle,
    ): Any {
        val isExact = varHandleTypes(varHandle).all { isNameable(lookup, it) }
        val callSiteHandle = if (isExact) varHandle.withInvokeExactBehavior() else varHandle
        val classBytes = generate(name, kind, varHandle) { type ->
            if (isNameable(lookup, type)) type else Any::class.java
        }

        val hidden = lookup.defineHiddenClassWithClassData(
            classBytes,
            listOf(callSiteHandle),
            true,
        )

        return hidden
            .findConstructor(hidden.lookupClass(), MethodType.methodType(Void.TYPE))
            .invoke()
    }

    private fun generate(
        name: ClassDesc,
        kind: FieldHandleKind,
        varHandle: VarHandle,
        callSiteType: (Class<*>) -> Class<*>,
    ): ByteArray {
        return ClassFile.of().build(name) { classBuilder ->
            // final synthetic class <name> implements <handleType>
            classBuilder.withFlags(ClassFile.ACC_FINAL or ClassFile.ACC_SYNTHETIC)
            classBuilder.withInterfaceSymbols(describe(kind.handleType))

            // public <init>() { super(); }
            classBuilder.withMethodBody(
                ConstantDescs.INIT_NAME,
                ConstantDescs.MTD_void,
                ClassFile.ACC_PUBLIC,
            ) { codeBuilder ->
                codeBuilder.aload(0)
                    .invokespecial(
                        ConstantDescs.CD_Object,
                        ConstantDescs.INIT_NAME,
                        ConstantDescs.MTD_void,
                    )
                    .return_()
            }

            for (operation in operations(kind)) {
                val accessMode = VarHandle.AccessMode.valueFromMethodName(operation.name)
                val accessType = describe(varHandle.accessModeType(accessMode), callSiteType)

                // public final R <mode>(P...) { return classDataAt(0).<mode>(P...); }
                classBuilder.withMethodBody(
                    operation.name,
                    describe(operation),
                    ClassFile.ACC_PUBLIC or ClassFile.ACC_FINAL,
                ) { codeBuilder ->
                    codeBuilder.callVarHandle(operation, accessMode, accessType)
                }
            }
        }
    }

    private fun CodeBuilder.callVarHandle(
        operation: Method,
        accessMode: VarHandle.AccessMode,
        accessType: MethodTypeDesc,
    ) {
        ldc(varHandleConstant())

        var slot = 1

        for ((index, parameter) in operation.parameterTypes.withIndex()) {
            val kind = TypeKind.from(parameter)
            loadLocal(kind, slot)
            slot += kind.slotSize()

            val accessParameter = accessType.parameterType(index)
            if (kind == TypeKind.REFERENCE && accessParameter != ConstantDescs.CD_Object) {
                checkcast(accessParameter)
            }
        }

        invokevirtual(CD_VAR_HANDLE, accessMode.methodName(), accessType)
        return_(TypeKind.from(operation.returnType))
    }

    private fun operations(kind: FieldHandleKind): List<Method> {
        return kind.handleType.methods
            .filter { Modifier.isAbstract(it.modifiers) }
            .sortedBy { it.name }
    }

    private fun varHandleTypes(varHandle: VarHandle): List<Class<*>> {
        return varHandle.coordinateTypes() + varHandle.varType()
    }

    /**
     * Returns whether code defined by [lookup] can name [type] in a descriptor.
     */
    private fun isNameable(lookup: MethodHandles.Lookup, type: Class<*>): Boolean {
        if (type.isPrimitive) return true

        return try {
            lookup.accessClass(type)
            Class.forName(type.name, false, lookup.lookupClass().classLoader) === type
        } catch (_: ReflectiveOperationException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    private fun describe(type: MethodType, callSiteType: (Class<*>) -> Class<*>): MethodTypeDesc {
        return MethodTypeDesc.of(
            describe(callSiteType(type.returnType())),
            *type.parameterArray().map { describe(callSiteType(it)) }.toTypedArray(),
        )
    }

    private fun describe(method: Method): MethodTypeDesc {
        return MethodTypeDesc.of(
            describe(method.returnType),
            *method.parameterTypes.map { describe(it) }.toTypedArray(),
        )
    }

    private fun describe(type: Class<*>): ClassDesc = type.describeConstable().orElseThrow()

    // MethodHandles.classDataAt(lookup, "_", VarHandle.class, 0)
    private fun varHandleConstant(): DynamicConstantDesc<VarHandle> {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        return DynamicConstantDesc.ofNamed(
            ConstantDescs.BSM_CLASS_DATA_AT,
            ConstantDescs.DEFAULT_NAME,
            CD_VAR_HANDLE,
            0 as Integer,
        )
    }
}

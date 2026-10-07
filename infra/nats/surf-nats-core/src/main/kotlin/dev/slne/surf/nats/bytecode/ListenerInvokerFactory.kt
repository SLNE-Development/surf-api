package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import org.jetbrains.annotations.TestOnly
import java.lang.classfile.ClassFile
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.DynamicConstantDesc
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation

@InternalNatsApi
object ListenerInvokerFactory {

    /**
     * @see ListenerInvoker
     */
    private val INVOKER_TYPE = MethodType.methodType(
        Any::class.java,
        Any::class.java,
        Continuation::class.java
    )

    private val LOOKUP = MethodHandles.lookup()

    private val CD_INVOKER_IMPL = ClassDesc.of(
        ListenerInvoker::class.java.packageName,
        "ListenerInvokerImpl"
    )
    private val CD_LISTENER_INVOKER = ClassDesc.of(ListenerInvoker::class.java.name)
    private val CD_CONTINUATION = ClassDesc.of(Continuation::class.java.name)

    // Object(Object, Continuation)
    private val MTD_INVOKE = MethodTypeDesc.of(
        ConstantDescs.CD_Object,
        ConstantDescs.CD_Object,
        CD_CONTINUATION,
    )

    // One time call of MethodHandles.classData(lookup, "_", MethodHandle.class)
    private val CLASS_DATA_HANDLE = DynamicConstantDesc.ofNamed<MethodHandle>(
        ConstantDescs.BSM_CLASS_DATA,
        ConstantDescs.DEFAULT_NAME,
        ConstantDescs.CD_MethodHandle,
    )

    private val invokerClassBytes by lazy { generateInvokerClass() }

    fun create(target: Any?, method: Method): ListenerInvoker {
        val handle = adaptHandle(target, method)
        val hidden = LOOKUP.defineHiddenClassWithClassData(invokerClassBytes, handle, true)
        return hidden
            .findConstructor(hidden.lookupClass(), MethodType.methodType(Void.TYPE))
            .invoke() as ListenerInvoker
    }

    /**
     * Turns [method] into a [MethodHandle] with the shape `(Object, Continuation) -> Object`
     */
    private fun adaptHandle(target: Any?, method: Method): MethodHandle {
        var handle = MethodHandles.privateLookupIn(method.declaringClass, LOOKUP)
            .unreflect(method)

        if (!Modifier.isStatic(method.modifiers)) {
            handle = handle.bindTo(requireNotNull(target) {
                "Listener instance required for non-static $method"
            })
        }

        // suspend methods are already of the shape Object(event, continuation)
        if (!isSuspend(method)) {
            // void(event) -> Object(event) (always returns Unit)
            handle = MethodHandles.filterReturnValue(
                handle,
                MethodHandles.constant(Any::class.java, Unit)
            )

            // Object(event) -> Object(event, continuation) (ignores continuation)
            handle = MethodHandles.dropArguments(handle, 1, Continuation::class.java)
        }

        return handle.asType(INVOKER_TYPE)
    }

    private fun isSuspend(method: Method): Boolean {
        val params = method.parameterTypes
        if (params.size == 0) {
            return false
        }

        val lastParam = params.last()
        return Continuation::class.java.isAssignableFrom(lastParam)
    }

    private fun generateInvokerClass(): ByteArray {
        return ClassFile.of().build(CD_INVOKER_IMPL) { classBuilder ->
            classBuilder.withFlags(ClassFile.ACC_FINAL or ClassFile.ACC_SYNTHETIC)
            classBuilder.withInterfaceSymbols(CD_LISTENER_INVOKER)

            // generate constructor: public ListenerInvokerImpl() { super(); }
            classBuilder.withMethodBody(
                ConstantDescs.INIT_NAME,
                ConstantDescs.MTD_void,
                ClassFile.ACC_PUBLIC
            ) { codeBuilder ->
                codeBuilder.aload(0)
                    .invokespecial(
                        ConstantDescs.CD_Object,
                        ConstantDescs.INIT_NAME,
                        ConstantDescs.MTD_void
                    )
                    .return_()
            }

            // generate invoke method: public Object invoke(Object event, Continuation c) { return handle.invokeExact(event, c); }
            classBuilder.withMethodBody(
                "invoke",
                MTD_INVOKE,
                ClassFile.ACC_PUBLIC
            ) { codeBuilder ->
                codeBuilder.ldc(CLASS_DATA_HANDLE) // Stack: handle
                    .aload(1) // Stack: handle, event
                    .aload(2) // Stack: handle, event, continuation
                    .invokevirtual(
                        ConstantDescs.CD_MethodHandle,
                        "invokeExact",
                        MTD_INVOKE
                    ) // Stack: result
                    .areturn()
            }
        }
    }

    @TestOnly
    internal fun createInvokerClass(): ByteArray {
        return generateInvokerClass()
    }
}
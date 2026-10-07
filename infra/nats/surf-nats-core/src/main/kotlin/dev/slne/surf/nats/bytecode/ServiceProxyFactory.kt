package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.rpc.MethodDescriptor
import dev.slne.surf.nats.rpc.RpcCaller
import dev.slne.surf.nats.rpc.ServiceDescriptor
import org.jetbrains.annotations.TestOnly
import java.lang.classfile.ClassBuilder
import java.lang.classfile.ClassFile
import java.lang.classfile.CodeBuilder
import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor
import kotlin.coroutines.Continuation

@InternalNatsApi
internal object ServiceProxyFactory {

    private val LOOKUP = MethodHandles.lookup()
    private val CD_RPC_CALLER = ClassDesc.of(RpcCaller::class.java.name)
    private val CD_CONTINUATION = ClassDesc.of(Continuation::class.java.name)

    // call(int, Object[], Continuation): Object
    private val MTD_CALL = MethodTypeDesc.of(
        ConstantDescs.CD_Object,
        ConstantDescs.CD_int,
        ConstantDescs.CD_Object.arrayType(),
        CD_CONTINUATION
    )

    private val MTD_INIT = MethodTypeDesc.of(ConstantDescs.CD_void, CD_RPC_CALLER)
    private const val CALLER_FIELD = "caller"

    private val serviceDescriptor = ThreadLocal<ServiceDescriptor>()
    private val constructors = object : ClassValue<Constructor<*>>() {
        private val defineLock = Any()

        override fun computeValue(type: Class<*>): Constructor<*> {
            val proxyClass = synchronized(defineLock) {
                val lookup = MethodHandles.privateLookupIn(type, LOOKUP)
                findProxyClass(lookup, type) ?: lookup.defineClass(generateProxyClass(serviceDescriptor.get()!!))
            }

            return proxyClass.getDeclaredConstructor(RpcCaller::class.java)
        }

        private fun findProxyClass(lookup: MethodHandles.Lookup, type: Class<*>): Class<*>? = try {
            lookup.findClass(type.name + $$"$NatsProxy")
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    fun <S : Any> create(descriptor: ServiceDescriptor, caller: RpcCaller): S {
        serviceDescriptor.set(descriptor)
        try {
            @Suppress("UNCHECKED_CAST")
            return constructors.get(descriptor.type).newInstance(caller) as S
        } finally {
            serviceDescriptor.remove()
        }
    }

    private fun generateProxyClass(descriptor: ServiceDescriptor): ByteArray {
        val type = descriptor.type
        val thisClass = ClassDesc.of(type.name + $$"$NatsProxy")

        return ClassFile.of().build(thisClass) { classBuilder ->
            classBuilder.withFlags(ClassFile.ACC_PUBLIC or ClassFile.ACC_FINAL or ClassFile.ACC_SYNTHETIC)
            classBuilder.withInterfaceSymbols(ClassDesc.of(type.name))

            // private final RpcCaller caller;
            classBuilder.withField(
                CALLER_FIELD,
                CD_RPC_CALLER,
                ClassFile.ACC_PRIVATE or ClassFile.ACC_FINAL
            )

            // public <init>(RpcCaller caller) {
            //   super();
            //   this.caller = caller;
            // }
            classBuilder.withMethodBody(
                ConstantDescs.INIT_NAME,
                MTD_INIT,
                ClassFile.ACC_PUBLIC
            ) { codeBuilder ->
                codeBuilder.aload(0)
                    .invokespecial(
                        ConstantDescs.CD_Object,
                        ConstantDescs.INIT_NAME,
                        ConstantDescs.MTD_void
                    )
                    .aload(0)
                    .aload(1)
                    .putfield(thisClass, CALLER_FIELD, CD_RPC_CALLER)
                    .return_()
            }

            for (method in descriptor.methods) {
                generateMethod(classBuilder, thisClass, method)
            }
        }
    }

    private fun generateMethod(
        classBuilder: ClassBuilder,
        thisClass: ClassDesc,
        descriptor: MethodDescriptor
    ) {
        val method = descriptor.method
        val parameterTypes = method.parameterTypes
        val argumentTypes = parameterTypes.dropLast(1) // Args without continuation
        val methodType = MethodTypeDesc.of(
            method.returnType.describeConstable().orElseThrow(),
            *parameterTypes.map { it.describeConstable().orElseThrow() }.toTypedArray()
        )

        // override interface method
        // return this.caller.call(index, args[], cont)
        classBuilder.withMethodBody(
            method.name,
            methodType,
            ClassFile.ACC_PUBLIC or ClassFile.ACC_FINAL
        ) { codeBuilder ->
            // Load RpcCaller from field
            codeBuilder.aload(0).getfield(thisClass, CALLER_FIELD, CD_RPC_CALLER)
            codeBuilder.loadConstant(descriptor.index) // index
            codeBuilder.loadConstant(argumentTypes.size)
                .anewarray(ConstantDescs.CD_Object) // Object[] args

            var slot = 1
            argumentTypes.forEachIndexed { i, parameterType ->
                val kind = TypeKind.from(parameterType)
                codeBuilder.dup()
                    .loadConstant(i)
                    .loadLocal(kind, slot)
                box(codeBuilder, parameterType)
                codeBuilder.aastore()
                slot += kind.slotSize()
            }

            codeBuilder.aload(slot)
                .invokeinterface(CD_RPC_CALLER, "call", MTD_CALL)
                .areturn()
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun box(codeBuilder: CodeBuilder, type: Class<*>) {
        if (!type.isPrimitive) return
        val wrapper = when (type) {
            Int::class.javaPrimitiveType -> Integer::class.java
            Long::class.javaPrimitiveType -> java.lang.Long::class.java
            Double::class.javaPrimitiveType -> java.lang.Double::class.java
            Float::class.javaPrimitiveType -> java.lang.Float::class.java
            Boolean::class.javaPrimitiveType -> java.lang.Boolean::class.java
            Byte::class.javaPrimitiveType -> java.lang.Byte::class.java
            Short::class.javaPrimitiveType -> java.lang.Short::class.java
            Char::class.javaPrimitiveType -> Character::class.java
            else -> error("Unexpected primitive $type")
        }

        val wrapperDesc = ClassDesc.of(wrapper.name)
        codeBuilder.invokestatic(
            wrapperDesc,
            "valueOf",
            MethodTypeDesc.of(wrapperDesc, type.describeConstable().orElseThrow())
        )
    }

    @TestOnly
    internal fun createProxyClass(descriptor: ServiceDescriptor): ByteArray {
        return generateProxyClass(descriptor)
    }
}

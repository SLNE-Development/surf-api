package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.InternalNatsApi
import java.lang.classfile.ClassFile
import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.DynamicConstantDesc
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.MethodHandle

@InternalNatsApi
internal object HandleInvokerClasses {

    // One time call of MethodHandles.classData(lookup, "_", MethodHandle.class)
    private val CLASS_DATA_HANDLE = DynamicConstantDesc.ofNamed<MethodHandle>(
        ConstantDescs.BSM_CLASS_DATA,
        ConstantDescs.DEFAULT_NAME,
        ConstantDescs.CD_MethodHandle,
    )

    /**
     * Generates a class template which implements [interfaceType] by forwarding
     * [methodName] to the [MethodHandle].
     */
    fun generate(
        name: ClassDesc,
        interfaceType: ClassDesc,
        methodName: String,
        type: MethodTypeDesc
    ): ByteArray {
        return ClassFile.of().build(name) { classBuilder ->
            classBuilder.withFlags(ClassFile.ACC_FINAL or ClassFile.ACC_SYNTHETIC)
            classBuilder.withInterfaceSymbols(interfaceType)

            // generate constructor: public <init>() { super(); }
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

            // public R methodName(Param1 p1, Param2 p2, ..., ParamN pn) { return handle.invokeExact(p1, p2, ..., pn); }
            classBuilder.withMethodBody(
                methodName,
                type,
                ClassFile.ACC_PUBLIC or ClassFile.ACC_FINAL
            ) { codeBuilder ->
                codeBuilder.ldc(CLASS_DATA_HANDLE) // Stack: handle

                var slot = 1
                for (parameter in type.parameterList()) {
                    val kind = TypeKind.from(parameter)
                    codeBuilder.loadLocal(kind, slot) // Stack: handle, p1, p2, ..., pn
                    slot += kind.slotSize()
                }

                codeBuilder.invokevirtual(
                    ConstantDescs.CD_MethodHandle,
                    "invokeExact",
                    type
                ) // Stack: result
                codeBuilder.return_(TypeKind.from(type.returnType()))
            }
        }
    }
}
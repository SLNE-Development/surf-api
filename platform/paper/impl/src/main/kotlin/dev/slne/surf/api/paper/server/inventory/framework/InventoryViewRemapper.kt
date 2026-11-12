package dev.slne.surf.api.paper.server.inventory.framework

import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.field.FieldList
import net.bytebuddy.description.method.MethodList
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.jar.asm.ClassVisitor
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import net.bytebuddy.pool.TypePool
import net.bytebuddy.utility.OpenedClassReader
import org.bukkit.inventory.InventoryView

object InventoryViewRemapper {
    private const val INVENTORY_VIEW_INTERNAL = "org/bukkit/inventory/InventoryView"
    private const val INVENTORY_UPDATE_INTERNAL =
        "dev/slne/surf/api/libs/devnatan/inventoryframework/runtime/thirdparty/InventoryUpdate"

    fun isRemapNecessary(): Boolean {
        return try {
            InventoryView::class.java.isInterface
        } catch (_: Throwable) {
            false
        }
    }

    fun remap() {
        if (!isRemapNecessary()) return

        val locator = ClassFileLocator.ForClassLoader.of(javaClass.classLoader)
        val typePool = TypePool.Default.of(locator)
        val typeDescription =
            typePool.describe(INVENTORY_UPDATE_INTERNAL.replace("/", ".")).resolve()

        ByteBuddy()
            .redefine<Any>(typeDescription, locator)
            .visit(InventoryViewClassVisitorWrapper())
            .make()
            .load(javaClass.classLoader, ClassLoadingStrategy.Default.INJECTION)
    }

    private class InventoryViewClassVisitorWrapper : AsmVisitorWrapper {
        override fun mergeWriter(flags: Int): Int = flags
        override fun mergeReader(flags: Int): Int = flags

        override fun wrap(
            instrumentedType: TypeDescription,
            classVisitor: ClassVisitor,
            implementationContext: Implementation.Context,
            typePool: TypePool,
            fields: FieldList<FieldDescription.InDefinedShape?>,
            methods: MethodList<*>,
            writerFlags: Int,
            readerFlags: Int
        ): ClassVisitor {
            return InventoryViewClassVisitor(classVisitor)
        }
    }

    private class InventoryViewClassVisitor(
        visitor: ClassVisitor
    ) : ClassVisitor(OpenedClassReader.ASM_API, visitor) {

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor {
            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            return InventoryViewMethodVisitor(mv)
        }
    }

    private class InventoryViewMethodVisitor(
        visitor: MethodVisitor
    ) : MethodVisitor(OpenedClassReader.ASM_API, visitor) {

        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
            if (opcode == Opcodes.INVOKEVIRTUAL && owner == INVENTORY_VIEW_INTERNAL) {
                super.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    owner,
                    name,
                    descriptor,
                    true
                )
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            }
        }
    }
}
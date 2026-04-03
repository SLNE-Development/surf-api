package dev.slne.surf.surfapi.bukkit.server.inventory.framework

import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.field.FieldList
import net.bytebuddy.description.method.MethodList
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.jar.asm.*
import net.bytebuddy.pool.TypePool
import net.bytebuddy.utility.OpenedClassReader

/**
 * Remaps the `CloseContext` class from the inventory-framework to accept `Object` instead of
 * `InventoryCloseEvent` as the close origin parameter.
 *
 * This is necessary because `IFInventoryListener.onPlayerQuit()` calls
 * `ElementFactory.createCloseContext(viewer, context, event)` where `event` is a
 * `PlayerQuitEvent`, **not** an `InventoryCloseEvent`. The original `BukkitElementFactory`
 * casts `closeOrigin` to `InventoryCloseEvent`, and the `CloseContext` constructor also
 * expects `InventoryCloseEvent`, causing a `ClassCastException` at runtime.
 *
 * This remapper rewrites the bytecode of both `BukkitElementFactory` and `CloseContext` so that:
 * 1. `BukkitElementFactory.createCloseContext()` no longer casts the origin to `InventoryCloseEvent`.
 * 2. `CloseContext`'s constructor accepts `Object` instead of `InventoryCloseEvent`.
 * 3. `CloseContext`'s `closeOrigin` field is typed as `Object` instead of `InventoryCloseEvent`.
 */
object CloseContextRemapper {

    private const val INVENTORY_CLOSE_EVENT_INTERNAL = "org/bukkit/event/inventory/InventoryCloseEvent"
    private const val OBJECT_INTERNAL = "java/lang/Object"

    private const val CLOSE_CONTEXT_INTERNAL =
        "dev/slne/surf/surfapi/libs/devnatan/inventoryframework/context/CloseContext"
    private const val BUKKIT_ELEMENT_FACTORY_INTERNAL =
        "dev/slne/surf/surfapi/libs/devnatan/inventoryframework/internal/BukkitElementFactory"

    fun remap() {
        remapCloseContext()
        remapBukkitElementFactory()
    }

    /**
     * Remaps `CloseContext`:
     * - Field `closeOrigin`: `InventoryCloseEvent` → `Object`
     * - Constructor descriptor: `(…, InventoryCloseEvent)` → `(…, Object)`
     * - Method bodies: removes `CHECKCAST InventoryCloseEvent` instructions
     */
    private fun remapCloseContext() {
        val locator = ClassFileLocator.ForClassLoader.of(javaClass.classLoader)
        val typePool = TypePool.Default.of(locator)
        val typeDescription = typePool.describe(CLOSE_CONTEXT_INTERNAL.replace("/", ".")).resolve()

        ByteBuddy()
            .redefine<Any>(typeDescription, locator)
            .visit(CloseContextClassVisitorWrapper())
            .make()
            .load(javaClass.classLoader, ClassLoadingStrategy.Default.INJECTION)
    }

    private class CloseContextClassVisitorWrapper : AsmVisitorWrapper {
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
            return CloseContextClassVisitor(classVisitor)
        }
    }

    private class CloseContextClassVisitor(
        visitor: ClassVisitor
    ) : ClassVisitor(OpenedClassReader.ASM_API, visitor) {

        override fun visitField(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            value: Any?
        ): FieldVisitor? {
            // Change field type: InventoryCloseEvent -> Object
            val remappedDescriptor = remapDescriptor(descriptor)
            return super.visitField(access, name, remappedDescriptor, signature, value)
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor {
            // Change method descriptor: replace InventoryCloseEvent with Object
            val remappedDescriptor = remapDescriptor(descriptor)
            val mv = super.visitMethod(access, name, remappedDescriptor, signature, exceptions)
            return CloseContextMethodVisitor(mv)
        }
    }

    private class CloseContextMethodVisitor(
        visitor: MethodVisitor
    ) : MethodVisitor(OpenedClassReader.ASM_API, visitor) {

        override fun visitTypeInsn(opcode: Int, type: String) {
            // Remove CHECKCAST to InventoryCloseEvent
            if (opcode == Opcodes.CHECKCAST && type == INVENTORY_CLOSE_EVENT_INTERNAL) {
                return // skip the cast entirely
            }
            super.visitTypeInsn(opcode, type)
        }

        override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
            // Remap field access descriptors
            val remappedDescriptor = remapDescriptor(descriptor)
            super.visitFieldInsn(opcode, owner, name, remappedDescriptor)
        }

        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
            // Remap method invocation descriptors (e.g. constructor calls within CloseContext)
            val remappedDescriptor = remapDescriptor(descriptor)
            super.visitMethodInsn(opcode, owner, name, remappedDescriptor, isInterface)
        }
    }

    /**
     * Remaps `BukkitElementFactory.createCloseContext()`:
     * - Removes the `CHECKCAST InventoryCloseEvent` instruction
     * - Rewrites the `CloseContext.<init>` invocation descriptor to use `Object`
     */
    private fun remapBukkitElementFactory() {
        val locator = ClassFileLocator.ForClassLoader.of(javaClass.classLoader)
        val typePool = TypePool.Default.of(locator)
        val typeDescription = typePool.describe(BUKKIT_ELEMENT_FACTORY_INTERNAL.replace("/", ".")).resolve()

        ByteBuddy()
            .redefine<Any>(typeDescription, locator)
            .visit(BukkitElementFactoryClassVisitorWrapper())
            .make()
            .load(javaClass.classLoader, ClassLoadingStrategy.Default.INJECTION)
    }

    private class BukkitElementFactoryClassVisitorWrapper : AsmVisitorWrapper {
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
            return BukkitElementFactoryClassVisitor(classVisitor)
        }
    }

    private class BukkitElementFactoryClassVisitor(
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

            // Only remap the createCloseContext method
            if (name == "createCloseContext") {
                return BukkitElementFactoryMethodVisitor(mv)
            }

            return mv
        }
    }

    private class BukkitElementFactoryMethodVisitor(
        visitor: MethodVisitor
    ) : MethodVisitor(OpenedClassReader.ASM_API, visitor) {

        override fun visitTypeInsn(opcode: Int, type: String) {
            // Remove CHECKCAST to InventoryCloseEvent
            if (opcode == Opcodes.CHECKCAST && type == INVENTORY_CLOSE_EVENT_INTERNAL) {
                return // skip the cast entirely
            }
            super.visitTypeInsn(opcode, type)
        }

        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
            // Remap the CloseContext constructor invocation descriptor
            if (owner == CLOSE_CONTEXT_INTERNAL && name == "<init>") {
                val remappedDescriptor = remapDescriptor(descriptor)
                super.visitMethodInsn(opcode, owner, name, remappedDescriptor, isInterface)
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            }
        }
    }

    /**
     * Replaces all occurrences of `InventoryCloseEvent` with `Object` in a type descriptor.
     */
    private fun remapDescriptor(descriptor: String): String {
        val closeEventDescriptor = Type.getObjectType(INVENTORY_CLOSE_EVENT_INTERNAL).descriptor
        val objectDescriptor = Type.getObjectType(OBJECT_INTERNAL).descriptor
        return descriptor.replace(closeEventDescriptor, objectDescriptor)
    }
}
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
import org.bukkit.Bukkit

/**
 * Remaps inventory-framework's McVersion parser to support Paper's new version format:
 *
 *   26.1.2.build.23-alpha
 *
 * inventory-framework 3.7.1 detects whether a patch version exists via a private method named
 * `countColons`, which actually counts dots and expects exactly 3 dots for versions like:
 *
 *   1.21.11-R0.1-SNAPSHOT
 *
 * With the new Paper format, `26.1.2.build.23-alpha` contains 4 dots, so IF incorrectly parses it
 * as `26.1` instead of `26.1.2`.
 *
 * This remapper replaces only the private static `countColons(String): Int` method with a parser
 * that returns `3` whenever the Minecraft version part has a numeric patch component.
 */
object McVersionRemapper {

    private const val MC_VERSION_INTERNAL =
        "dev/slne/surf/api/libs/devnatan/inventoryframework/runtime/thirdparty/McVersion"

    private const val REMAPPER_INTERNAL =
        "dev/slne/surf/api/paper/server/inventory/framework/McVersionRemapper"

    fun isRemapNecessary(): Boolean {
        return try {
            val version = Bukkit.getBukkitVersion()
            version.contains(".build.") || hasNumericPatchVersionPart(version)
        } catch (_: Throwable) {
            false
        }
    }

    fun remap() {
        if (!isRemapNecessary()) return

        val locator = ClassFileLocator.ForClassLoader.of(javaClass.classLoader)
        val typePool = TypePool.Default.of(locator)
        val typeDescription = typePool.describe(MC_VERSION_INTERNAL.replace("/", ".")).resolve()

        ByteBuddy()
            .redefine<Any>(typeDescription, locator)
            .visit(McVersionClassVisitorWrapper())
            .make()
            .load(javaClass.classLoader, ClassLoadingStrategy.Default.INJECTION)
    }

    /**
     * Replacement for inventory-framework's private static `countColons(String): Int`.
     *
     * Important: The original code checks `countColons(version) == 3` before parsing
     * `version.split(".")[2]` as the patch number.
     *
     * Therefore this method deliberately returns:
     * - `3` if the Minecraft version part has a numeric patch, e.g. `1.21.11` or `26.1.2`
     * - `2` for patchless Paper build versions, e.g. `26.1.build.23-alpha`
     * - the original dot count for legacy/non-build versions
     */
    @JvmStatic
    @Suppress("unused")
    fun countVersionDotsForPatchDetection(version: String): Int {
        if (version.contains(".build.")) {
            val mcVersionPart = version.substringBefore(".build.")
            return if (hasNumericPatchVersionPart(mcVersionPart)) 3 else 2
        }

        return if (hasNumericPatchVersionPart(version)) {
            3
        } else {
            version.count { it == '.' }
        }
    }

    private fun hasNumericPatchVersionPart(version: String): Boolean {
        val mcVersionPart = version.substringBefore('-')
        val parts = mcVersionPart.split('.')

        return parts.size >= 3 &&
                parts[0].isNotEmpty() &&
                parts[1].isNotEmpty() &&
                parts[2].isNotEmpty() &&
                parts[0].all(Char::isDigit) &&
                parts[1].all(Char::isDigit) &&
                parts[2].all(Char::isDigit)
    }

    private class McVersionClassVisitorWrapper : AsmVisitorWrapper {
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
            return McVersionClassVisitor(classVisitor)
        }
    }

    private class McVersionClassVisitor(
        visitor: ClassVisitor
    ) : ClassVisitor(OpenedClassReader.ASM_API, visitor) {

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<String>?
        ): MethodVisitor? {
            if (name == "countColons" && descriptor == "(Ljava/lang/String;)I") {
                val mv = super.visitMethod(access, name, descriptor, signature, exceptions)

                mv.visitCode()
                mv.visitVarInsn(Opcodes.ALOAD, 0)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    REMAPPER_INTERNAL,
                    "countVersionDotsForPatchDetection",
                    "(Ljava/lang/String;)I",
                    false
                )
                mv.visitInsn(Opcodes.IRETURN)
                mv.visitMaxs(1, 1)
                mv.visitEnd()

                return null
            }

            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }
}
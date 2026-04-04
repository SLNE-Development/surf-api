package dev.slne.surf.redis

import dev.slne.surf.api.core.util.logger
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

object IoUringRedissonPatcher {
    private val log = logger()

    private const val OLD_PKG = "dev/slne/surf/redis/shaded/io/netty/incubator/channel/uring/"
    private const val NEW_PKG = "dev/slne/surf/redis/shaded/io/netty/channel/uring/"

    private val CLASS_MAP = mapOf(
        "${OLD_PKG}IOUringSocketChannel" to "${NEW_PKG}IoUringSocketChannel",
        "${OLD_PKG}IOUringDatagramChannel" to "${NEW_PKG}IoUringDatagramChannel",
        "${OLD_PKG}IOUringEventLoopGroup" to "${NEW_PKG}IoUringEventLoopGroup",
        "${OLD_PKG}IOUringChannelOption" to "${NEW_PKG}IoUringChannelOption",
    )

    private val TARGETS = listOf(
        "dev.slne.surf.redis.libs.redisson.connection.ServiceManager",
        "dev.slne.surf.redis.libs.redisson.client.RedisClient",
    )

    fun patch(classLoader: ClassLoader) {
        log.atInfo().log("Patching Redisson classes for Netty 4.2 io_uring compatibility...")

        for (targetName in TARGETS) {
            try {
                patchClass(targetName, classLoader)
                log.atInfo().log("Patched %s", targetName)
            } catch (e: Exception) {
                log.atSevere().withCause(e).log("Failed to patch %s", targetName)
                throw RuntimeException("Failed to patch $targetName for io_uring support", e)
            }
        }

        log.atInfo().log("Redisson io_uring patching complete.")
    }

    private fun patchClass(className: String, classLoader: ClassLoader) {
        val locator = ClassFileLocator.ForClassLoader.of(classLoader)
        val typePool = TypePool.Default.of(locator)
        val typeDescription = typePool.describe(className).resolve()

        ByteBuddy()
            .redefine<Any>(typeDescription, locator)
            .visit(IoUringRemappingVisitor())
            .make()
            .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
    }

    private class IoUringRemappingVisitor : AsmVisitorWrapper {
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
            return RemappingClassVisitor(classVisitor)
        }
    }

    private class RemappingClassVisitor(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM9, cv) {
        override fun visit(
            version: Int, access: Int, name: String?,
            signature: String?, superName: String?, interfaces: Array<out String>?
        ) {
            super.visit(
                version,
                access,
                name,
                remapDesc(signature),
                remapName(superName),
                interfaces
            )
        }

        override fun visitField(
            access: Int, name: String?, descriptor: String?,
            signature: String?, value: Any?
        ): FieldVisitor =
            super.visitField(access, name, remapDesc(descriptor), remapDesc(signature), value)

        override fun visitMethod(
            access: Int, name: String?, descriptor: String?,
            signature: String?, exceptions: Array<out String>?
        ): MethodVisitor {
            val mv = super.visitMethod(
                access,
                name,
                remapDesc(descriptor),
                remapDesc(signature),
                exceptions
            )
            return RemappingMethodVisitor(mv)
        }
    }

    private class RemappingMethodVisitor(mv: MethodVisitor) : MethodVisitor(Opcodes.ASM9, mv) {

        override fun visitTypeInsn(opcode: Int, type: String?) {
            super.visitTypeInsn(opcode, remapName(type))
        }

        override fun visitFieldInsn(
            opcode: Int,
            owner: String?,
            name: String?,
            descriptor: String?
        ) {
            super.visitFieldInsn(opcode, remapName(owner), name, remapDesc(descriptor))
        }

        override fun visitMethodInsn(
            opcode: Int, owner: String?, name: String?,
            descriptor: String?, isInterface: Boolean
        ) {
            super.visitMethodInsn(
                opcode,
                remapName(owner),
                name,
                remapDesc(descriptor),
                isInterface
            )
        }

        override fun visitLdcInsn(value: Any?) {
            if (value is Type) {
                val remapped = remapDesc(value.descriptor)
                if (remapped != value.descriptor) {
                    super.visitLdcInsn(Type.getType(remapped))
                    return
                }
            }
            super.visitLdcInsn(value)
        }

        override fun visitFrame(
            type: Int, numLocal: Int, local: Array<out Any>?,
            numStack: Int, stack: Array<out Any>?
        ) {
            val remappedLocal =
                local?.map { if (it is String) remapName(it) ?: it else it }?.toTypedArray()
            val remappedStack =
                stack?.map { if (it is String) remapName(it) ?: it else it }?.toTypedArray()
            super.visitFrame(type, numLocal, remappedLocal, numStack, remappedStack)
        }

        override fun visitLocalVariable(
            name: String?, descriptor: String?, signature: String?,
            start: Label?, end: Label?, index: Int
        ) {
            super.visitLocalVariable(
                name,
                remapDesc(descriptor),
                remapDesc(signature),
                start,
                end,
                index
            )
        }

        override fun visitTryCatchBlock(
            start: Label?,
            end: Label?,
            handler: Label?,
            type: String?
        ) {
            super.visitTryCatchBlock(start, end, handler, remapName(type))
        }

        override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
            super.visitMultiANewArrayInsn(remapDesc(descriptor), numDimensions)
        }
    }

    private fun remapName(name: String?): String? {
        if (name == null) return null
        return CLASS_MAP[name] ?: name
    }

    private fun remapDesc(desc: String?): String? {
        if (desc == null) return null
        var result: String = desc
        for ((old, new) in CLASS_MAP) {
            result = result.replace("L$old;", "L$new;")
            result = result.replace(old, new)
        }
        return result
    }
}
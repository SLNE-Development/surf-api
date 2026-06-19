package dev.slne.surf.api.gradle

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream

object SurfComponentBuilderBridgePatcher {
    private const val SURF_COMPONENT_BUILDER = "dev/slne/surf/api/core/messages/builder/SurfComponentBuilder"
    private const val SURF_COMPONENT_BUILDER_IMPL = "dev/slne/surf/api/core/messages/builder/SurfComponentBuilderImpl"

    private const val TEXT_COMPONENT_DESC = "()Lnet/kyori/adventure/text/TextComponent;"
    private const val COMPONENT_DESC = "()Lnet/kyori/adventure/text/Component;"

    fun patchJar(jar: Path) {
        require(Files.isRegularFile(jar)) {
            "Jar does not exist: $jar"
        }

        val tmp = Files.createTempFile(
            jar.parent,
            jar.fileName.toString(),
            ".surf-builder-bridge.tmp",
        )


        var changed = false

        JarInputStream(Files.newInputStream(jar)).use { input ->
            JarOutputStream(Files.newOutputStream(tmp)).use { output ->
                val written = hashSetOf<String>()

                while (true) {
                    val entry = input.nextJarEntry ?: break
                    val name = entry.name

                    if (!written.add(name)) {
                        continue
                    }

                    if (isSignatureEntry(name)) {
                        continue
                    }

                    val originalBytes = input.readBytes()

                    val patchedBytes = when (name) {
                        "$SURF_COMPONENT_BUILDER.class" -> patchSurfComponentBuilderInterface(originalBytes)
                        "$SURF_COMPONENT_BUILDER_IMPL.class" -> patchSurfComponentBuilderImpl(originalBytes)

                        else -> originalBytes
                    }

                    if (!patchedBytes.contentEquals(originalBytes)) {
                        changed = true
                    }

                    val newEntry = JarEntry(name)
                    newEntry.time = entry.time

                    output.putNextEntry(newEntry)
                    output.write(patchedBytes)
                    output.closeEntry()
                }
            }
        }

        if (changed) {
            Files.move(
                tmp,
                jar,
                StandardCopyOption.REPLACE_EXISTING,
            )

            println("[SurfComponentBuilderBridgePatcher] Patched $jar")
        } else {
            Files.deleteIfExists(tmp)

            println("[SurfComponentBuilderBridgePatcher] Nothing to patch in $jar")
        }
    }

    private fun patchSurfComponentBuilderInterface(bytes: ByteArray): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)

        var hasTextComponentBuild = false
        var hasComponentBuild = false

        val visitor = object : ClassVisitor(Opcodes.ASM9, writer) {
            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String,
                signature: String?,
                exceptions: Array<out String>?,
            ): MethodVisitor {
                if (name == "build" && descriptor == TEXT_COMPONENT_DESC) {
                    hasTextComponentBuild = true
                }

                if (name == "build" && descriptor == COMPONENT_DESC) {
                    hasComponentBuild = true
                }

                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }

            override fun visitEnd() {
                if (hasTextComponentBuild && !hasComponentBuild) {
                    addComponentBridge()
                }

                super.visitEnd()
            }

            private fun addComponentBridge() {
                val mv = super.visitMethod(
                    Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_BRIDGE,
                    "build",
                    COMPONENT_DESC,
                    null,
                    null,
                )

                mv.visitCode()
                mv.visitVarInsn(Opcodes.ALOAD, 0)
                mv.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    SURF_COMPONENT_BUILDER,
                    "build",
                    TEXT_COMPONENT_DESC,
                    true,
                )
                mv.visitInsn(Opcodes.ARETURN)
                mv.visitMaxs(0, 0)
                mv.visitEnd()
            }
        }

        reader.accept(visitor, 0)

        return writer.toByteArray()
    }

    private fun patchSurfComponentBuilderImpl(bytes: ByteArray): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)

        var className: String? = null
        var hasTextComponentBuild = false
        var hasComponentBuild = false

        val visitor = object : ClassVisitor(Opcodes.ASM9, writer) {
            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?,
            ) {
                className = name
                super.visit(version, access, name, signature, superName, interfaces)
            }

            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String,
                signature: String?,
                exceptions: Array<out String>?,
            ): MethodVisitor {
                if (name == "build" && descriptor == TEXT_COMPONENT_DESC) {
                    hasTextComponentBuild = true
                }

                if (name == "build" && descriptor == COMPONENT_DESC) {
                    hasComponentBuild = true
                }

                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }

            override fun visitEnd() {
                if (hasTextComponentBuild && !hasComponentBuild) {
                    addComponentBridge(className ?: SURF_COMPONENT_BUILDER_IMPL)
                }

                super.visitEnd()
            }

            private fun addComponentBridge(owner: String) {
                val mv = super.visitMethod(
                    Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC or Opcodes.ACC_BRIDGE,
                    "build",
                    COMPONENT_DESC,
                    null,
                    null,
                )

                mv.visitCode()
                mv.visitVarInsn(Opcodes.ALOAD, 0)
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    owner,
                    "build",
                    TEXT_COMPONENT_DESC,
                    false,
                )
                mv.visitInsn(Opcodes.ARETURN)
                mv.visitMaxs(0, 0)
                mv.visitEnd()
            }
        }

        reader.accept(visitor, 0)

        return writer.toByteArray()
    }

    private fun isSignatureEntry(name: String): Boolean {
        val upper = name.uppercase(Locale.ROOT)

        if (!upper.startsWith("META-INF/")) {
            return false
        }

        return upper.endsWith(".SF") ||
                upper.endsWith(".RSA") ||
                upper.endsWith(".DSA") ||
                upper.endsWith(".EC")
    }
}
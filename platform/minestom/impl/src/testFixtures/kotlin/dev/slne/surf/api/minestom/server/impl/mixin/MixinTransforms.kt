package dev.slne.surf.api.minestom.server.impl.mixin

import dev.slne.surf.api.minestom.server.testing.SurfMixinTestEnvironment
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

/**
 * Applies the installed mixins to class files without loading the result, to assert that they
 * still match their targets.
 */
object MixinTransforms {

    /**
     * Returns the class [binaryName] with the installed mixins applied to its shipped bytecode.
     *
     * Installs Mixin through [SurfMixinTestEnvironment] if needed. The result is not loaded.
     */
    fun transform(binaryName: String): ClassNode {
        SurfMixinTestEnvironment.install()
        val transformer = checkNotNull(InstrumentationMixinService.transformer()) { "Mixin never offered its transformer" }
        val transformed = transformer.transformClassBytes(binaryName, binaryName, originalBytes(binaryName))

        val node = ClassNode(Opcodes.ASM9)
        ClassReader(transformed).accept(node, 0)
        return node
    }

    /**
     * Returns the unmodified bytecode of [binaryName] from the class path.
     */
    fun originalBytes(binaryName: String): ByteArray {
        val resource = binaryName.replace('.', '/') + ".class"
        return checkNotNull(ClassLoader.getSystemClassLoader().getResourceAsStream(resource)) {
            "$binaryName is not on the class path"
        }.use { it.readBytes() }
    }
}

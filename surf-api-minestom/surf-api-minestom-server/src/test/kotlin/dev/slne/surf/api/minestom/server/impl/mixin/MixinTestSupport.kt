package dev.slne.surf.api.minestom.server.impl.mixin

import com.google.gson.JsonParser
import dev.slne.surf.api.minestom.server.testing.SurfMixinTestEnvironment
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.Mixin

object MixinTestSupport {

    /**
     * The mixin configurations applied in tests.
     */
    val CONFIGURATIONS: List<String> get() = SurfMixinTestEnvironment.configurations

    fun install() = SurfMixinTestEnvironment.install()

    fun transform(binaryName: String): ClassNode = MixinTransforms.transform(binaryName)

    fun originalBytes(binaryName: String): ByteArray = MixinTransforms.originalBytes(binaryName)

    /**
     * The binary names of the mixin classes listed in [configuration].
     */
    fun mixinsOf(configuration: String): List<String> {
        val json = checkNotNull(javaClass.classLoader.getResourceAsStream(configuration)) {
            "Missing $configuration"
        }.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }

        val mixinPackage = json.get("package").asString
        return listOf("mixins", "server", "client").flatMap { section ->
            json.getAsJsonArray(section)?.map { "$mixinPackage.${it.asString}" }.orEmpty()
        }
    }

    /**
     * The internal names of the interfaces [mixin] adds to its targets.
     */
    fun interfacesOf(mixin: String): List<String> {
        val node = ClassNode(Opcodes.ASM9)
        ClassReader(originalBytes(mixin)).accept(node, ClassReader.SKIP_CODE)
        return node.interfaces.orEmpty()
    }

    /**
     * The binary names of the classes [mixin] targets, read from its `@Mixin` annotation.
     */
    fun targetsOf(mixin: String): List<String> {
        val node = ClassNode(Opcodes.ASM9)
        ClassReader(originalBytes(mixin)).accept(
            node,
            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
        )

        val annotation = checkNotNull(
            (node.visibleAnnotations.orEmpty() + node.invisibleAnnotations.orEmpty())
                .singleOrNull { it.desc == Type.getDescriptor(Mixin::class.java) },
        ) { "$mixin has no @Mixin annotation" }

        return annotation.values.orEmpty().chunked(2).flatMap { (key, value) ->
            when (key) {
                "value" -> (value as List<*>).map { (it as Type).className }
                "targets" -> (value as List<*>).map { (it as String).replace('/', '.') }
                else -> emptyList()
            }
        }.distinct()
    }
}

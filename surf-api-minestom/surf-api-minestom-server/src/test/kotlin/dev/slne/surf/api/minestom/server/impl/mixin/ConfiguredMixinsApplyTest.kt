package dev.slne.surf.api.minestom.server.impl.mixin

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged

/**
 * Applies every configured mixin to the shipped bytecode of each of its targets.
 *
 * In production a failed transformation only loads the class untransformed, so a Minestom update
 * that moves an injection point has to fail here. Each mixin has to leave its own mark on the
 * target, so one mixin applying does not hide another on the same target failing.
 */
class ConfiguredMixinsApplyTest {

    @TestFactory
    fun `configured mixins apply to all declared targets`(): List<DynamicTest> {
        val mixins = MixinTestSupport.CONFIGURATIONS.flatMap(MixinTestSupport::mixinsOf)
        check(mixins.isNotEmpty()) { "No mixins configured" }

        return mixins.flatMap { mixin ->
            val targets = MixinTestSupport.targetsOf(mixin)
            check(targets.isNotEmpty()) { "$mixin declares no targets" }

            targets.map { target ->
                dynamicTest("${mixin.substringAfterLast('.')} -> $target") {
                    val transformed = MixinTestSupport.transform(target)
                    assertTrue(transformed.isMergedFrom(mixin)) { "$mixin left no trace in $target" }
                }
            }
        }
    }

    private fun ClassNode.isMergedFrom(mixin: String): Boolean {
        val interfaces = MixinTestSupport.interfacesOf(mixin)
        if (interfaces.isNotEmpty() && this.interfaces.containsAll(interfaces)) return true

        val annotations = methods.flatMap { it.invisibleAnnotations.orEmpty() + it.visibleAnnotations.orEmpty() } +
                fields.flatMap { it.invisibleAnnotations.orEmpty() + it.visibleAnnotations.orEmpty() }
        return annotations.any { it.desc == MERGED && it.value("mixin") == mixin }
    }

    private fun AnnotationNode.value(key: String): Any? =
        values.orEmpty().chunked(2).firstOrNull { (name, _) -> name == key }?.get(1)

    private companion object {
        val MERGED: String = Type.getDescriptor(MixinMerged::class.java)
    }
}

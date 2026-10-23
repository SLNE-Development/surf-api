package dev.slne.surf.nats.descriptor

import org.junit.jupiter.api.Test
import java.lang.classfile.Attributes
import java.lang.classfile.ClassFile
import java.lang.classfile.MethodModel
import java.lang.reflect.AccessFlag
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The entry points of generated code carry `@NatsGeneratedApi`, which ABI validation covers, and not `@InternalNatsApi`,
 * which it excludes. Both markers have binary retention, so the class files are read directly.
 */
class GeneratedApiMarkerTest {

    private fun methods(type: Class<*>): List<MethodModel> {
        val bytes = checkNotNull(type.getResourceAsStream("${type.simpleName}.class")).use { it.readBytes() }
        return ClassFile.of().parse(bytes).methods()
    }

    private fun markers(method: MethodModel): List<String> {
        return method.findAttribute(Attributes.runtimeInvisibleAnnotations())
            .map { attribute -> attribute.annotations().map { it.className().stringValue() } }
            .orElse(emptyList())
    }

    private fun assertGeneratedApi(methods: List<MethodModel>) {
        assertTrue(methods.isNotEmpty())
        for (method in methods) {
            assertEquals(listOf(GENERATED_API), markers(method).filter { it in setOf(GENERATED_API, INTERNAL_API) }, method.toString())
        }
    }

    @Test
    fun `descriptor constructors are generated API`() {
        val types = listOf(
            NatsEventDescriptor::class.java,
            NatsServiceDescriptor::class.java,
            NatsMethodDescriptor::class.java,
            NatsParameter::class.java,
        )
        for (type in types) {
            assertGeneratedApi(
                methods(type).filter { it.methodName().equalsString("<init>") && AccessFlag.SYNTHETIC !in it.flags().flags() }
            )
        }
    }

    @Test
    fun `byte array content helpers are generated API`() {
        val facade = Class.forName("dev.slne.surf.nats.descriptor.ByteArrayContentKt")
        val helpers = methods(facade).filter { method ->
            method.methodName().stringValue().startsWith("natsContent") && AccessFlag.PUBLIC in method.flags().flags()
        }
        assertEquals(9, helpers.size)
        assertGeneratedApi(helpers)
    }

    private companion object {
        const val GENERATED_API = "Ldev/slne/surf/nats/NatsGeneratedApi;"
        const val INTERNAL_API = "Ldev/slne/surf/nats/InternalNatsApi;"
    }
}

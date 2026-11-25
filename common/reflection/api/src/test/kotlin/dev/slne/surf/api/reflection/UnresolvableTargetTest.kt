package dev.slne.surf.api.reflection

import dev.slne.surf.api.reflection.fixture.Absent
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

private const val FIXTURES = "dev.slne.surf.api.reflection.fixture"
private const val ABSENT = "$FIXTURES.Absent"

@ReflectionProxy(targetName = "$FIXTURES.AbsentSignatureTarget")
interface AbsentSignatureProxy {

    @ReflectedMethod
    fun present(instance: Any)
}

@ReflectionProxy(targetName = "$FIXTURES.AbsentSuperclassTarget")
interface AbsentSuperclassProxy

@ReflectionProxy(Absent::class)
interface AbsentTypeProxy

/**
 * Checks that targets whose members or superclass name an unloadable class fail with a
 * [ReflectionProxyException], as targets with optional dependencies do.
 */
class UnresolvableTargetTest {

    @Test
    fun `a target whose member signature names a missing class fails`() {
        val proxyType = isolate(AbsentSignatureProxy::class.java, "AbsentSignatureTarget")

        val message = assertFailsWith<ReflectionProxyException> {
            ReflectionProxies.get(proxyType)
        }.message.orEmpty()

        assertContains(message, "present: cannot resolve the members of")
        assertContains(message, NoClassDefFoundError::class.java.name)
    }

    @Test
    fun `a target whose superclass is missing fails`() {
        val proxyType = isolate(AbsentSuperclassProxy::class.java, "AbsentSuperclassTarget")

        val failure = assertFailsWith<ReflectionProxyException> {
            ReflectionProxies.get(proxyType)
        }

        assertContains(failure.message.orEmpty(), "AbsentSuperclassTarget of")
        assertIs<NoClassDefFoundError>(failure.cause)
    }

    @Test
    fun `a target class literal that cannot be loaded fails`() {
        val proxyType = isolate(AbsentTypeProxy::class.java)

        val failure = assertFailsWith<ReflectionProxyException> {
            ReflectionProxies.get(proxyType)
        }

        assertContains(failure.message.orEmpty(), "Target class $ABSENT of")
        assertIs<TypeNotPresentException>(failure.cause)
    }

    private fun isolate(proxyType: Class<*>, targetSimpleName: String? = null): Class<*> {
        val isolated = setOfNotNull(proxyType.name, targetSimpleName?.let { "$FIXTURES.$it" })

        return HidingClassLoader(proxyType, isolated).loadClass(proxyType.name)
    }

    /**
     * Defines the [isolated] classes itself and cannot load [ABSENT], so the isolated target
     * resolves against a loader that lacks it.
     */
    private class HidingClassLoader(
        anchor: Class<*>,
        private val isolated: Set<String>,
    ) : ClassLoader(anchor.classLoader) {

        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name == ABSENT) throw ClassNotFoundException(name)
            if (name !in isolated) return super.loadClass(name, resolve)

            synchronized(getClassLoadingLock(name)) {
                findLoadedClass(name)?.let { return it }
                val resource = name.replace('.', '/') + ".class"
                val bytes = parent.getResourceAsStream(resource)!!.use { it.readBytes() }

                return defineClass(name, bytes, 0, bytes.size)
            }
        }
    }
}

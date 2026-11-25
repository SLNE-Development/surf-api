package dev.slne.surf.api.reflection

import kotlin.test.*

/**
 * Checks proxies of interfaces that live in another class loader's unnamed module, as a plugin's
 * interfaces do.
 */
class ForeignModuleProxyTest {

    @Test
    fun `an interface of another module gets a working hidden class in its own loader`() {
        val loader = IsolatingClassLoader(StringBuilderProxy::class.java)
        val foreignType = loader.loadClass(StringBuilderProxy::class.java.name)
        assertNotSame(StringBuilderProxy::class.java.module, foreignType.module)

        val proxy = ReflectionProxies.get(foreignType)

        assertTrue(proxy.javaClass.isHidden)
        assertSame(loader, proxy.javaClass.classLoader)
        val create = foreignType.getMethod("create", String::class.java)
        assertEquals("abc", create.invoke(proxy, "abc").toString())
    }

    /**
     * Defines [isolated] itself instead of delegating, so the copy lands in this loader's unnamed
     * module.
     */
    private class IsolatingClassLoader(private val isolated: Class<*>) :
        ClassLoader(isolated.classLoader) {

        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name != isolated.name) return super.loadClass(name, resolve)

            synchronized(getClassLoadingLock(name)) {
                findLoadedClass(name)?.let { return it }
                val resource = name.replace('.', '/') + ".class"
                val bytes = parent.getResourceAsStream(resource)!!.use { it.readBytes() }

                return defineClass(name, bytes, 0, bytes.size)
            }
        }
    }
}

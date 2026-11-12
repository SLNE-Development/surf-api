package dev.slne.surf.microservice.runtime.discovery

import dev.slne.surf.microservice.api.Microservice
import org.junit.jupiter.api.io.TempDir
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MicroserviceMainClassTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `creates a class through its no-argument constructor`() {
        val microservice = instantiate(resourceRoot("one", ClassMicroservice::class.java.name))

        assertIs<ClassMicroservice>(microservice)
    }

    @Test
    fun `uses the instance of an object`() {
        val microservice = instantiate(resourceRoot("one", ObjectMicroservice::class.java.name))

        assertSame(ObjectMicroservice, microservice)
    }

    @Test
    fun `ignores surrounding whitespace in the resource`() {
        val microservice = instantiate(resourceRoot("one", "\n ${ClassMicroservice::class.java.name} \r\n"))

        assertIs<ClassMicroservice>(microservice)
    }

    @Test
    fun `fails without a resource`() {
        val exception = assertFailsWith<MicroserviceDiscoveryException> { instantiate() }

        assertTrue("No ${MicroserviceMainClass.RESOURCE}" in exception.message.orEmpty())
    }

    @Test
    fun `fails with two resources and lists both`() {
        val first = resourceRoot("first", ClassMicroservice::class.java.name)
        val second = resourceRoot("second", ObjectMicroservice::class.java.name)

        val exception = assertFailsWith<MicroserviceDiscoveryException> { instantiate(first, second) }

        val message = exception.message.orEmpty()
        assertTrue("first" in message && "second" in message, message)
    }

    @Test
    fun `fails for an unknown class`() {
        assertFailsWith<MicroserviceDiscoveryException> { instantiate(resourceRoot("one", "com.example.Missing")) }
    }

    @Test
    fun `fails for a class that is not a microservice`() {
        val exception = assertFailsWith<MicroserviceDiscoveryException> {
            instantiate(resourceRoot("one", NotAMicroservice::class.java.name))
        }

        assertTrue("does not extend" in exception.message.orEmpty())
    }

    @Test
    fun `fails for a class without a no-argument constructor`() {
        val exception = assertFailsWith<MicroserviceDiscoveryException> {
            instantiate(resourceRoot("one", ConstructorArgumentMicroservice::class.java.name))
        }

        assertTrue("no-argument constructor" in exception.message.orEmpty())
    }

    @Test
    fun `passes on the exception of a failing constructor`() {
        val exception = assertFailsWith<MicroserviceDiscoveryException> {
            instantiate(resourceRoot("one", FailingMicroservice::class.java.name))
        }

        assertIs<IllegalStateException>(exception.cause)
    }

    private fun resourceRoot(directoryName: String, content: String): Path {
        val root = tempDir.resolve(directoryName)
        val resource = root.resolve(MicroserviceMainClass.RESOURCE)
        resource.parent.createDirectories()
        resource.writeText(content)
        return root
    }

    private fun instantiate(vararg roots: Path): Microservice {
        val urls = roots.map { root -> root.toUri().toURL() }.toTypedArray()
        return URLClassLoader(urls, javaClass.classLoader).use(MicroserviceMainClass::instantiate)
    }

    class ClassMicroservice : Microservice()

    object ObjectMicroservice : Microservice()

    class ConstructorArgumentMicroservice(@Suppress("unused") val port: Int) : Microservice()

    class FailingMicroservice : Microservice() {
        init {
            error("constructor failed")
        }
    }

    class NotAMicroservice
}

package dev.slne.surf.api.spring

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class SurfBeanTest {

    private class Greeter(val name: String)

    private val greeter by surfBean<Greeter>()
    private val qualifiedGreeter by surfBean<Greeter>("second")

    private val contexts = mutableListOf<AnnotationConfigApplicationContext>()

    @AfterEach
    fun closeContexts() {
        SurfContexts.uninstall()

        for (context in contexts) {
            context.close()
        }
    }

    private fun root(vararg names: String): AnnotationConfigApplicationContext {
        val context = AnnotationConfigApplicationContext()

        for (name in names) {
            context.registerBean(name) { Greeter(name) }
        }

        context.refresh()
        contexts += context

        return context
    }

    @Test
    fun `reading before a root is installed fails`() {
        assertFailsWith<IllegalStateException> { greeter }
    }

    @Test
    fun `the property follows the installed root`() {
        val first = root("first")
        val second = root("second")

        SurfContexts.install(first)
        assertSame(first.getBean(Greeter::class.java), greeter)

        SurfContexts.uninstall()
        SurfContexts.install(second)
        assertSame(second.getBean(Greeter::class.java), greeter)
    }

    @Test
    fun `the property keeps the last bean while no root is installed`() {
        val first = root("first")
        SurfContexts.install(first)
        val bean = greeter

        SurfContexts.uninstall()

        assertSame(bean, greeter)
    }

    @Test
    fun `a qualifier selects among several beans of a type`() {
        val both = root("first", "second")
        SurfContexts.install(both)

        assertSame(both.getBean("second"), qualifiedGreeter)
    }
}

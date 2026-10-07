package dev.slne.surf.nats.bytecode

import java.lang.classfile.ClassFile
import kotlin.test.Test
import kotlin.test.assertTrue

class ListenerInvokerClassTest {

    private val bytes = ListenerInvokerFactory.createInvokerClass()

    @Test
    fun `generated class passes the bytecode verifier`() {
        val model = ClassFile.of().parse(bytes)
        println(model.toDebugString())

        val errors = ClassFile.of().verify(model)
        assertTrue(errors.isEmpty(), errors.joinToString("\n"))
    }
}

package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.rpc.NatsService
import dev.slne.surf.nats.client.NatsClientImpl
import java.lang.classfile.ClassFile
import kotlin.test.Test
import kotlin.test.assertTrue

class ServiceProxyClassTest {

    @Suppress("unused")
    @NatsService("example")
    interface ExampleService {
        suspend fun mixed(a: Int, b: Long, factor: Double, flag: Boolean, label: String): String
        suspend fun noArguments(): Int
        suspend fun fire(message: String)

        fun local(): String = "local"
    }

    @Test
    fun `generated proxy class passes the bytecode verifier`() {
        val bytes = (NatsClient.create("proxy-class-test") as NatsClientImpl).use { client ->
            ServiceProxyFactory.createProxyClass(client.serviceDescriptor(ExampleService::class.java))
        }

        val model = ClassFile.of().parse(bytes)
        println(model.toDebugString())

        val errors = ClassFile.of().verify(model)
        assertTrue(errors.isEmpty(), errors.joinToString("\n"))
    }
}

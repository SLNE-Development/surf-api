package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.test.proto.v1.CalcService
import dev.slne.surf.nats.test.proto.v1.FinderService
import dev.slne.surf.nats.test.proto.v1.TypesService
import java.lang.classfile.ClassFile
import kotlin.test.Test
import kotlin.test.assertTrue
import dev.slne.surf.nats.test.proto.v2.VersionedService as VersionedServiceV2

class ServiceProxyClassTest {

    // Primitives, value classes (Duration, Duration?, Tag, Tag?), nullable Long, no arguments, Unit, Answer,
    // default parameter values and every field kind of Everything
    private val services = listOf(
        CalcService::class.java,
        FinderService::class.java,
        VersionedServiceV2::class.java,
        TypesService::class.java,
    )

    @Test
    fun `generated proxy classes pass the bytecode verifier`() {
        (NatsClient.create("proxy-class-test") as NatsClientImpl).use { client ->
            for (service in services) {
                val bytes = ServiceProxyFactory.createProxyClass(client.serviceDescriptor(service))

                val model = ClassFile.of().parse(bytes)
                println(model.toDebugString())

                val errors = ClassFile.of().verify(model)
                assertTrue(errors.isEmpty(), "${service.name}:\n" + errors.joinToString("\n"))
            }
        }
    }
}

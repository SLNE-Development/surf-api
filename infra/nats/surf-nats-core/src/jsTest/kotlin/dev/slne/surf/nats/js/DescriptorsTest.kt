package dev.slne.surf.nats.js

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.descriptor.ProtocolOptionReader
import dev.slne.surf.nats.js.event.EventDescriptor
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.DescService
import dev.slne.surf.nats.js.fixtures.AuditService
import dev.slne.surf.nats.js.fixtures.ChatMessageSchema
import dev.slne.surf.nats.js.fixtures.OptedOutMessageSchema
import dev.slne.surf.nats.js.fixtures.PlainMessageSchema
import dev.slne.surf.nats.js.fixtures.PlayerJoinedSchema
import dev.slne.surf.nats.js.fixtures.PlayerService
import dev.slne.surf.nats.js.fixtures.RegistryService
import dev.slne.surf.nats.js.fixtures.RulesSchema
import dev.slne.surf.nats.js.fixtures.ServerStartedSchema
import dev.slne.surf.nats.js.fixtures.StreamingService
import dev.slne.surf.nats.js.rpc.MethodDescriptor
import dev.slne.surf.nats.protocol.ProtocolDefaults
import dev.slne.surf.nats.rpc.Recipients
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.js.fixtures.v2.PlayerJoinedSchema as PlayerJoinedV2Schema
import dev.slne.surf.nats.js.fixtures.v2.PlayerService as PlayerServiceV2

class DescriptorsTest {

    private fun desc(service: DescService, localName: String): DescMethod = service.methods.first { it.localName == localName }

    private fun method(service: DescService, localName: String): MethodDescriptor = MethodDescriptor.of(desc(service, localName))

    @Test
    fun eventSubjectIsTheMessageFullName() {
        val descriptor = EventDescriptor.of(PlayerJoinedSchema)
        assertEquals("surf.natsjs.test.v1.PlayerJoined", descriptor.typeName)
        assertEquals("surf.evt.surf.natsjs.test.v1.PlayerJoined", descriptor.subject)
        assertEquals("surf.evt.surf.natsjs.test.v1.ChatMessage", EventDescriptor.of(ChatMessageSchema).subject)
        assertEquals("surf.evt.surf.natsjs.test.v1.ServerStarted", EventDescriptor.of(ServerStartedSchema).subject)
    }

    @Test
    fun majorVersionsOfAnEventHaveDistinctSubjects() {
        assertEquals("surf.evt.surf.natsjs.test.v2.PlayerJoined", EventDescriptor.of(PlayerJoinedV2Schema).subject)
    }

    @Test
    fun messageWithoutEventOptionIsRejected() {
        for (schema in listOf(PlainMessageSchema, OptedOutMessageSchema)) {
            val error = assertFailsWith<IllegalArgumentException> { EventDescriptor.of(schema) }
            assertTrue(error.message!!.contains("is not a surf-nats event"), error.message)
        }
    }

    @Test
    fun descriptorsAreCachedPerSchema() {
        assertSame(EventDescriptor.of(PlayerJoinedSchema), EventDescriptor.of(PlayerJoinedSchema))
        val find = desc(PlayerService, "findPlayer")
        assertSame(MethodDescriptor.of(find), MethodDescriptor.of(find))
        assertSame(MethodDescriptor.of(find).service, method(PlayerService, "kick").service)
    }

    @Test
    fun coreMethodDefaults() {
        val find = method(PlayerService, "findPlayer")
        assertEquals("surf.natsjs.test.v1.PlayerService", find.service.names.service)
        assertEquals("FindPlayer", find.protoName)
        assertEquals("surf.natsjs.test.v1.PlayerService.FindPlayer", find.label)
        assertFalse(find.persistent)
        assertEquals(Recipients.ANY_INSTANCE, find.recipients)
        assertEquals(NatsResultKind.VALUE, find.result)
        assertEquals(ProtocolDefaults.CORE_TIMEOUT_SECONDS.seconds, find.timeout)
        assertEquals("surf.rpc.surf.natsjs.test.v1.PlayerService.FindPlayer", find.subject)
    }

    @Test
    fun resultKindsAndMethodOptions() {
        assertEquals(NatsResultKind.UNIT, method(PlayerService, "kick").result)

        val locate = method(PlayerService, "locate")
        assertEquals(Recipients.ALL_INSTANCES, locate.recipients)
        assertEquals(NatsResultKind.ANSWER, locate.result)

        val broadcast = method(PlayerService, "broadcast")
        assertEquals("surf.rpc.surf.natsjs.test.v1.PlayerService.Broadcast", broadcast.subject)
        assertEquals(Recipients.ALL_INSTANCES, broadcast.recipients)
        assertEquals(NatsResultKind.UNIT, broadcast.result)

        assertEquals(1.seconds, method(PlayerService, "slowFind").timeout)
    }

    @Test
    fun persistentServiceInheritance() {
        val record = method(AuditService, "record")
        assertTrue(record.persistent)
        assertEquals(NatsResultKind.UNIT, record.result)
        assertEquals(10.seconds, record.timeout)
        assertEquals("surf.persist.surf.natsjs.test.v1.AuditService.Record", record.subject)
        assertEquals("SURF_PERSIST_surf_natsjs_test_v1_AuditService", record.stream)

        val query = method(AuditService, "query")
        assertTrue(query.persistent)
        assertEquals(NatsResultKind.VALUE, query.result)

        // persistent = false on the method overrides the service, timeout 0 inherits the service's 10 s
        val ping = method(AuditService, "ping")
        assertFalse(ping.persistent)
        assertEquals(10.seconds, ping.timeout)
        assertEquals(NatsResultKind.UNIT, ping.result)
        assertEquals("surf.rpc.surf.natsjs.test.v1.AuditService.Ping", ping.subject)
    }

    @Test
    fun recipientsInheritance() {
        val lookup = method(RegistryService, "lookup")
        assertEquals(Recipients.ANY_INSTANCE, lookup.recipients)
        assertEquals(NatsResultKind.VALUE, lookup.result)
        assertEquals("surf.rpc.surf.natsjs.test.v1.RegistryService.Lookup", lookup.subject)

        val announce = method(RegistryService, "announce")
        assertEquals(Recipients.ALL_INSTANCES, announce.recipients)
        assertEquals(NatsResultKind.UNIT, announce.result)
    }

    @Test
    fun versionedServiceNames() {
        val find = method(PlayerServiceV2, "findPlayer")
        assertEquals("surf.rpc.surf.natsjs.test.v2.PlayerService.FindPlayer", find.subject)
        assertEquals("SURF_PERSIST_surf_natsjs_test_v2_PlayerService", find.service.names.stream)
    }

    @Test
    fun streamingMethodsAreRejectedWithTheirService() {
        val error = assertFailsWith<IllegalArgumentException> { method(StreamingService, "watch") }
        assertTrue(error.message!!.contains("streaming rpcs are not supported"), error.message)
    }

    @Test
    fun nullableOption() {
        val fields = RulesSchema.fields.associateBy { it.localName }
        assertTrue(ProtocolOptionReader.isNullable(fields.getValue("nullableText")))
        assertFalse(ProtocolOptionReader.isNullable(fields.getValue("requiredText")))
        assertFalse(ProtocolOptionReader.isNullable(fields.getValue("defaulted")))
    }
}

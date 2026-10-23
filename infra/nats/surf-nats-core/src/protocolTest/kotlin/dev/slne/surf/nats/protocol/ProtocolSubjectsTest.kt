package dev.slne.surf.nats.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ProtocolSubjectsTest {

    @Test
    fun kindTokens() {
        assertEquals("\$rpc", ProtocolSubjects.RPC)
        assertEquals("\$persist", ProtocolSubjects.PERSIST)
        assertEquals("\$presence", ProtocolSubjects.PRESENCE)
        assertEquals("\$dlq", ProtocolSubjects.DLQ)
    }

    @Test
    fun eventSubjectsMatchVectors() {
        for (vector in ProtocolVectors.events) {
            val subject = ProtocolSubjects.event(vector.client, vector.subject, vector.majorVersion)
            assertEquals(vector.expected, subject, "event '${vector.subject}' of ${vector.client}")
            assertTrue(ProtocolNaming.isValidEventSubject(subject), "event subject '$subject'")
        }
    }

    @Test
    fun serviceNamesMatchVectors() {
        for (vector in ProtocolVectors.services) {
            val names = ServiceNames(vector.client, vector.service, vector.majorVersion)
            val label = "service '${vector.service}' v${vector.majorVersion} of ${vector.client}"

            assertEquals(vector.rpcBase, names.rpcBase, label)
            assertEquals(vector.persistBase, names.persistBase, label)
            assertEquals(vector.presenceSubject, names.presenceSubject, label)
            assertEquals(vector.dlqSubject, names.dlqSubject, label)
            assertEquals(vector.streamFilter, names.streamFilter, label)
            assertEquals(vector.stream, names.stream, label)
            assertEquals(vector.dlqStream, names.dlqStream, label)
            assertEquals(vector.responderGroup, names.responderGroup, label)
            assertEquals(vector.registrationKey, names.registrationKey, label)

            for (method in vector.methods) {
                val subject = names.methodSubject(method.method, method.persistent)
                assertEquals(method.expected, subject, "$label method '${method.method}'")
                assertTrue(ProtocolNaming.isValidSubject(subject), "method subject '$subject'")
            }
        }
    }

    @Test
    fun majorVersionsOfOneServiceAreDisjoint() {
        val v1 = ServiceNames("surf-punish", "player", 1)
        val v2 = ServiceNames("surf-punish", "player", 2)

        assertNotEquals(v1.rpcBase, v2.rpcBase)
        assertNotEquals(v1.persistBase, v2.persistBase)
        assertNotEquals(v1.presenceSubject, v2.presenceSubject)
        assertNotEquals(v1.dlqSubject, v2.dlqSubject)
        assertNotEquals(v1.stream, v2.stream)
        assertNotEquals(v1.dlqStream, v2.dlqStream)
        assertNotEquals(v1.registrationKey, v2.registrationKey)
        assertEquals(v1.responderGroup, v2.responderGroup)
    }

    @Test
    fun globalAndClientServicesOfOneNameAreDisjoint() {
        val client = ServiceNames("roles", "roles", 1)
        val global = ServiceNames(null, "roles", 1)

        assertNotEquals(client.rpcBase, global.rpcBase)
        assertNotEquals(client.stream, global.stream)
        assertNotEquals(client.registrationKey, global.registrationKey)
    }

    @Test
    fun versionsBelowOneAreRejected() {
        assertEquals("v1", ProtocolSubjects.versionToken(1))
        for (version in listOf(0, -1, Int.MIN_VALUE)) {
            assertFailsWith<IllegalArgumentException>("version $version") { ProtocolSubjects.versionToken(version) }
            assertFailsWith<IllegalArgumentException>("version $version") {
                ProtocolSubjects.event("punish", "created", version)
            }
            assertFailsWith<IllegalArgumentException>("version $version") { ServiceNames("punish", "player", version) }
        }
    }
}

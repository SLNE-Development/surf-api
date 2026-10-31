package dev.slne.surf.nats.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtocolSubjectsTest {

    @Test
    fun kindTokens() {
        assertEquals("surf", ProtocolSubjects.ROOT)
        assertEquals("rpc", ProtocolSubjects.RPC)
        assertEquals("persist", ProtocolSubjects.PERSIST)
        assertEquals("presence", ProtocolSubjects.PRESENCE)
        assertEquals("dlq", ProtocolSubjects.DLQ)
        assertEquals("evt", ProtocolSubjects.EVENT)
    }

    @Test
    fun eventSubjectsMatchVectors() {
        for (vector in ProtocolVectors.events) {
            val subject = ProtocolSubjects.event(vector.messageName)
            assertEquals(vector.expected, subject, "event ${vector.messageName}")
            assertTrue(SubjectSyntax.isValid(subject), "event subject '$subject'")
        }
    }

    @Test
    fun eventsRequireProtoNames() {
        for (name in ProtocolVectors.invalidProtoNames) {
            val error = assertFailsWith<IllegalArgumentException>("event '$name'") { ProtocolSubjects.event(name) }
            assertEquals("event $name: ${ProtocolNaming.PROTO_NAME_RULE}", error.message)
        }
    }

    @Test
    fun serviceNamesMatchVectors() {
        for (vector in ProtocolVectors.services) {
            val names = ServiceNames(vector.service)
            val label = "service ${vector.service}"

            assertEquals(vector.service, names.service, label)
            assertEquals(vector.rpcBase, names.rpcBase, label)
            assertEquals(vector.persistBase, names.persistBase, label)
            assertEquals(vector.presenceSubject, names.presenceSubject, label)
            assertEquals(vector.dlqSubject, names.dlqSubject, label)
            assertEquals(vector.streamFilter, names.streamFilter, label)
            assertEquals(vector.stream, names.stream, label)
            assertEquals(vector.dlqStream, names.dlqStream, label)
            assertEquals(vector.service, names.queueGroup, label)
            assertEquals("responders", names.durable, label)
            assertNull(ServiceNames.problem(vector.service), label)

            for (method in vector.methods) {
                val subject = names.methodSubject(method.method, method.persistent)
                assertEquals(method.expected, subject, "$label method '${method.method}'")
                assertTrue(SubjectSyntax.isValid(subject), "method subject '$subject'")
            }
        }
    }

    @Test
    fun streamAndConsumerNames() {
        assertEquals("SURF_PERSIST_", ProtocolStreams.PERSIST_PREFIX)
        assertEquals("SURF_DLQ_", ProtocolStreams.DLQ_PREFIX)
        assertEquals(255, ProtocolStreams.MAX_NAME_LENGTH)
        assertEquals("responders", ProtocolStreams.DURABLE)
        assertEquals("a_b__c", ProtocolStreams.encode("a.b_c"))
        assertEquals("a__b_c", ProtocolStreams.encode("a_b.c"))
        assertEquals("x____y_Z", ProtocolStreams.encode("x__y.Z"))
        assertEquals("a_____b", ProtocolStreams.encode("a__.b"))
    }

    @Test
    fun underscoreAndDotDoNotCollideInStreamNames() {
        val left = ServiceNames("a.b_c")
        val right = ServiceNames("a_b.c")

        assertNotEquals(left.stream, right.stream)
        assertNotEquals(left.dlqStream, right.dlqStream)
        assertNotEquals(left.rpcBase, right.rpcBase)
    }

    @Test
    fun streamNameEncodingIsInjectiveExhaustively() {
        val byEncoding = HashMap<String, String>()
        for (name in allStrings("aB_.", maxLength = 7).filter(ProtocolNaming::isProtoName)) {
            val previous = byEncoding.put(ProtocolStreams.encode(name), name)
            assertNull(previous, "'$name' and '$previous' encode to ${ProtocolStreams.encode(name)}")
        }
    }

    @Test
    fun streamNameEncodingKeepsTheCase() {
        val upper = ServiceNames("surf.punish.v1.PlayerService")
        val lower = ServiceNames("surf.punish.v1.playerservice")

        assertEquals("SURF_PERSIST_surf_punish_v1_PlayerService", upper.stream)
        assertEquals("SURF_PERSIST_surf_punish_v1_playerservice", lower.stream)
        assertNotEquals(upper.stream, lower.stream)
        assertNotEquals(upper.rpcBase, lower.rpcBase)
    }

    @Test
    fun majorVersionsOfOneServiceAreDisjoint() {
        val v1 = ServiceNames("surf.punish.v1.PlayerService")
        val v2 = ServiceNames("surf.punish.v2.PlayerService")

        assertNotEquals(v1.rpcBase, v2.rpcBase)
        assertNotEquals(v1.persistBase, v2.persistBase)
        assertNotEquals(v1.presenceSubject, v2.presenceSubject)
        assertNotEquals(v1.dlqSubject, v2.dlqSubject)
        assertNotEquals(v1.stream, v2.stream)
        assertNotEquals(v1.dlqStream, v2.dlqStream)
        assertNotEquals(v1.queueGroup, v2.queueGroup)
        assertEquals(v1.durable, v2.durable)
    }

    @Test
    fun streamNamesAreLimitedTo255EncodedCharacters() {
        // "a_" encodes to "a__", so the encoded name is one character longer than the full name.
        val longest = "a_" + "b".repeat(239)
        val tooLong = "a_" + "b".repeat(240)

        val names = ServiceNames(longest)
        assertEquals(255, names.stream.length)
        assertTrue(names.dlqStream.length < 255)
        assertNull(ServiceNames.problem(longest))
        assertEquals(emptyList(), ProtocolResolution.service(ServiceOptions(), longest).problems())

        assertEquals(255, ProtocolStreams.PERSIST_PREFIX.length + tooLong.length)
        val expected = "service $tooLong: its stream name ${ProtocolStreams.persist(tooLong)} has 256 characters, " +
                "more than 255; shorten the package or the service name"
        assertEquals(expected, ServiceNames.problem(tooLong))
        assertEquals(listOf(expected), ProtocolResolution.service(ServiceOptions(), tooLong).problems())
        val error = assertFailsWith<IllegalArgumentException> { ServiceNames(tooLong) }
        assertEquals(expected, error.message)
    }

    @Test
    fun servicesRequireProtoNames() {
        for (name in ProtocolVectors.invalidProtoNames) {
            val expected = "service $name: ${ProtocolNaming.PROTO_NAME_RULE}"
            assertEquals(expected, ServiceNames.problem(name), "service '$name'")
            assertEquals(listOf(expected), ProtocolResolution.service(ServiceOptions(), name).problems())
            val error = assertFailsWith<IllegalArgumentException>("service '$name'") { ServiceNames(name) }
            assertEquals(expected, error.message)
        }
    }

    @Test
    fun methodsRequireSingleProtoIdentifiers() {
        val names = ServiceNames("a.v1.S")
        assertNull(ServiceNames.methodProblem("a.v1.S", "Find_Player2"))

        val dotted = "rpc a.v1.S.A.B: the declared name 'A.B' must not contain '.'"
        assertEquals(dotted, ServiceNames.methodProblem("a.v1.S", "A.B"))
        assertEquals(dotted, assertFailsWith<IllegalArgumentException> { names.methodSubject("A.B", persistent = true) }.message)

        for (method in listOf("", "_A", "1A", "A-B", "A*", "A B")) {
            val expected = "rpc a.v1.S.$method: ${ProtocolNaming.PROTO_NAME_RULE}"
            assertEquals(expected, ServiceNames.methodProblem("a.v1.S", method), "rpc '$method'")
            val error = assertFailsWith<IllegalArgumentException>("rpc '$method'") { names.methodSubject(method, persistent = false) }
            assertEquals(expected, error.message)
        }
    }

    @Test
    fun servicesRejectDottedDeclaredNames() {
        val service = ProtocolResolution.service(ServiceOptions(), "a.v1.S.T")
        assertEquals(emptyList(), service.problems())
        assertEquals(listOf("service a.v1.S.T: the declared name 'S.T' must not contain '.'"), service.problems("S.T"))
        assertEquals(emptyList(), ProtocolResolution.service(ServiceOptions(), "a.v1.S").problems("S"))
    }

    private fun allStrings(alphabet: String, maxLength: Int): List<String> {
        val result = mutableListOf("")
        var previous = listOf("")
        repeat(maxLength) {
            previous = previous.flatMap { prefix -> alphabet.map { prefix + it } }
            result += previous
        }
        return result
    }
}

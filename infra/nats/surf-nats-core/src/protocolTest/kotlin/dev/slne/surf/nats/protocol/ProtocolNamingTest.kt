package dev.slne.surf.nats.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtocolNamingTest {

    @Test
    fun kebabCaseMatchesVectors() {
        for ((name, expected) in ProtocolVectors.kebabCase) {
            assertEquals(expected, ProtocolNaming.kebabCase(name), "kebab-case of '$name'")
        }
    }

    @Test
    fun kebabCaseMatchesReferenceRegexExhaustively() {
        val boundary = Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
        for (name in allStrings("abAB1_-Ä", maxLength = 4)) {
            val expected = name.split(boundary).joinToString("-") { it.lowercase() }
            assertEquals(expected, ProtocolNaming.kebabCase(name), "kebab-case of '$name'")
        }
    }

    @Test
    fun validTokensAreAccepted() {
        for (token in ProtocolVectors.validTokens) {
            assertTrue(ProtocolNaming.isValidToken(token), "token '$token'")
        }
    }

    @Test
    fun invalidTokensAreRejected() {
        for (token in ProtocolVectors.invalidTokens) {
            assertFalse(ProtocolNaming.isValidToken(token), "token '$token'")
            assertFalse(ProtocolNaming.isValidNameToken(token), "name token '$token'")
        }
    }

    @Test
    fun reservedTokensAreNoNameTokens() {
        assertTrue(ProtocolNaming.isReservedToken("\$rpc"))
        assertFalse(ProtocolNaming.isReservedToken("a\$"))
        assertFalse(ProtocolNaming.isValidNameToken("\$rpc"))
        assertFalse(ProtocolNaming.isValidNameToken("\$"))
        assertTrue(ProtocolNaming.isValidNameToken("find-server"))
        assertTrue(ProtocolNaming.isValidNameToken("a\$"))
    }

    @Test
    fun subjectsMatchVectors() {
        for (subject in ProtocolVectors.validSubjects) {
            assertTrue(ProtocolNaming.isValidSubject(subject), "subject '$subject'")
            assertEquals(subject, ProtocolNaming.requireValidSubject(subject, "test"))
        }
        for (subject in ProtocolVectors.invalidSubjects) {
            assertFalse(ProtocolNaming.isValidSubject(subject), "subject '$subject'")
            assertFailsWith<IllegalArgumentException>("subject '$subject'") {
                ProtocolNaming.requireValidSubject(subject, "test")
            }
        }
    }

    @Test
    fun subjectsMatchReferenceRegexExhaustively() {
        // java.util.regex `^[^\s.*>]+(\.[^\s.*>]+)*$` with `\s` spelled out, so JS evaluates the same set.
        val token = "[^ \t\n\u000B\u000C\r.*>]+"
        val subject = Regex("$token(\\.$token)*")
        for (candidate in allStrings("a.*> \t\$ ", maxLength = 4)) {
            assertEquals(subject.matches(candidate), ProtocolNaming.isValidSubject(candidate), "subject '$candidate'")
        }
    }

    @Test
    fun eventSubjectsMatchVectors() {
        for (subject in ProtocolVectors.validEventSubjects) {
            assertTrue(ProtocolNaming.isValidEventSubject(subject), "event subject '$subject'")
            assertEquals(subject, ProtocolNaming.requireValidEventSubject(subject, "test"))
        }
        for (subject in ProtocolVectors.invalidEventSubjects) {
            assertFalse(ProtocolNaming.isValidEventSubject(subject), "event subject '$subject'")
            assertFailsWith<IllegalArgumentException>("event subject '$subject'") {
                ProtocolNaming.requireValidEventSubject(subject, "test")
            }
        }
    }

    @Test
    fun kebabNamesMatchVectors() {
        for (name in ProtocolVectors.kebabNames) {
            assertTrue(ProtocolNaming.isKebabName(name), "name '$name'")
        }
        for (name in ProtocolVectors.invalidKebabNames) {
            assertFalse(ProtocolNaming.isKebabName(name), "name '$name'")
        }
    }

    @Test
    fun kebabNamesMatchReferenceRegexExhaustively() {
        val pattern = Regex("[a-z0-9]+(-[a-z0-9]+)*")
        for (candidate in allStrings("a1-A_", maxLength = 5)) {
            assertEquals(pattern.matches(candidate), ProtocolNaming.isKebabName(candidate), "name '$candidate'")
        }
    }

    @Test
    fun majorVersionsMatchVectors() {
        for ((protoPackage, expected) in ProtocolVectors.majorVersions) {
            assertEquals(expected, ProtocolNaming.majorVersionOrNull(protoPackage), "package '$protoPackage'")
            assertEquals(expected, ProtocolNaming.majorVersion(protoPackage), "package '$protoPackage'")
        }
        for (protoPackage in ProtocolVectors.invalidVersionPackages) {
            assertNull(ProtocolNaming.majorVersionOrNull(protoPackage), "package '$protoPackage'")
            assertFailsWith<IllegalArgumentException>("package '$protoPackage'") {
                ProtocolNaming.majorVersion(protoPackage)
            }
        }
    }

    @Test
    fun majorVersionErrorsNameTheSource() {
        val plain = assertFailsWith<IllegalArgumentException> { ProtocolNaming.majorVersion("surf.punish") }
        assertEquals(
            "Proto package 'surf.punish' must end with a major version suffix '.v<N>' (N >= 1, no leading zeros)",
            plain.message,
        )
        val named = assertFailsWith<IllegalArgumentException> { ProtocolNaming.majorVersion("surf.punish", "a.B") }
        assertEquals(
            "Proto package 'surf.punish' of a.B must end with a major version suffix '.v<N>' (N >= 1, no leading zeros)",
            named.message,
        )
    }

    @Test
    fun clientNamesMustBeKebabCase() {
        for (name in ProtocolVectors.kebabNames) {
            assertEquals(name, ProtocolNaming.requireClientName(name))
        }
        for (name in ProtocolVectors.invalidKebabNames) {
            val error = assertFailsWith<IllegalArgumentException>("name '$name'") { ProtocolNaming.requireClientName(name) }
            assertEquals("Invalid NATS client name '$name': must be kebab-case ([a-z0-9] separated by '-')", error.message)
        }
        val owner = assertFailsWith<IllegalArgumentException> { ProtocolNaming.requireClientName("A", "owner") }
        assertEquals("Invalid owner 'A': must be kebab-case ([a-z0-9] separated by '-')", owner.message)
    }

    @Test
    fun serviceNamesMustBeKebabCaseAndNotReserved() {
        for (name in ProtocolVectors.kebabNames) {
            assertNull(ProtocolNaming.serviceNameProblem(name, "a.S"), "name '$name'")
        }
        for (name in ProtocolVectors.invalidKebabNames) {
            assertEquals(
                "service a.S resolves to the name '$name', which is not kebab-case ([a-z0-9] words joined by '-'); " +
                        "set (surf.nats.v1.service).name",
                ProtocolNaming.serviceNameProblem(name, "a.S"),
            )
        }
        for (name in listOf("dlq", "roles-dlq", "a-b-dlq")) {
            assertEquals(
                "service name '$name' is reserved: its stream would collide with the dead-letter stream of another service",
                ProtocolNaming.serviceNameProblem(name, "a.S"),
            )
        }
        assertNull(ProtocolNaming.serviceNameProblem("dlq-roles", "a.S"))
        assertNull(ProtocolNaming.serviceNameProblem("roles-dlqs", "a.S"))
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

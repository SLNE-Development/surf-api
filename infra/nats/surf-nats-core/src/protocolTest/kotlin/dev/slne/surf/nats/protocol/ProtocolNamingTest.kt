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
    fun subjectsMatchVectors() {
        for (subject in ProtocolVectors.validSubjects) {
            assertTrue(SubjectSyntax.isValid(subject), "subject '$subject'")
        }
        for (subject in ProtocolVectors.invalidSubjects) {
            assertFalse(SubjectSyntax.isValid(subject), "subject '$subject'")
        }
    }

    @Test
    fun subjectsMatchReferenceRegexExhaustively() {
        // java.util.regex `^[^\s.*>]+(\.[^\s.*>]+)*$` with `\s` spelled out, so JS evaluates the same set.
        val token = "[^ \t\n\u000B\u000C\r.*>]+"
        val subject = Regex("$token(\\.$token)*")
        for (candidate in allStrings("a.*> \t\$ ", maxLength = 4)) {
            assertEquals(subject.matches(candidate), SubjectSyntax.isValid(candidate), "subject '$candidate'")
        }
    }

    @Test
    fun protoNamesMatchVectors() {
        for (name in ProtocolVectors.protoNames) {
            assertTrue(ProtocolNaming.isProtoName(name), "proto name '$name'")
            assertEquals(name, ProtocolNaming.requireProtoName(name, "test"))
        }
        for (name in ProtocolVectors.invalidProtoNames) {
            assertFalse(ProtocolNaming.isProtoName(name), "proto name '$name'")
            val error = assertFailsWith<IllegalArgumentException>("proto name '$name'") {
                ProtocolNaming.requireProtoName(name, "test")
            }
            assertEquals("test $name: ${ProtocolNaming.PROTO_NAME_RULE}", error.message)
        }
    }

    @Test
    fun protoIdentifiersAreProtoNamesWithoutDots() {
        for (name in ProtocolVectors.protoNames) {
            assertEquals('.' !in name, ProtocolNaming.isProtoIdentifier(name), "identifier '$name'")
        }
        for (name in ProtocolVectors.invalidProtoNames) {
            assertFalse(ProtocolNaming.isProtoIdentifier(name), "identifier '$name'")
        }
    }

    @Test
    fun nameProblemsRejectDottedDeclaredNames() {
        assertNull(ProtocolNaming.nameProblem("service", "a.v1.S", "S"))
        assertNull(ProtocolNaming.nameProblem("service", "a.v1.S"))
        assertEquals(
            "service a.v1.S.T: the declared name 'S.T' must not contain '.'",
            ProtocolNaming.nameProblem("service", "a.v1.S.T", "S.T"),
        )
        assertEquals(
            "event a.v1._E: ${ProtocolNaming.PROTO_NAME_RULE}",
            ProtocolNaming.nameProblem("event", "a.v1._E", "_E"),
        )
    }

    @Test
    fun protoNamesMatchReferenceRegexExhaustively() {
        val identifier = "[A-Za-z][A-Za-z0-9_]*"
        val pattern = Regex("$identifier(\\.$identifier)*")
        for (candidate in allStrings("aB1_.-", maxLength = 5)) {
            assertEquals(pattern.matches(candidate), ProtocolNaming.isProtoName(candidate), "proto name '$candidate'")
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
        val labelled = assertFailsWith<IllegalArgumentException> { ProtocolNaming.requireClientName("A", "client name") }
        assertEquals("Invalid client name 'A': must be kebab-case ([a-z0-9] separated by '-')", labelled.message)
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

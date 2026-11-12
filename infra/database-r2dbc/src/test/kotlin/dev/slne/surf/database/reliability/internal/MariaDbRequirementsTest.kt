package dev.slne.surf.database.reliability.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class MariaDbRequirementsTest {

    @ParameterizedTest
    @ValueSource(strings = ["10.6.0-MariaDB", "10.11.9-MariaDB-ubu2204", "11.4.2-MariaDB-ubu2404", "12.0.1-MariaDB"])
    fun acceptsMariaDb106AndNewer(version: String) {
        MariaDbRequirements.requireSupportedVersion(version)
    }

    @ParameterizedTest
    @ValueSource(strings = ["10.5.27-MariaDB-ubu2004", "10.3.39-MariaDB", "5.5.68-MariaDB"])
    fun rejectsMariaDbBefore106(version: String) {
        val failure = assertFailsWith<IllegalStateException> { MariaDbRequirements.requireSupportedVersion(version) }

        assertContains(failure.message.orEmpty(), "SKIP LOCKED")
        assertContains(failure.message.orEmpty(), version)
    }

    @Test
    fun rejectsMySql() {
        assertFailsWith<IllegalStateException> { MariaDbRequirements.requireSupportedVersion("8.4.2") }
    }

    @Test
    fun requiresInnoDbForEveryTable() {
        MariaDbRequirements.requireInnoDb(mapOf("a" to "InnoDB", "b" to "InnoDB"))

        val failure = assertFailsWith<IllegalStateException> {
            MariaDbRequirements.requireInnoDb(mapOf("a" to "InnoDB", "b" to "Aria", "c" to null))
        }

        assertContains(failure.message.orEmpty(), "b: Aria")
        assertContains(failure.message.orEmpty(), "c: missing")
    }
}

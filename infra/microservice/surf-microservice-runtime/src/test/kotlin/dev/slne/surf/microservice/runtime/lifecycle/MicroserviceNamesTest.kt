package dev.slne.surf.microservice.runtime.lifecycle

import dev.slne.surf.microservice.api.Microservice
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MicroserviceNamesTest {

    @ParameterizedTest
    @ValueSource(strings = ["punish", "surf-punish", "a1-b2-c3", "9"])
    fun `accepts valid names`(name: String) {
        assertEquals(name, MicroserviceNames.requireValid(name))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Punish", "surf_punish", "-punish", "punish-", "surf--punish", "surf punish"])
    fun `rejects invalid names`(name: String) {
        assertFailsWith<IllegalStateException> { MicroserviceNames.requireValid(name) }
    }

    @Test
    fun `accepts names up to the maximum length`() {
        val name = "a".repeat(Microservice.MAX_NAME_LENGTH)

        assertEquals(name, MicroserviceNames.requireValid(name))
    }

    @Test
    fun `rejects names above the maximum length`() {
        assertFailsWith<IllegalStateException> {
            MicroserviceNames.requireValid("a".repeat(Microservice.MAX_NAME_LENGTH + 1))
        }
    }
}

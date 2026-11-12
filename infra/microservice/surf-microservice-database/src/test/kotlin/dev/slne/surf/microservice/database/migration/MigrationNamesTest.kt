package dev.slne.surf.microservice.database.migration

import kotlin.test.Test
import kotlin.test.assertEquals

class MigrationNamesTest {

    @Test
    fun `replaces hyphens with underscores`() {
        assertEquals("punish_web_api", migrationName("punish-web-api"))
    }

    @Test
    fun `keeps names without hyphens`() {
        assertEquals("punish", migrationName("punish"))
    }
}

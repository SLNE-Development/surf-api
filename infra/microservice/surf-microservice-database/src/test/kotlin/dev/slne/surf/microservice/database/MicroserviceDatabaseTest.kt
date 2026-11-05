package dev.slne.surf.microservice.database

import dev.slne.surf.microservice.api.Microservice
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MicroserviceDatabaseTest {

    @Test
    fun `database is unavailable before the integration started`() {
        assertFailsWith<IllegalStateException> { TestMicroservice.database }
    }

    @Test
    fun `database api is unavailable before the integration started`() {
        assertFailsWith<IllegalStateException> { TestMicroservice.databaseApi }
    }

    private object TestMicroservice : Microservice()
}

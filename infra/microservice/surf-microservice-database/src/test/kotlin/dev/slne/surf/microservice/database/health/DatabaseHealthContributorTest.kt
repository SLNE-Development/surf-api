package dev.slne.surf.microservice.database.health

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class DatabaseHealthContributorTest {

    @Test
    fun `reports nothing while the database is not running`() = runTest {
        assertTrue(DatabaseHealthContributor().check().isEmpty())
    }
}

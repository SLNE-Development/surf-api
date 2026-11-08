package dev.slne.surf.microservice.reliability.test

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.microservice.reliability.support.RecordingTestService
import dev.slne.surf.microservice.reliability.support.TestDatabase
import dev.slne.surf.microservice.reliability.support.TestVendor
import dev.slne.surf.nats.test.common.proto.v1.StoreRequest
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [TransactionalHandlerFixture] on a fresh database without the reliability schema and without a started NATS.
 */
@Testcontainers(disabledWithoutDocker = true)
class TransactionalHandlerFixtureIT {

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `events of a committed block are recorded`(vendor: TestVendor) =
        withFixture(vendor) { setup, fixture ->
            val service = RecordingTestService(setup.api, fixture.events)

            val result = fixture.runInHandler {
                service.store(StoreRequest(entry = "first"))
                service.store(StoreRequest(entry = "second"))
                "done"
            }

            assertEquals("done", result)
            assertEquals(listOf("first", "second"), fixture.publishedOf<TestChatEvent>().map { it.message })
            assertEquals(listOf("first", "second"), fixture.published.map { it.aggregateId })
            assertEquals(1L, setup.entries("first"))
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `a failing block rolls back its statements and events`(vendor: TestVendor) =
        withFixture(vendor) { setup, fixture ->
            val service = RecordingTestService(setup.api, fixture.events)

            val thrown = assertFailsWith<IllegalStateException> {
                fixture.runInHandler {
                    service.store(StoreRequest(entry = "rolled-back"))
                    throw IllegalStateException("handler failed")
                }
            }

            assertEquals("handler failed", thrown.message)
            assertEquals(emptyList(), fixture.published)
            assertEquals(0L, setup.entries("rolled-back"))
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `concurrent blocks record their events independently`(vendor: TestVendor) =
        withFixture(vendor) { setup, fixture ->
            val service = RecordingTestService(setup.api, fixture.events)

            coroutineScope {
                (1..5).map { index ->
                    async { fixture.runInHandler { service.store(StoreRequest(entry = "entry-$index")) } }
                }.forEach { it.await() }
            }

            assertEquals((1..5).map { "entry-$it" }.toSet(), fixture.published.map { it.aggregateId }.toSet())
        }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `publishing outside a block fails`(vendor: TestVendor) = withFixture(vendor) { _, fixture ->
        assertFailsWith<IllegalStateException> {
            fixture.events.publish(TestChatEvent(origin = OWNER, message = "outside"))
        }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `clear forgets the recorded events`(vendor: TestVendor) = withFixture(vendor) { setup, fixture ->
        val service = RecordingTestService(setup.api, fixture.events)
        fixture.runInHandler { service.store(StoreRequest(entry = "cleared")) }

        fixture.clear()

        assertEquals(emptyList(), fixture.published)
    }

    private class Setup(private val database: TestDatabase, val api: DatabaseApi) {
        suspend fun entries(entry: String): Long = database.entryCount(api, entry)
    }

    private fun withFixture(vendor: TestVendor, block: suspend (Setup, TransactionalHandlerFixture) -> Unit) =
        runBlocking {
            val database = vendor.freshDatabase(migrateReliability = false)
            database.open("fixture").use { api ->
                block(Setup(database, api), TransactionalHandlerFixture.create(api, owner = OWNER))
            }
        }

    private companion object {
        const val OWNER = "fixture-test"
    }
}

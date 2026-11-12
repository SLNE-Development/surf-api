package dev.slne.surf.microservice.reliability.support

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.microservice.reliability.TransactionalEvents
import dev.slne.surf.nats.test.common.proto.v1.EchoRequest
import dev.slne.surf.nats.test.common.proto.v1.EchoResponse
import dev.slne.surf.nats.test.common.proto.v1.FailRequest
import dev.slne.surf.nats.test.common.proto.v1.FailResponse
import dev.slne.surf.nats.test.common.proto.v1.MicroserviceTestService
import dev.slne.surf.nats.test.common.proto.v1.SizeRequest
import dev.slne.surf.nats.test.common.proto.v1.SizeResponse
import dev.slne.surf.nats.test.common.proto.v1.StoreRequest
import dev.slne.surf.nats.test.common.proto.v1.StoredCountRequest
import dev.slne.surf.nats.test.common.proto.v1.StoredCountResponse
import dev.slne.surf.nats.test.common.proto.v1.StoredRequest
import dev.slne.surf.nats.test.common.proto.v1.StoredResponse
import dev.slne.surf.nats.test.common.proto.v1.TestChatEvent
import kotlinx.coroutines.flow.single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Test service whose persistent methods write to `test_entries` and count their runs.
 *
 * `store` inserts its entry and, with [events], publishes a [TestChatEvent] after the commit. `storedCount`
 * inserts [RPC_ENTRY] and answers with the number of rows, so a second run would answer differently.
 */
class RecordingTestService(
    private val databaseApi: DatabaseApi,
    private val events: TransactionalEvents? = null,
) : MicroserviceTestService {
    val storeRuns = ConcurrentHashMap<String, AtomicInteger>()
    val storedCountRuns = AtomicInteger()

    override suspend fun store(request: StoreRequest) {
        storeRuns.computeIfAbsent(request.entry) { AtomicInteger() }.incrementAndGet()
        insert(request.entry)
        events?.publish(TestChatEvent(origin = ORIGIN, message = request.entry), aggregateId = request.entry)
    }

    override suspend fun storedCount(request: StoredCountRequest): StoredCountResponse {
        storedCountRuns.incrementAndGet()
        insert(RPC_ENTRY)
        val count = suspendTransaction(db = databaseApi.database) {
            exec("SELECT COUNT(*) FROM test_entries") { row -> (row.get(0) as Number).toInt() }!!.single()!!
        }
        return StoredCountResponse(count = count)
    }

    fun storeRunsOf(entry: String): Int = storeRuns[entry]?.get() ?: 0

    private suspend fun insert(entry: String) {
        suspendTransaction(db = databaseApi.database) {
            exec("INSERT INTO test_entries (entry) VALUES ('$entry')")
        }
    }

    override suspend fun echo(request: EchoRequest): EchoResponse = unused()
    override suspend fun fail(request: FailRequest): FailResponse = unused()
    override suspend fun size(request: SizeRequest): SizeResponse = unused()
    override suspend fun stored(request: StoredRequest): StoredResponse = unused()

    private fun unused(): Nothing = throw UnsupportedOperationException("Not used by the reliability tests")

    companion object {
        const val ORIGIN = "reliability-test"
        const val RPC_ENTRY = "rpc-call"
    }
}

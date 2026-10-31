@file:OptIn(DelicateCoroutinesApi::class)

package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.CallOptions
import dev.slne.surf.nats.js.api.ConnectOptions
import dev.slne.surf.nats.js.codec.decodeToString
import dev.slne.surf.nats.js.external.nats.core.ConnectionOptions
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.MsgHdrs
import dev.slne.surf.nats.js.external.nats.core.NatsConnection
import dev.slne.surf.nats.js.external.nats.core.headers
import dev.slne.surf.nats.runtime.epochMillis
import js.buffer.ArrayBuffer
import js.numbers.JsNumbers.toJsUByte
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import node.fs.ReadFileSyncBufferOptions
import node.fs.readFileSync
import node.process.process
import web.encoding.TextEncoder
import kotlin.js.Promise
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.js.external.nats.core.connect as natsConnect

internal fun asyncTest(block: suspend CoroutineScope.() -> Unit): Promise<Unit> = GlobalScope.promise { block() }

private var uniqueCounter = 0

/**
 * Returns a kebab-case name that is unique within the test run.
 */
internal fun uniqueName(prefix: String): String {
    uniqueCounter++
    return "$prefix-${epochMillis() % 1_000_000}-$uniqueCounter"
}

internal fun callOptions(timeoutMs: Int): CallOptions = CallOptions(timeoutMs = timeoutMs)

internal fun bytes(vararg values: Int): Uint8Array<*> {
    val array = Uint8Array<ArrayBuffer>(values.size)
    for (index in values.indices) array[index] = values[index].toUByte().toJsUByte()
    return array
}

internal fun bytesOfSize(size: Int, seed: Int = 7): Uint8Array<*> {
    val array = Uint8Array<ArrayBuffer>(size)
    for (index in 0 until size) array[index] = ((index * 31 + seed) % 251).toUByte().toJsUByte()
    return array
}

internal fun Uint8Array<*>.contentEquals(other: Uint8Array<*>?): Boolean {
    if (other == null || length != other.length) return false
    for (index in 0 until length) {
        if (this[index] != other[index]) return false
    }
    return true
}

internal fun utf8Bytes(text: String): Uint8Array<*> = TextEncoder().encode(text)

private external interface FakeMsg : Msg {
    override var subject: String
    override var data: Uint8Array<*>
    override var headers: MsgHdrs?
}

internal fun fakeMessage(data: Uint8Array<*>, headers: MsgHdrs? = null, subject: String = "test.subject"): Msg {
    return unsafeJso<FakeMsg> {
        this.subject = subject
        this.data = data
        this.headers = headers
    }
}

internal fun protobufHeaders(vararg entries: Pair<String, String>): MsgHdrs {
    val result = headers()
    result.set("Content-Type", "application/protobuf")
    for ((key, value) in entries) result.set(key, value)
    return result
}

internal suspend fun eventually(timeout: Duration = 5.seconds, condition: () -> Boolean) {
    val deadline = epochMillis() + timeout.inWholeMilliseconds
    while (!condition()) {
        if (epochMillis() > deadline) throw AssertionError("Condition not met within $timeout")
        delay(20.milliseconds)
    }
}

@JsName("Object")
internal external object JsObject {
    fun keys(o: Any): Array<String>
}

/**
 * NATS server of the e2e tests, started by the `startNatsJsTestServer` Gradle task.
 *
 * Returns `null` when the e2e tests are disabled (`-Psurf.nats.js.e2e=false`), so the e2e tests skip.
 */
internal object TestNats {
    private val url: String? by lazy {
        val file = process.env["SURF_NATS_JS_TEST_URL_FILE"] ?: return@lazy null
        readFileSync(file, ReadFileSyncBufferOptions()).decodeToString().trim()
    }

    fun urlOrNull(): String? = url

    // tokenUrl, token, userUrl, user, pass of the authenticating servers
    private val auth: Map<String, String>? by lazy {
        val file = process.env["SURF_NATS_JS_TEST_AUTH_FILE"] ?: return@lazy null
        readFileSync(file, ReadFileSyncBufferOptions()).decodeToString().lines()
            .map(String::trim)
            .filter { '=' in it }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
    }

    fun auth(key: String): String = checkNotNull(auth?.get(key)) { "NATS e2e auth server setting $key is not configured" }

    suspend fun rawConnection(): NatsConnection {
        val server = checkNotNull(url) { "NATS e2e server is not configured" }
        val options = ConnectionOptions(servers = arrayOf(server), name = "raw-test")
        var last: Throwable? = null
        repeat(50) {
            try {
                return natsConnect(options).await()
            } catch (e: Throwable) {
                last = e
                delay(200.milliseconds)
            }
        }
        throw AssertionError("NATS test server $server is not reachable", last)
    }

    suspend fun client(name: String): SurfNatsClient {
        rawConnection().close().await()
        val options = ConnectOptions(name = name, servers = arrayOf(checkNotNull(url)))
        return connect(options).await()
    }
}

internal fun e2eTest(block: suspend CoroutineScope.(url: String) -> Unit): Promise<Unit> = asyncTest {
    val url = TestNats.urlOrNull()
    if (url == null) {
        console.log("Skipping NATS e2e test: SURF_NATS_JS_TEST_URL_FILE is not set")
        return@asyncTest
    }
    block(url)
}

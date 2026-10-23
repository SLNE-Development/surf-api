package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.external.protobuf.create
import dev.slne.surf.nats.js.external.protobuf.fromBinary
import dev.slne.surf.nats.js.external.protobuf.toBinary
import dev.slne.surf.nats.js.fixtures.UuidSchema
import dev.slne.surf.nats.js.fixtures.facade.FacadeConnectOptions
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import js.typedarrays.toUByteArray
import kotlinx.coroutines.await
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import dev.slne.surf.nats.js.fixtures.facade.uuidFromString as facadeUuidFromString
import dev.slne.surf.nats.js.fixtures.facade.uuidToString as facadeUuidToString
import dev.slne.surf.nats.js.fixtures.facade.NatsCallTimeoutError as FacadeCallTimeoutError
import dev.slne.surf.nats.js.fixtures.facade.NatsNoRespondersError as FacadeNoRespondersError
import dev.slne.surf.nats.js.fixtures.facade.NatsRemoteError as FacadeRemoteError
import dev.slne.surf.nats.js.fixtures.facade.NatsRpcError as FacadeRpcError
import dev.slne.surf.nats.js.fixtures.facade.connect as facadeConnect
import dev.slne.surf.nats.js.fixtures.kotlin.NatsCallTimeoutError as KotlinCallTimeoutError
import dev.slne.surf.nats.js.fixtures.kotlin.NatsNoRespondersError as KotlinNoRespondersError
import dev.slne.surf.nats.js.fixtures.kotlin.NatsRemoteError as KotlinRemoteError
import dev.slne.surf.nats.js.fixtures.kotlin.NatsRpcError as KotlinRpcError

class FacadeTest {

    private fun facadeOptions(name: String, servers: String): FacadeConnectOptions = unsafeJso {
        this.name = name
        this.servers = servers
    }

    @Test
    fun facadeReExportsTheKotlinErrors() {
        assertTrue(FacadeRpcError === KotlinRpcError)
        assertTrue(FacadeRemoteError === KotlinRemoteError)
        assertTrue(FacadeNoRespondersError === KotlinNoRespondersError)
        assertTrue(FacadeCallTimeoutError === KotlinCallTimeoutError)
    }

    @Test
    fun errorsAreErrorsAndKeepTheirHierarchy() {
        val error: Throwable = NatsRemoteError("java.lang.IllegalStateException", "boom")
        assertTrue(isInstance(error, ErrorConstructor))
        assertTrue(isInstance(error, FacadeRpcError))
        assertTrue(isInstance(error, FacadeRemoteError))
        assertEquals("java.lang.IllegalStateException: boom", error.message)
    }

    @Test
    fun connectSplitsAServerStringBeforeKotlinValidatesIt() = asyncTest {
        // Only blank entries remain after the split, so Kotlin rejects the list before connecting.
        val error = assertFailsWith<IllegalArgumentException> { facadeConnect(facadeOptions("surf-panel", " , ")).await() }
        assertTrue(error.message!!.contains("must not be empty"), error.message)
    }

    @Test
    fun connectRejectsMissingOptions() = asyncTest {
        val error = assertFailsWith<IllegalArgumentException> { facadeConnect(null).await() }
        assertEquals("options is required", error.message)
    }

    @Test
    fun connectAcceptsACommaSeparatedServerString() = e2eTest { url ->
        TestNats.rawConnection().close().await()
        val client = facadeConnect(facadeOptions(uniqueName("facade"), "$url, $url")).await()
        try {
            assertFalse(client.isClosed)
        } finally {
            client.close().await()
        }
    }

    @Test
    fun uuidHelpersUseTheJvmByteOrder() {
        // The vector of the JVM UuidSerializerTest: msb then lsb, big-endian
        val uuid = facadeUuidFromString("00112233-4455-6677-8899-AABBCCDDEEFF")

        assertEquals("0a1000112233445566778899aabbccddeeff", hex(toBinary(UuidSchema, create(UuidSchema, uuid))))
        assertEquals("00112233445566778899aabbccddeeff", hex(assertNotNull(uuid.value)))
        assertEquals("surf.types.v1.Uuid", uuid.`$typeName`)
        assertEquals("00112233-4455-6677-8899-aabbccddeeff", facadeUuidToString(uuid))
        assertEquals("00112233-4455-6677-8899-aabbccddeeff", facadeUuidToString(uuid.value))
        val decoded = fromBinary(UuidSchema, bytes(0x0a, 0x10, *IntArray(16) { 0xff - it }))
        assertEquals("fffefdfc-fbfa-f9f8-f7f6-f5f4f3f2f1f0", facadeUuidToString(decoded))
    }

    @Test
    fun uuidHelpersRejectMalformedInput() {
        for (value in listOf("00112233445566778899aabbccddeeff", "00112233-4455-6677-8899-aabbccddeef", "g0112233-4455-6677-8899-aabbccddeeff", null)) {
            val error = assertFails { facadeUuidFromString(value) }
            assertTrue(error.message!!.startsWith("Invalid UUID"), error.message)
        }
        val tooShort = assertFails { facadeUuidToString(bytesOfSize(15)) }
        assertEquals("surf.types.v1.Uuid value must be exactly 16 bytes, was 15", tooShort.message)
        assertFails { facadeUuidToString(null) }
    }

    private fun hex(bytes: Uint8Array<*>): String = bytes.toUByteArray().joinToString("") { it.toString(16).padStart(2, '0') }

    private val isInstance: (Any?, Any) -> Boolean = js("(function (value, type) { return value instanceof type; })")
}

@JsName("Error")
private external val ErrorConstructor: Any

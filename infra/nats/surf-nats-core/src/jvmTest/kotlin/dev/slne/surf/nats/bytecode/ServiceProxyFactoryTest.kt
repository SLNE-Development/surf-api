package dev.slne.surf.nats.bytecode

import dev.slne.surf.nats.rpc.RpcCaller
import dev.slne.surf.nats.rpc.ServiceDescriptor
import dev.slne.surf.nats.test.proto.v1.CalcService
import dev.slne.surf.nats.test.proto.v1.FireRequest
import dev.slne.surf.nats.test.proto.v1.HealthReply
import dev.slne.surf.nats.test.proto.v1.MixedRequest
import dev.slne.surf.nats.test.proto.v1.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds

class ServiceProxyFactoryTest {

    private class RecordingCaller(private val result: Any?) : RpcCaller {
        val calls = mutableListOf<Pair<Int, Any>>()

        override suspend fun call(index: Int, request: Any): Any? {
            delay(1.milliseconds)
            calls += index to request
            return result
        }
    }

    private val descriptor = ServiceDescriptor.of(CalcService::class.java)

    private fun index(name: String) = descriptor.methods.single { it.kotlinName == name }.index

    @Test
    fun `proxy passes the request message with the method index`() = runTest {
        val caller = RecordingCaller(Text("ok"))
        val request = MixedRequest(1, 2L, 3.5, true, "label")

        assertEquals(Text("ok"), ServiceProxyFactory.create<CalcService>(descriptor, caller).mixed(request))

        val (index, passed) = caller.calls.single()
        assertEquals(index("mixed"), index)
        assertSame(request, passed)
    }

    @Test
    fun `proxy passes Unit for an Empty request`() = runTest {
        val caller = RecordingCaller(HealthReply())

        assertEquals(HealthReply(), ServiceProxyFactory.create<CalcService>(descriptor, caller).health())

        assertEquals(listOf<Pair<Int, Any>>(index("health") to Unit), caller.calls)
    }

    @Test
    fun `unit method returns after the call`() = runTest {
        val caller = RecordingCaller(Unit)

        ServiceProxyFactory.create<CalcService>(descriptor, caller).fire(FireRequest())

        assertEquals(listOf<Pair<Int, Any>>(index("fire") to FireRequest()), caller.calls)
    }
}

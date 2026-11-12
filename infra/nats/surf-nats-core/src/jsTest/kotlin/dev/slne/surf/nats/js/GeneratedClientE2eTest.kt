package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.codec.NatsHeaders
import dev.slne.surf.nats.js.fixtures.FindPlayerRequest
import dev.slne.surf.nats.js.fixtures.FindPlayerRequestSchema
import dev.slne.surf.nats.js.fixtures.FindPlayerResponse
import dev.slne.surf.nats.js.fixtures.FindPlayerResponseSchema
import dev.slne.surf.nats.js.fixtures.LocateResponse
import dev.slne.surf.nats.js.fixtures.LocateResponseSchema
import dev.slne.surf.nats.js.fixtures.createPlayerServiceClient
import dev.slne.surf.nats.js.fixtures.fields
import js.objects.unsafeJso
import kotlinx.coroutines.await
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Calls through the client protoc-gen-surf-nats generated for `surf.natsjs.test.v1.PlayerService` (services_nats.js).
 */
class GeneratedClientE2eTest {

    private val service = "surf.natsjs.test.v1.PlayerService"

    private fun request(name: String): FindPlayerRequest = unsafeJso { this.name = name }

    @Test
    fun generatedClientCallsValueUnitAndAnswerMethods() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("generated-caller"))
        try {
            val kicked = mutableListOf<String?>()
            raw.subscribe("surf.rpc.$service.FindPlayer", queue = service) { message ->
                val name = raw.decode(FindPlayerRequestSchema, message.data).fields<FindPlayerRequest>().name
                raw.respond(message, raw.encode(FindPlayerResponseSchema, unsafeJso<FindPlayerResponse> {
                    this.name = name
                    server = "lobby"
                }))
            }
            raw.subscribe("surf.rpc.$service.Kick", queue = service) { message ->
                kicked += raw.decode(FindPlayerRequestSchema, message.data).fields<FindPlayerRequest>().name
                raw.respond(message, NatsHeaders.EMPTY_BODY)
            }
            raw.subscribe("surf.rpc.$service.Locate") { message ->
                raw.respond(message, raw.encode(LocateResponseSchema, unsafeJso<LocateResponse> { server = "lobby-3" }))
            }
            raw.flush()

            val players = createPlayerServiceClient(client)

            val found = players.findPlayer(request("Steve")).await().fields<FindPlayerResponse>()
            assertEquals("Steve", found.name)
            assertEquals("lobby", found.server)

            assertEquals("undefined", jsTypeOf(players.kick(request("Alex"), callOptions(2_000)).await()))
            assertEquals(listOf<String?>("Alex"), kicked)

            val located = players.locate(request("Steve"), callOptions(2_000)).await()
            assertEquals(true, located.known)
            assertEquals("lobby-3", located.value.fields<LocateResponse>().server)
        } finally {
            client.close().await()
            raw.close()
        }
    }
}

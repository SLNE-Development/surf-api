package dev.slne.surf.nats.js.fixtures

import dev.slne.surf.nats.js.api.Answer
import dev.slne.surf.nats.js.api.CallOptions
import dev.slne.surf.nats.js.api.ProtoMessage
import kotlin.js.Promise

// The generated PlayerServiceClient of services_nats.d.ts; LocateRequest has the shape of FindPlayerRequest
internal external interface PlayerServiceClient {
    fun findPlayer(request: FindPlayerRequest, options: CallOptions = definedExternally): Promise<ProtoMessage>
    fun kick(request: FindPlayerRequest, options: CallOptions = definedExternally): Promise<Unit?>
    fun locate(request: FindPlayerRequest, options: CallOptions = definedExternally): Promise<Answer>
}

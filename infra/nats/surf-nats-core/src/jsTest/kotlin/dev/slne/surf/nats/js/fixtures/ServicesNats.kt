@file:JsModule("../gen/surf/natsjs/test/v1/services_nats.js")

package dev.slne.surf.nats.js.fixtures

import dev.slne.surf.nats.js.SurfNatsClient

internal external fun createPlayerServiceClient(nats: SurfNatsClient): PlayerServiceClient

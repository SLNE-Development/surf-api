@file:JsModule("../gen/surf/natsjs/test/v1/services_pb.js")

package dev.slne.surf.nats.js.fixtures

import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescService

internal external val FindPlayerRequestSchema: DescMessage
internal external val FindPlayerResponseSchema: DescMessage
internal external val LocateRequestSchema: DescMessage
internal external val LocateResponseSchema: DescMessage
internal external val BlobRequestSchema: DescMessage
internal external val BlobResponseSchema: DescMessage
internal external val AuditEntrySchema: DescMessage
internal external val AuditQuerySchema: DescMessage
internal external val AuditResultSchema: DescMessage

internal external val PlayerService: DescService
internal external val AuditService: DescService
internal external val RegistryService: DescService
internal external val StreamingService: DescService

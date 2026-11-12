@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.fixtures

import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.external.protobuf.DescMessage

// Parses the proto3 JSON form of a message.
internal external fun fromJsonString(schema: DescMessage, json: String): ProtoMessage

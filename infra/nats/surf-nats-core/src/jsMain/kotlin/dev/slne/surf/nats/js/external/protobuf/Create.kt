@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit

internal external fun create(schema: DescMessage, init: ProtoMessageInit = definedExternally): ProtoMessage

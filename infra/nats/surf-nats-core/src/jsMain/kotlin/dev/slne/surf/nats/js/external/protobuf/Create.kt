@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.ProtoMessage
import dev.slne.surf.nats.js.ProtoMessageInit

internal external fun create(schema: DescMessage, init: ProtoMessageInit = definedExternally): ProtoMessage

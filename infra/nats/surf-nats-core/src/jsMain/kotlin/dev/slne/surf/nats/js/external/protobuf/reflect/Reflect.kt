@file:JsModule("@bufbuild/protobuf/reflect")

package dev.slne.surf.nats.js.external.protobuf.reflect

import dev.slne.surf.nats.js.ProtoMessage
import dev.slne.surf.nats.js.external.protobuf.DescMessage

internal external fun reflect(
    messageDesc: DescMessage,
    message: ProtoMessage = definedExternally,
    check: Boolean = definedExternally,
): ReflectMessage

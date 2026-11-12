@file:JsModule("@bufbuild/protobuf/reflect")

package dev.slne.surf.nats.js.external.protobuf.reflect

import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.external.protobuf.DescMessage

/**
 * Binds `reflect` of `@bufbuild/protobuf/reflect`, which creates a [ReflectMessage] for [messageDesc].
 *
 * Without [message] it creates a new message. By default, `set` validates field values, `check = false` disables that.
 */
internal external fun reflect(
    messageDesc: DescMessage,
    message: ProtoMessage = definedExternally,
    check: Boolean = definedExternally,
): ReflectMessage

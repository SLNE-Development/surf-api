@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.api.ProtoMessageInit

/**
 * Binds `create`, which creates a message of [schema] from an optional init object.
 */
internal external fun create(schema: DescMessage, init: ProtoMessageInit = definedExternally): ProtoMessage

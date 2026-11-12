@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

import dev.slne.surf.nats.js.external.protobuf.wkt.FileDescriptorSet
import js.typedarrays.Uint8Array

// fromBinary() of @bufbuild/protobuf, typed for FileDescriptorSetSchema
@JsName("fromBinary")
internal external fun decodeFileDescriptorSet(schema: DescMessage, bytes: Uint8Array<*>): FileDescriptorSet

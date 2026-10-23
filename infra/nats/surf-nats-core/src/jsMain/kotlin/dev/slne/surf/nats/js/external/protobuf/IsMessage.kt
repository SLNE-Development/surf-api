@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

// A type guard in TypeScript (`arg is MessageShape<Desc>`); see messageOf.
internal external fun isMessage(arg: Any?, schema: DescMessage = definedExternally): Boolean

@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

/**
 * Binds `AnyDesc`, the union of the descriptor types of `@bufbuild/protobuf`.
 *
 * The `Desc*` interfaces of this package model the types of `descriptors.d.ts` and declare only the members that
 * surf-nats reads. Where TypeScript uses a union discriminated by `kind` or `fieldKind`, the Kotlin side declares one
 * interface per variant, for example [DescFieldScalar].
 */
internal external interface AnyDesc {
    /**
     * One of `file`, `enum`, `enum_value`, `message`, `field`, `extension`, `oneof`, `service` and `rpc`.
     */
    val kind: String
}

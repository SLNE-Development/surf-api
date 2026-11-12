@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

/**
 * A field with `fieldKind` `list`.
 */
internal external interface DescFieldList : DescField {
    /**
     * Kind of the elements: `scalar`, `enum` or `message`.
     */
    val listKind: String
}

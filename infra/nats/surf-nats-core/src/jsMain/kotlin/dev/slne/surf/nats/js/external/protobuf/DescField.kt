@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

/**
 * Binds `DescField`, a field of a message.
 *
 * protobuf-es discriminates the variants by `fieldKind`. The Kotlin side declares them as [DescFieldSingular],
 * [DescFieldList], [DescFieldMap] and their subinterfaces. `FieldShape` maps a field to its variant.
 */
internal external interface DescField : AnyDesc {
    val name: String
    val localName: String
    val number: Int

    /**
     * One of `scalar`, `enum`, `message`, `list` and `map`.
     */
    val fieldKind: String

    /**
     * `FeatureSet.FieldPresence`: 1 explicit, 2 implicit, 3 legacy required.
     */
    val presence: Int
    val parent: DescMessage

    /**
     * Always `undefined` for list and map fields.
     */
    val oneof: DescOneof?

    /**
     * Set for message fields and for message lists and maps, `undefined` otherwise.
     */
    val message: DescMessage?

    /**
     * Set for enum fields and for enum lists and maps, `undefined` otherwise.
     */
    val enum: DescEnum?
}

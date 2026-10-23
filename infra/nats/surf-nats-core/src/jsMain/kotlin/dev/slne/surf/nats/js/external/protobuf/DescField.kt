@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

// A field of a message; protobuf-es discriminates the variants by fieldKind (see FieldShape).
internal external interface DescField : AnyDesc {
    val name: String
    val localName: String
    val number: Int

    // scalar | enum | message | list | map
    val fieldKind: String

    // FeatureSet.FieldPresence: 1 explicit, 2 implicit, 3 legacy required
    val presence: Int
    val parent: DescMessage

    // Always undefined for list and map fields
    val oneof: DescOneof?

    // Set for message fields and message lists and maps, undefined otherwise
    val message: DescMessage?

    // Set for enum fields and enum lists and maps, undefined otherwise
    val enum: DescEnum?
}

@file:JsModule("@bufbuild/protobuf")

package dev.slne.surf.nats.js.external.protobuf

// Comments of an element in its .proto source; sourcePath is the SourceCodeInfo.Location.path of the element
internal external interface DescComments {
    val leading: String?
    val trailing: String?
    val sourcePath: Array<Int>
}

@file:JsModule("@bufbuild/protobuf/wkt")

package dev.slne.surf.nats.js.external.protobuf

// SourceCodeInfo.Location: span is [startLine, startColumn, endLine?, endColumn], zero-based.
internal external interface SourceCodeInfoLocation {
    val path: Array<Int>
    val span: Array<Int>
}

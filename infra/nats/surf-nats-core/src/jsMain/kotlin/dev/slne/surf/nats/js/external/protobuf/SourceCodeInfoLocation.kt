@file:JsModule("@bufbuild/protobuf/wkt")

package dev.slne.surf.nats.js.external.protobuf

/**
 * Binds `SourceCodeInfo.Location`.
 */
internal external interface SourceCodeInfoLocation {
    val path: Array<Int>

    /**
     * Zero-based `[startLine, startColumn, endLine, endColumn]`. `endLine` is omitted when it equals `startLine`.
     */
    val span: Array<Int>
}

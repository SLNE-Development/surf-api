package dev.slne.surf.nats.js.external.protobuf.protoplugin

import dev.slne.surf.nats.js.external.protobuf.DescFile
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescService

internal external interface GeneratedFile {
    fun preamble(file: DescFile)

    // Prints one line; imports and local names of the symbols are resolved when the file is rendered
    fun print(vararg printables: Printable)
    fun jsDoc(text: String, indentation: String = definedExternally): Printable
    fun export(declaration: String, name: String): Printable

    // The descriptor protoc-gen-es generates, such as PlayerService or FindPlayerRequestSchema
    fun importSchema(schema: DescService, typeOnly: Boolean = definedExternally): ImportSymbol
    fun importSchema(schema: DescMessage, typeOnly: Boolean = definedExternally): ImportSymbol

    // The message type protoc-gen-es generates, always type-only
    fun importShape(schema: DescMessage): ImportSymbol

    // from is a package or a path relative to the output directory, such as ./surf/a/v1/x_pb.js
    fun import(name: String, from: String, typeOnly: Boolean = definedExternally): ImportSymbol
}

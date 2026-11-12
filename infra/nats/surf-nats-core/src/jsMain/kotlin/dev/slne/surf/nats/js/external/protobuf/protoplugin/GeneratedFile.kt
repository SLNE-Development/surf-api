package dev.slne.surf.nats.js.external.protobuf.protoplugin

import dev.slne.surf.nats.js.external.protobuf.DescFile
import dev.slne.surf.nats.js.external.protobuf.DescMessage
import dev.slne.surf.nats.js.external.protobuf.DescService

/**
 * Binds `GeneratedFile` of `@bufbuild/protoplugin`, a file of the plugin output. [Schema.generateFile] creates it.
 */
internal external interface GeneratedFile {
    /**
     * Writes the standard header of generated files: the `@generated` comments for [file] and the comments of its
     * `syntax` statement.
     */
    fun preamble(file: DescFile)

    /**
     * Prints one line. Imports and local names of the symbols are resolved when the file is rendered.
     */
    fun print(vararg printables: Printable)
    fun jsDoc(text: String, indentation: String = definedExternally): Printable
    fun export(declaration: String, name: String): Printable

    /**
     * Imports the descriptor that protoc-gen-es generates, such as `PlayerService` or `FindPlayerRequestSchema`.
     */
    fun importSchema(schema: DescService, typeOnly: Boolean = definedExternally): ImportSymbol
    fun importSchema(schema: DescMessage, typeOnly: Boolean = definedExternally): ImportSymbol

    /**
     * Imports the message type that protoc-gen-es generates. The import is always type-only.
     */
    fun importShape(schema: DescMessage): ImportSymbol

    /**
     * Imports [name] from [from], a package or a path relative to the output directory such as `./surf/a/v1/x_pb.js`.
     */
    fun import(name: String, from: String, typeOnly: Boolean = definedExternally): ImportSymbol
}

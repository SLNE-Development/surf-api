package dev.slne.surf.nats.js.external.protobuf.protoplugin

import dev.slne.surf.nats.js.external.protobuf.DescFile

/**
 * Binds `Schema` of `@bufbuild/protoplugin`, the input of `generateTs`.
 */
internal external interface Schema {
    /**
     * The files to generate.
     */
    val files: Array<DescFile>

    /**
     * Every file of the request, dependencies included.
     */
    val allFiles: Array<DescFile>

    /**
     * Creates an output file. [name] is relative to the output directory and ends with `.ts`.
     */
    fun generateFile(name: String): GeneratedFile
}

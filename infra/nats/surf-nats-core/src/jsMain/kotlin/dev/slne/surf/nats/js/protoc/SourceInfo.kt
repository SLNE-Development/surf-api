package dev.slne.surf.nats.js.protoc

import dev.slne.surf.nats.js.external.protobuf.DescComments
import dev.slne.surf.nats.js.external.protobuf.DescFile

/**
 * Source positions of the declarations in [file], read from its `source_code_info`, and the JSDoc text of their
 * comments.
 *
 * Without source info, positions name the file only.
 */
internal class SourceInfo(private val file: DescFile) {
    private val spans = HashMap<String, Array<Int>>().apply {
        for (location in file.proto.sourceCodeInfo?.location.orEmpty()) {
            val path = location.path.joinToString(",")
            if (path !in this) this[path] = location.span
        }
    }

    /**
     * Returns `<file>:<line>:<column>` (one-based) of the declaration [comments] belong to, or the file name when it
     * is unknown.
     */
    fun position(comments: DescComments): String {
        val span = spans[comments.sourcePath.joinToString(",")] ?: return file.proto.name
        return "${file.proto.name}:${span[0] + 1}:${span[1] + 1}"
    }

    companion object {
        /**
         * Returns the leading and trailing comment as JSDoc text, or `null` without any.
         *
         * Formatted like protoc-gen-es: the leading comment, a blank line, the trailing comment, and one leading space
         * removed from every line. Line endings are normalized to `\n`, so the JSDoc does not depend on those of the
         * `.proto` file; the preamble of protoplugin copies the `syntax` and `package` comments unchanged.
         */
        fun comment(comments: DescComments): String? {
            val text = listOfNotNull(comments.leading, comments.trailing)
                .map { it.replace("\r\n", "\n").replace('\r', '\n').removeSuffix("\n") }
                .filter(String::isNotEmpty)
                .joinToString("\n\n")
            if (text.isEmpty()) return null
            return text.lines().joinToString("\n") { it.removePrefix(" ") }
        }
    }
}

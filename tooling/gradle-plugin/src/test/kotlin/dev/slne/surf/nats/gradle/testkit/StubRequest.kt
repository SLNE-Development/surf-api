package dev.slne.surf.nats.gradle.testkit

/**
 * One request received by [WeaverStub].
 */
internal class StubRequest(
    val method: String,
    val path: String,
    headers: Map<String, String>,
    val body: ByteArray,
) {
    private val headers = headers.mapKeys { it.key.lowercase() }

    fun header(name: String): String? = headers[name.lowercase()]

    /**
     * Returns the payload of the named multipart part of a publish request.
     */
    fun part(name: String): ByteArray? {
        val boundary = Regex("boundary=(\\S+)").find(header("content-type").orEmpty())?.groupValues?.get(1)
            ?: return null
        val text = String(body, Charsets.ISO_8859_1)
        val start = text.indexOf("Content-Disposition: form-data; name=\"$name\"").takeIf { it >= 0 } ?: return null
        val contentStart = text.indexOf("\r\n\r\n", start) + 4
        val contentEnd = text.indexOf("\r\n--$boundary", contentStart)
        return text.substring(contentStart, contentEnd).toByteArray(Charsets.ISO_8859_1)
    }

    override fun toString(): String = "$method $path"
}

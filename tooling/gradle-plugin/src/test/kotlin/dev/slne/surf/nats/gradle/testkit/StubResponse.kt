package dev.slne.surf.nats.gradle.testkit

/**
 * Canned answer of [WeaverStub].
 */
internal class StubResponse(
    val status: Int = 200,
    val body: ByteArray = ByteArray(0),
    val headers: Map<String, String> = emptyMap(),
) {
    constructor(status: Int, body: String) : this(status, body.toByteArray(Charsets.UTF_8))
}

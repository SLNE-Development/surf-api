package dev.slne.surf.nats.gradle.testkit

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-process stand-in for the weaver registry REST API on a random local port.
 *
 * Unknown routes answer 404. Every request is recorded in [requests].
 */
internal class WeaverStub : AutoCloseable {

    private val routes = ConcurrentHashMap<String, (StubRequest) -> StubResponse>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

    val requests = CopyOnWriteArrayList<StubRequest>()
    val url: String get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/") { exchange -> handle(exchange) }
        server.start()
    }

    fun route(method: String, path: String, response: (StubRequest) -> StubResponse) {
        routes["$method $path"] = response
    }

    /**
     * Serves the version list of `group:name`; each entry is a version and whether it is a snapshot.
     */
    fun versions(group: String, name: String, vararg versions: Pair<String, Boolean>, repository: String = REPOSITORY) {
        val json = versions.joinToString(",", "{\"versions\":[", "]}") { (version, snapshot) ->
            "{\"version\":\"$version\",\"isSnapshot\":$snapshot,\"buildNumber\":1}"
        }
        route("GET", artifactPath(repository, group, name)) { StubResponse(200, json) }
    }

    /**
     * Serves the archive of `group:name:version`, with its `x-weaver-sha256` header unless [sha256] is null.
     */
    fun archive(
        group: String,
        name: String,
        version: String,
        bytes: ByteArray,
        sha256: String? = sha256(bytes),
        repository: String = REPOSITORY,
    ) {
        val headers = if (sha256 == null) emptyMap() else mapOf("x-weaver-sha256" to sha256)
        route("GET", "${artifactPath(repository, group, name)}/$version/archive") { StubResponse(200, bytes, headers) }
    }

    /**
     * Accepts publishes of `group:name:version` with a success response.
     */
    fun acceptPublish(group: String, name: String, version: String, repository: String = REPOSITORY) {
        val body = "{\"coordinate\":\"$group:$name:$version\",\"buildNumber\":1,\"sha256\":\"${"0".repeat(64)}\"," +
                "\"redeployed\":false}"
        route("PUT", "${artifactPath(repository, group, name)}/$version") { StubResponse(200, body) }
    }

    /**
     * Answers `/resolve` with an empty resolution.
     */
    fun acceptResolve() {
        route("POST", "/api/v1/resolve") { StubResponse(200, "{\"artifacts\":[],\"conflicts\":[]}") }
    }

    fun requests(method: String): List<StubRequest> = requests.filter { it.method == method }

    private fun handle(exchange: HttpExchange) {
        exchange.use {
            val request = StubRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.rawPath,
                headers = exchange.requestHeaders.entries.associate { it.key to it.value.first() },
                body = exchange.requestBody.readBytes(),
            )
            requests += request

            val response = routes["${request.method} ${request.path}"]?.invoke(request)
                ?: StubResponse(404, "{\"error\":{\"code\":\"not_found\",\"message\":\"${request.path} is unknown\"}}")
            response.headers.forEach { (name, value) -> exchange.responseHeaders.add(name, value) }
            exchange.sendResponseHeaders(response.status, if (response.body.isEmpty()) -1 else response.body.size.toLong())
            if (response.body.isNotEmpty()) exchange.responseBody.use { it.write(response.body) }
        }
    }

    override fun close() {
        server.stop(0)
    }

    companion object {
        const val REPOSITORY = "proto-releases-public"

        fun artifactPath(repository: String, group: String, name: String): String =
            "/api/v1/repos/$repository/artifacts/$group/$name"

        fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}

package dev.slne.surf.nats.gradle.weaver

import kotlinx.serialization.json.*
import org.gradle.api.GradleException
import org.jetbrains.annotations.Blocking
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Reads the versions and archives of one weaver artifact for the compatibility baseline.
 *
 * Uses HTTP/1.1 like weaver's own client, because the registry closes plain-http connections that ask for an
 * h2c upgrade. Every non-2xx response except a 404 on the version list becomes a [GradleException].
 */
internal class WeaverBaselineClient(baseUrl: String, private val token: String?) {

    private val baseUrl = baseUrl.trimEnd('/')
    private val http = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * Returns the release versions of `group:name` in [repository], or `null` when the artifact does not exist
     * there or the token cannot read it.
     */
    @Blocking
    fun releases(repository: String, group: String, name: String): List<String>? {
        val request = request(artifactUri(repository, group, name)).GET().build()
        val response = send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == NOT_FOUND) return null
        if (response.statusCode() !in SUCCESS) throw failure(
            response.statusCode(),
            response.body(),
            request.uri()
        )

        return try {
            Json.parseToJsonElement(response.body())
                .jsonObject.getValue("versions").jsonArray
                .map { it.jsonObject }
                .filterNot { it.getValue("isSnapshot").jsonPrimitive.boolean }
                .map { it.getValue("version").jsonPrimitive.content }
        } catch (e: RuntimeException) {
            throw GradleException("Unexpected response from ${request.uri()}: ${e.message}", e)
        }
    }

    /**
     * Downloads the archive of `group:name:version` from [repository] to [target].
     *
     * Verifies the `x-weaver-sha256` header when the server sends one.
     */
    @Blocking
    fun download(repository: String, group: String, name: String, version: String, target: File) {
        target.parentFile.mkdirs()
        val uri = URI.create("${artifactUri(repository, group, name)}/${encode(version)}/archive")
        val request = request(uri).GET().build()
        val handler = HttpResponse.BodyHandlers.ofFile(
            target.toPath(),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )

        val response = try {
            send(request, handler)
        } catch (e: GradleException) {
            target.delete()
            throw e
        }

        if (response.statusCode() !in SUCCESS) {
            val body = target.takeIf(File::isFile)?.readText().orEmpty()
            target.delete()
            throw failure(response.statusCode(), body, uri)
        }

        val expected = response.headers().firstValue(SHA256_HEADER).orElse(null) ?: return
        val actual = sha256(target)
        if (!actual.equals(expected, ignoreCase = true)) {
            target.delete()
            throw GradleException("Checksum mismatch for $group:$name:$version: expected $expected, got $actual")
        }
    }

    private fun artifactUri(repository: String, group: String, name: String): URI {
        val path =
            listOf(repository, "artifacts", group, name).joinToString("/", transform = ::encode)
        return URI.create("$baseUrl/api/v1/repos/$path")
    }

    private fun request(uri: URI): HttpRequest.Builder {
        return HttpRequest.newBuilder(uri)
            .timeout(TIMEOUT)
            .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
    }

    /**
     * Sends [request] and waits at most [TIMEOUT] for the complete response, body included.
     *
     * The request timeout alone ends once the response headers arrive, so a stalled body would block forever.
     */
    @Blocking
    private fun <T> send(
        request: HttpRequest,
        handler: HttpResponse.BodyHandler<T>
    ): HttpResponse<T> {
        val future = http.sendAsync(request, handler)
        return try {
            future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw GradleException(
                "surf-nats request to ${request.uri()} did not complete within $TIMEOUT",
                e
            )
        } catch (e: ExecutionException) {
            val cause = e.cause ?: e
            if (cause is IOException) {
                throw GradleException(
                    "surf-nats could not reach ${request.uri()}: ${cause.message}",
                    cause
                )
            }
            throw GradleException(
                "surf-nats request to ${request.uri()} failed: ${cause.message}",
                cause
            )
        } catch (e: InterruptedException) {
            future.cancel(true)
            Thread.currentThread().interrupt()
            throw GradleException("surf-nats request to ${request.uri()} was interrupted", e)
        }
    }

    private fun failure(status: Int, body: String, uri: URI): GradleException =
        GradleException("Weaver request $uri failed with HTTP $status: ${body.take(MAX_BODY)}")

    private companion object {
        const val NOT_FOUND = 404
        const val MAX_BODY = 500
        const val SHA256_HEADER = "x-weaver-sha256"
        val TIMEOUT: Duration = Duration.ofMinutes(5)
        val SUCCESS = 200..299

        fun encode(segment: String): String = URLEncoder.encode(segment, StandardCharsets.UTF_8)

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

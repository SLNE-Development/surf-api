package dev.slne.surf.api.core.server.impl.lookup

import kotlinx.coroutines.future.await
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Sends GET requests to JSON APIs with the JDK's [HttpClient].
 *
 * Requests suspend instead of blocking a thread; cancelling the calling coroutine cancels the
 * request. [close] shuts the client down; requests afterwards fail.
 */
@Component
internal class JsonHttp : AutoCloseable {

    private val client = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * The status of a response and its body, or `null` as body if it is empty or does not parse.
     */
    class Response<T>(val status: Int, val body: T?) {
        val isSuccess: Boolean get() = status in 200..299
    }

    final suspend inline fun <reified T> get(url: String): Response<T> {
        return get(url, Json.serializersModule.serializer())
    }

    /**
     * Fetches [url] and parses the body with [deserializer].
     *
     * @throws java.io.IOException if the request fails before a response arrives
     */
    suspend fun <T> get(url: String, deserializer: DeserializationStrategy<T>): Response<T> {
        val response = getString(url)
        val body = response.body?.let {
            runCatching { json.decodeFromString(deserializer, it) }.getOrNull()
        }

        return Response(response.status, body)
    }

    private suspend fun getString(url: String): Response<String> {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(TIMEOUT)
            .header("Accept", "application/json")
            .GET()
            .build()
        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()

        return Response(response.statusCode(), response.body().takeIf(String::isNotEmpty))
    }

    override fun close(): Unit = client.close()

    companion object {
        private val TIMEOUT = 10.seconds.toJavaDuration()

        private val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
        }

        /**
         * Encodes [value] for use as one URL path segment.
         */
        fun pathSegment(value: String): String {
            return URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")
        }
    }
}
